package com.masesas.exercises.demo1.owasp.safe;

import com.masesas.exercises.demo1.exception.InvalidRequestException;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

public final class UrlGuard {

    private static final String SKEMA_DIIZINKAN = "https";

    private UrlGuard() {
    }

    public static URI periksa(String url) {
        URI uri = parse(url);
        requireSkemaAman(uri);
        requireHostPublik(uri);
        return uri;
    }

    private static URI parse(String url) {
        if (url == null || url.isBlank()) {
            throw new InvalidRequestException("url wajib diisi");
        }
        try {
            URI uri = new URI(url.trim());
            if (uri.getHost() == null) {
                throw new InvalidRequestException("url tidak memuat host yang sah");
            }
            return uri;
        } catch (URISyntaxException ex) {
            throw new InvalidRequestException("url tidak berbentuk alamat yang sah");
        }
    }

    private static void requireSkemaAman(URI uri) {
        if (!SKEMA_DIIZINKAN.equalsIgnoreCase(uri.getScheme())) {
            throw new InvalidRequestException(
                    "hanya skema https yang diterima, bukan " + uri.getScheme());
        }
    }

    /**
     * Menolak alamat internal. Nama host di-resolve dulu karena penolakan berdasarkan
     * teks tidak ada gunanya: {@code http://localtest.me} dan {@code http://2130706433}
     * sama-sama bermuara ke 127.0.0.1 tanpa pernah menuliskannya.
     *
     * <p>Semua hasil resolve diperiksa, bukan hanya yang pertama — satu nama host bisa
     * menunjuk ke beberapa alamat sekaligus.
     */
    private static void requireHostPublik(URI uri) {
        try {
            for (InetAddress alamat : InetAddress.getAllByName(uri.getHost())) {
                if (bukanAlamatPublik(alamat)) {
                    throw new InvalidRequestException(
                            "alamat internal tidak boleh dituju: " + alamat.getHostAddress());
                }
            }
        } catch (UnknownHostException ex) {
            throw new InvalidRequestException("host tidak dikenal: " + uri.getHost());
        }
    }

    private static boolean bukanAlamatPublik(InetAddress alamat) {
        return alamat.isLoopbackAddress()
                || alamat.isSiteLocalAddress()
                || alamat.isLinkLocalAddress()
                || alamat.isAnyLocalAddress()
                || alamat.isMulticastAddress()
                || uniqueLocalIpv6(alamat);
    }

    /** fc00::/7 — padanan IPv6 untuk 10.x/192.168.x, tidak tercakup isSiteLocalAddress. */
    private static boolean uniqueLocalIpv6(InetAddress alamat) {
        byte[] byteAlamat = alamat.getAddress();
        return byteAlamat.length == 16 && (byteAlamat[0] & 0xFE) == 0xFC;
    }
}
