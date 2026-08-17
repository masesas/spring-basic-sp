# Impl Plan — Prasyarat Server Ubuntu

Langkah manual yang dijalankan **sekali** di server sebelum deploy pertama.
Tidak ada satu pun dari langkah ini yang diulang oleh workflow — kalau salah
satunya terlewat, deploy akan gagal di langkah yang bisa ditebak, dan tabel
diagnosa di bagian akhir memetakan gejalanya.

Seluruh perintah dijalankan sebagai user yang nantinya dipakai deploy
(`SSH_USERNAME_*`, saat ini `masesas`).

---

## 1. Docker Engine dan plugin Compose

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker "$USER"
```

Keluar dan masuk lagi supaya keanggotaan grup berlaku, lalu pastikan keduanya
terpasang:

```bash
docker version
docker compose version     # harus v2.x — `docker-compose` v1 dengan tanda hubung tidak dipakai
```

`deploy.sh` memanggil `docker compose` tanpa tanda hubung. Versi 1 tidak mengenal
`--env-file` dengan perilaku yang sama dan tidak didukung di sini.

## 2. Kunci SSH khusus deploy

Dibuat di mesin lokal, bukan di server — private key tidak boleh pernah ada di
server.

```bash
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/demo1_deploy -N ""
```

Kunci publiknya dipasang di server:

```bash
ssh-copy-id -i ~/.ssh/demo1_deploy.pub masesas@<host>
```

Kunci privatnya (`~/.ssh/demo1_deploy`, **tanpa** akhiran `.pub`) disalin utuh
menjadi secret `SSH_PRIVATE_KEY_DEV` dan `SSH_PRIVATE_KEY_PROD` di GitHub,
termasuk baris `-----BEGIN OPENSSH PRIVATE KEY-----` dan `-----END ...-----`.

Uji dari mesin lokal sebelum menyerahkannya ke GitHub:

```bash
ssh -i ~/.ssh/demo1_deploy masesas@<host> "echo koneksi ok"
```

Satu pasang kunci dipakai untuk dev dan prod karena keduanya menuju server yang
sama dengan user yang sama. Saat server prod dipisah nanti, buat pasangan kedua.

## 3. Direktori deploy dan direktori data

Keduanya dibuat otomatis oleh pipeline — **tidak ada langkah manual di sini.**

Direktori deploy dibuat workflow lewat `mkdir -p` sebelum `scp`. Direktori data
dibuat `scripts/deploy.sh`, yang juga mengurus pemiliknya:

```bash
mkdir -p "$APP_DATA_DIR"
if [ "$(stat -c %u "$APP_DATA_DIR")" != "1001" ]; then
    docker run --rm -u 0 --entrypoint chown \
        -v "$APP_DATA_DIR:/data" "$APP_IMAGE" -R 1001:1001 /data
fi
```

`mkdir` tidak butuh `sudo` karena `/home/masesas` sudah milik user deploy.
Pemiliknya perlu UID 1001 — UID user di dalam container, sesuai
[impl-plan-dockerfile.md](./impl-plan-dockerfile.md) — dan itu menuntut root.
Root didapat dari container sekali pakai, bukan dari `sudo`, sehingga user
deploy tetap tanpa hak istimewa apa pun di server. Image yang dipakai adalah
image aplikasi yang baru ditarik, jadi tidak ada dependensi baru.

Tanpa `chown`, aplikasi berjalan tapi setiap unggahan avatar gagal dengan
`Permission denied` — dan itu baru ketahuan saat seseorang mencoba mengunggah,
bukan saat deploy. Karena itu pemeriksaannya dijalankan setiap deploy, bukan
sekali di awal.

## 4. Login ke GHCR

Image di GHCR bersifat privat secara bawaan, jadi server perlu kredensial untuk
menariknya. Kredensial ini **tidak** disimpan di GitHub Secrets dan tidak pernah
dikirim lewat pipeline — server login sekali, hasilnya tersimpan di
`~/.docker/config.json`.

Buat Personal Access Token (classic) dengan scope **`read:packages`** saja di
GitHub → Settings → Developer settings → Personal access tokens, lalu di server:

```bash
echo "<PAT>" | docker login ghcr.io -u masesas --password-stdin
```

Uji dengan menarik image apa pun yang sudah ada di GHCR:

```bash
docker pull ghcr.io/masesas/demo1-dev:<tag-yang-ada>
```

Alternatifnya, package `demo1-dev` dan `demo1-prod` diubah menjadi publik di
halaman package GHCR, dan langkah login ini tidak diperlukan sama sekali. Yang
menjadi publik hanyalah image jadi, bukan source code — tapi image itu memuat
seluruh kode aplikasi dalam bentuk jar, jadi ini pilihan yang perlu disadari,
bukan jalan pintas.

## 5. Firewall

```bash
sudo ufw allow 8080/tcp     # dev
sudo ufw allow 8081/tcp     # prod
sudo ufw status
```

Port SSH sudah terbuka karena deploy memakainya.

## 6. Zona waktu server

```bash
timedatectl set-timezone Asia/Jakarta
```

Container punya `TZ` sendiri lewat `.env.app`, jadi ini hanya membuat log Docker
dan `docker ps` di server ikut selaras.

---

## Checklist

- [ ] `docker version` dan `docker compose version` (v2) berjalan tanpa `sudo`
- [ ] Kunci SSH dibuat di mesin lokal, publiknya terpasang di server
- [ ] `ssh -i ~/.ssh/demo1_deploy masesas@<host>` berhasil tanpa password
- [ ] Private key disalin ke secret `SSH_PRIVATE_KEY_DEV` dan `SSH_PRIVATE_KEY_PROD`
- [ ] `docker login ghcr.io` berhasil, atau kedua package dijadikan publik
- [ ] Port 8080 dan 8081 terbuka di firewall
- [ ] Zona waktu server `Asia/Jakarta`

Verifikasi cepat seluruhnya:

```bash
docker compose version
ls -ln /home/masesas/data/demo1-dev/images | head -1   # pemilik harus 1001
grep -q ghcr.io ~/.docker/config.json && echo "ghcr login ok"
sudo ufw status | grep -E '808[01]'
timedatectl | grep "Time zone"
```

## Diagnosa Kegagalan

| Gejala saat deploy | Prasyarat yang terlewat |
|---|---|
| `Permission denied (publickey)` di step SSH | Langkah 2 — kunci publik belum terpasang, atau secret berisi `.pub` |
| `denied: denied` saat `docker pull` | Langkah 4 — server belum login ke GHCR, atau PAT kedaluwarsa |
| `docker: command not found` | Langkah 1 |
| `permission denied while trying to connect to the Docker daemon` | Langkah 1 — user belum masuk grup `docker`, atau belum login ulang |
| `mkdir: cannot create directory` di `deploy.sh` | Langkah 3 — `/home/masesas` bukan milik user deploy |
| Container `healthy` tapi `curl` dari luar timeout | Langkah 5 |
| Unggah avatar gagal `Permission denied`, hal lain normal | Langkah 3 — direktori ada tapi pemiliknya bukan 1001 |
| `docker compose: 'compose' is not a docker command` | Langkah 1 — plugin Compose v2 belum terpasang |

## Batasan Explicit

- **Satu server menjalankan dev dan prod.** Beban dev memengaruhi prod, dan
  `docker` yang bermasalah menjatuhkan keduanya sekaligus.
- **PAT `read:packages` di server tidak punya masa berlaku yang dipantau
  apa pun.** Saat kedaluwarsa, deploy berikutnya gagal di `docker pull` tanpa
  peringatan sebelumnya.
- **Tidak ada pemantauan.** Container yang mati setelah deploy hanya akan
  dihidupkan lagi oleh `restart: unless-stopped`; tidak ada yang memberi tahu
  siapa pun kalau itu terjadi berulang kali.
- **Langkah-langkah ini tidak otomatis dan tidak diperiksa ulang oleh pipeline.**
  Server yang dibangun ulang harus melewati seluruh daftar ini lagi secara manual.
