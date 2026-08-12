package com.masesas.exercises.demo1.service;

import com.masesas.exercises.demo1.config.prop.AppConfigProperties;
import com.masesas.exercises.demo1.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageStorageService {

    private static final Map<String, String> EKSTENSI_PER_TIPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    private final AppConfigProperties properties;

    public String simpan(MultipartFile berkas, String folder) {
        String ekstensi = validasi(berkas);
        Path direktori = direktoriDasar().resolve(folder);
        String namaBerkas = UUID.randomUUID() + ekstensi;

        try (InputStream isi = berkas.getInputStream()) {
            Files.createDirectories(direktori);
            Files.copy(isi, direktori.resolve(namaBerkas), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Gagal menyimpan gambar ke " + direktori, e);
        }

        return folder + "/" + namaBerkas;
    }

    public void hapus(String lokasi) {
        if (lokasi == null || lokasi.isBlank()) {
            return;
        }

        Path dasar = direktoriDasar();
        Path berkas = dasar.resolve(lokasi).normalize();
        if (!berkas.startsWith(dasar)) {
            throw new InvalidRequestException("lokasi gambar tidak valid");
        }

        try {
            Files.deleteIfExists(berkas);
        } catch (IOException e) {
            throw new UncheckedIOException("Gagal menghapus gambar " + lokasi, e);
        }
    }

    private Path direktoriDasar() {
        return Path.of(properties.getImage().getBaseDir()).toAbsolutePath().normalize();
    }

    private String validasi(MultipartFile berkas) {
        if (berkas == null || berkas.isEmpty()) {
            throw new InvalidRequestException("file wajib diisi");
        }

        long ukuranMaksimumByte = properties.getImage().getMaxSize().toBytes();
        if (berkas.getSize() > ukuranMaksimumByte) {
            throw new InvalidRequestException("ukuran gambar melebihi " + ukuranMaksimumByte + " byte");
        }

        String ekstensi = EKSTENSI_PER_TIPE.get(berkas.getContentType());
        if (ekstensi == null) {
            throw new InvalidRequestException("tipe gambar harus image/jpeg, image/png, atau image/webp");
        }
        return ekstensi;
    }
}
