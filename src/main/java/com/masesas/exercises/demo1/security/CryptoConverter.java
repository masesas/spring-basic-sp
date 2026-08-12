package com.masesas.exercises.demo1.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@Converter
public class CryptoConverter implements AttributeConverter<String, String> {

    private static final String PENANDA = "enc:v1:";
    private static final String TRANSFORMASI = "AES/GCM/NoPadding";
    private static final int PANJANG_IV = 12;
    private static final int PANJANG_TAG_BIT = 128;

    private final SecretKey kunci;
    private final SecureRandom acak = new SecureRandom();

    public CryptoConverter(@Value("${app.security.crypto-key}") String kunciBase64) {
        this.kunci = new SecretKeySpec(Base64.getDecoder().decode(kunciBase64), "AES");
    }

    @Override
    public String convertToDatabaseColumn(String nilai) {
        if (nilai == null || nilai.isEmpty()) {
            return nilai;
        }
        try {
            byte[] iv = new byte[PANJANG_IV];
            acak.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMASI);
            cipher.init(Cipher.ENCRYPT_MODE, kunci, new GCMParameterSpec(PANJANG_TAG_BIT, iv));
            byte[] terenkripsi = cipher.doFinal(nilai.getBytes(StandardCharsets.UTF_8));

            byte[] gabungan = new byte[iv.length + terenkripsi.length];
            System.arraycopy(iv, 0, gabungan, 0, iv.length);
            System.arraycopy(terenkripsi, 0, gabungan, iv.length, terenkripsi.length);

            return PENANDA + Base64.getEncoder().encodeToString(gabungan);
        } catch (Exception ex) {
            throw new IllegalStateException("Gagal mengenkripsi data sensitif", ex);
        }
    }

    @Override
    public String convertToEntityAttribute(String tersimpan) {
        if (tersimpan == null || !tersimpan.startsWith(PENANDA)) {
            return tersimpan;
        }
        try {
            byte[] gabungan = Base64.getDecoder().decode(tersimpan.substring(PENANDA.length()));

            byte[] iv = new byte[PANJANG_IV];
            System.arraycopy(gabungan, 0, iv, 0, PANJANG_IV);

            Cipher cipher = Cipher.getInstance(TRANSFORMASI);
            cipher.init(Cipher.DECRYPT_MODE, kunci, new GCMParameterSpec(PANJANG_TAG_BIT, iv));
            byte[] asli = cipher.doFinal(gabungan, PANJANG_IV, gabungan.length - PANJANG_IV);

            return new String(asli, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Gagal mendekripsi data sensitif", ex);
        }
    }
}
