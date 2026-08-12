package com.masesas.exercises.demo1.service;

import com.masesas.exercises.demo1.config.prop.AppConfigProperties;
import com.masesas.exercises.demo1.exception.InvalidRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DisplayName("ImageStorageService")
class ImageStorageServiceTest {

    private static final String FOLDER = "karyawan";
    private static final byte[] ISI = "gambar-palsu".getBytes();

    @TempDir
    Path direktoriDasar;

    private ImageStorageService imageStorageService;

    @BeforeEach
    void setUp() {
        AppConfigProperties.Image image = new AppConfigProperties.Image();
        image.setBaseDir(direktoriDasar.toString());
        image.setMaxSize(DataSize.ofBytes(100));

        AppConfigProperties properties = new AppConfigProperties();
        properties.setImage(image);

        imageStorageService = new ImageStorageService(properties);
    }

    @Test
    @DisplayName("simpan menulis berkas ke <base-dir>/karyawan dan mengembalikan lokasi relatif")
    void simpan_berkasValid_menulisKeFolderKaryawan() throws IOException {
        MockMultipartFile berkas = new MockMultipartFile("file", "foto.png", "image/png", ISI);

        String lokasi = imageStorageService.simpan(berkas, FOLDER);

        assertThat(lokasi).startsWith(FOLDER + "/").endsWith(".png");
        Path tersimpan = direktoriDasar.resolve(lokasi);
        assertThat(tersimpan).exists();
        assertThat(Files.readAllBytes(tersimpan)).isEqualTo(ISI);
    }

    @Test
    @DisplayName("simpan mengabaikan nama berkas kiriman klien sehingga path traversal tidak mungkin")
    void simpan_namaBerkasJahat_tidakDipakaiSebagaiPath() {
        MockMultipartFile berkas = new MockMultipartFile(
                "file", "../../../etc/passwd.png", "image/png", ISI);

        String lokasi = imageStorageService.simpan(berkas, FOLDER);

        assertThat(lokasi).doesNotContain("..");
        assertThat(direktoriDasar.resolve(lokasi).normalize()).startsWith(direktoriDasar);
    }

    @Test
    @DisplayName("simpan menolak berkas kosong")
    void simpan_berkasKosong_menolak() {
        MockMultipartFile berkas = new MockMultipartFile("file", "foto.png", "image/png", new byte[0]);

        Throwable error = catchThrowable(() -> imageStorageService.simpan(berkas, FOLDER));

        assertThat(error).isInstanceOf(InvalidRequestException.class).hasMessageContaining("file");
    }

    @Test
    @DisplayName("simpan menolak tipe konten di luar daftar izin")
    void simpan_tipeTidakDiizinkan_menolak() {
        MockMultipartFile berkas = new MockMultipartFile(
                "file", "skrip.pdf", "application/pdf", ISI);

        Throwable error = catchThrowable(() -> imageStorageService.simpan(berkas, FOLDER));

        assertThat(error).isInstanceOf(InvalidRequestException.class).hasMessageContaining("tipe gambar");
    }

    @Test
    @DisplayName("simpan menolak berkas melebihi app.image.max-size")
    void simpan_berkasTerlaluBesar_menolak() {
        MockMultipartFile berkas = new MockMultipartFile(
                "file", "foto.png", "image/png", new byte[101]);

        Throwable error = catchThrowable(() -> imageStorageService.simpan(berkas, FOLDER));

        assertThat(error).isInstanceOf(InvalidRequestException.class).hasMessageContaining("ukuran gambar");
    }

    @Test
    @DisplayName("hapus menghapus berkas yang sudah tersimpan")
    void hapus_lokasiAda_berkasHilang() {
        MockMultipartFile berkas = new MockMultipartFile("file", "foto.png", "image/png", ISI);
        String lokasi = imageStorageService.simpan(berkas, FOLDER);

        imageStorageService.hapus(lokasi);

        assertThat(direktoriDasar.resolve(lokasi)).doesNotExist();
    }

    @Test
    @DisplayName("hapus mengabaikan lokasi kosong")
    void hapus_lokasiKosong_tidakMelempar() {
        Throwable error = catchThrowable(() -> imageStorageService.hapus(null));

        assertThat(error).isNull();
    }

    @Test
    @DisplayName("hapus menolak lokasi yang keluar dari direktori dasar")
    void hapus_lokasiKeluarDirektoriDasar_menolak() {
        Throwable error = catchThrowable(() -> imageStorageService.hapus("../rahasia.png"));

        assertThat(error).isInstanceOf(InvalidRequestException.class);
    }
}
