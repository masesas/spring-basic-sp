#!/usr/bin/env python3
"""Menjalankan berkas role-*.http tanpa IDE.

    python3 http/runner.py

Keluar dengan kode 1 bila ada assertion yang gagal, sehingga bisa dipakai di CI.
Aplikasi harus sudah berjalan, dan http-client.private.env.json sudah diisi.

Ini penafsir minimal, bukan pengganti JetBrains HTTP Client. Yang didukung hanya
subset yang dipakai berkas RBAC di direktori ini:

  * satu request per blok ###, dengan header dan body opsional
  * substitusi {{variabel}} dari environment dan dari client.global.set
  * client.assert atas response.status dan response.body
  * client.global.set("nama", response.body.field)

Tidak ada mesin JavaScript di sini: ekspresi assertion diterjemahkan ke Python.
Satu-satunya nilai yang dihitung khusus adalah periode payroll, yang di berkas
.http dihitung lewat new Date() — di sini diisi tanggal 1 bulan berjalan, sama
seperti yang dilakukan script login di tiap berkas.
"""
import json
import re
import sys
import urllib.error
import urllib.request
from datetime import date
from pathlib import Path

DIR = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).parent

env_publik = DIR / "http-client.env.json"
env_privat = DIR / "http-client.private.env.json"

if not env_privat.exists():
    sys.exit(f"{env_privat} belum ada. Salin dari {env_privat.name}.example "
             f"lalu isi demoPassword sesuai DEMO_PASSWORD di .env")

env = json.loads(env_publik.read_text())["dev"]
env.update(json.loads(env_privat.read_text())["dev"])

globals_ = {}
awal_bulan = date.today().replace(day=1).isoformat()


def subst(teks):
    def ganti(m):
        nama = m.group(1)
        if nama in globals_:
            return str(globals_[nama])
        if nama in env:
            return str(env[nama])
        raise KeyError(nama)
    return re.sub(r"\{\{(\w+)\}\}", ganti, teks)


def parse(path):
    """Memecah berkas .http menjadi daftar blok request."""
    blok = []
    saat_ini = None
    dalam_handler = False
    for baris in path.read_text().split("\n"):
        if baris.startswith("###"):
            if saat_ini:
                blok.append(saat_ini)
            saat_ini = {"judul": baris.lstrip("# ").strip(), "baris": [], "handler": []}
            dalam_handler = False
            continue
        if saat_ini is None:
            continue
        if baris.startswith("> {%"):
            sisa = baris[4:]
            if sisa.rstrip().endswith("%}"):
                saat_ini["handler"].append(sisa.rstrip()[:-2])
            else:
                saat_ini["handler"].append(sisa)
                dalam_handler = True
            continue
        if dalam_handler:
            if baris.strip() == "%}":
                dalam_handler = False
            else:
                saat_ini["handler"].append(baris)
            continue
        saat_ini["baris"].append(baris)
    if saat_ini:
        blok.append(saat_ini)
    return [b for b in blok
            if any(re.match(r"^(GET|POST|PUT|DELETE|PATCH) ", x) for x in b["baris"])]


def bangun(blok):
    baris = list(blok["baris"])
    while baris and not baris[0].strip():
        baris.pop(0)
    metode, url = baris[0].split(" ", 1)
    headers = {}
    i = 1
    while i < len(baris) and baris[i].strip():
        nama, nilai = baris[i].split(":", 1)
        headers[nama.strip()] = nilai.strip()
        i += 1
    body = "\n".join(baris[i:]).strip()
    return (metode,
            subst(url.strip()),
            {k: subst(v) for k, v in headers.items()},
            subst(body) if body else None)


def terjemah(ekspr):
    """Menerjemahkan ekspresi assertion JavaScript menjadi ekspresi Python."""
    e = ekspr.strip()
    e = re.sub(r"(response\.body(?:\.\w+)*)\.includes\(([^)]*)\)", r"(\2 in \1)", e)
    e = re.sub(r"(response\.body(?:\.\w+)*)\.length", r"len(\1)", e)
    e = e.replace("response.status", "status")
    e = re.sub(r"response\.body\.(\w+)", r"body.get('\1')", e)
    e = e.replace("response.body", "body")
    e = e.replace("!==", "!=").replace("===", "==")
    e = e.replace("&&", " and ").replace("||", " or ")
    e = re.sub(r"(?<![=!<>])!\s*\(", "not (", e)
    e = e.replace("undefined", "None")
    return e


def jalankan(path):
    hasil = []
    for blok in parse(path):
        try:
            metode, url, headers, body = bangun(blok)
        except KeyError as ex:
            hasil.append((blok["judul"], "LEWAT", f"variabel {ex} belum terisi"))
            continue

        req = urllib.request.Request(
            url, method=metode,
            data=body.encode() if body else None, headers=headers)
        try:
            with urllib.request.urlopen(req, timeout=30) as resp:
                status, mentah = resp.status, resp.read()
        except urllib.error.HTTPError as ex:
            status, mentah = ex.code, ex.read()
        except Exception as ex:
            hasil.append((blok["judul"], "ERROR", str(ex)))
            continue

        try:
            parsed = json.loads(mentah) if mentah else None
        except ValueError:
            parsed = None
        body_obj = parsed if isinstance(parsed, dict) else {}

        naskah = "\n".join(blok["handler"])
        for nama, isi in re.findall(
                r'client\.test\("([^"]*)",\s*function\s*\(\)\s*\{(.*?)\}\s*\)\s*;',
                naskah, re.S):
            m = re.search(r"client\.assert\((.*?),\s*\"", isi, re.S)
            if not m:
                continue
            py = terjemah(m.group(1))
            try:
                lolos = eval(py, {}, {"status": status, "body": body_obj, "len": len})
            except Exception as ex:
                hasil.append((f"{blok['judul']} :: {nama}", "ERROR", f"{py} -> {ex}"))
                continue
            catatan = f"status {status}"
            if not lolos and parsed is not None:
                catatan += f" body {json.dumps(parsed)[:160]}"
            hasil.append((f"{blok['judul']} :: {nama}", "OK" if lolos else "GAGAL", catatan))

        for nama, field in re.findall(
                r'client\.global\.set\("(\w+)",\s*response\.body\.(\w+)\)', naskah):
            if body_obj.get(field) is not None:
                globals_[nama] = body_obj[field]
        if 'client.global.set("periode"' in naskah:
            globals_["periode"] = awal_bulan
    return hasil


berkas = sorted(DIR.glob("role-*.http"))
if not berkas:
    sys.exit(f"tidak ada berkas role-*.http di {DIR}")

total_ok = total_gagal = 0
for path in berkas:
    ok = gagal = 0
    baris_gagal = []
    for judul, verdikt, catatan in jalankan(path):
        if verdikt == "OK":
            ok += 1
        else:
            gagal += 1
            baris_gagal.append(f"    [{verdikt}] {judul} — {catatan}")
    total_ok += ok
    total_gagal += gagal
    tanda = "OK  " if gagal == 0 else "GAGAL"
    print(f"{tanda} {path.name:32s} {ok:3d} lolos, {gagal:2d} gagal")
    for baris in baris_gagal:
        print(baris)

print(f"\nTOTAL: {total_ok} lolos, {total_gagal} gagal")
sys.exit(1 if total_gagal else 0)
