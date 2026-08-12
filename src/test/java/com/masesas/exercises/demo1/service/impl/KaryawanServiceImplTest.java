package com.masesas.exercises.demo1.service.impl;

import com.masesas.exercises.demo1.dto.CreateKaryawanRequest;
import com.masesas.exercises.demo1.dto.DetailKaryawanRequest;
import com.masesas.exercises.demo1.dto.KaryawanResponse;
import com.masesas.exercises.demo1.dto.UpdateKaryawanRequest;
import com.masesas.exercises.demo1.entity.DetailKaryawan;
import com.masesas.exercises.demo1.entity.Karyawan;
import com.masesas.exercises.demo1.exception.InvalidRequestException;
import com.masesas.exercises.demo1.exception.ResourceNotFoundException;
import com.masesas.exercises.demo1.repository.KaryawanRepository;
import com.masesas.exercises.demo1.repository.KaryawanTrainingRepository;
import com.masesas.exercises.demo1.repository.RekeningRepository;
import com.masesas.exercises.demo1.service.ImageStorageService;
import com.masesas.exercises.demo1.service.support.DetailKaryawanWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KaryawanServiceImpl")
class KaryawanServiceImplTest {

    private static final Integer ID = 7;
    private static final Integer ID_TIDAK_ADA = 404;
    private static final LocalDate DOB = LocalDate.of(1990, 1, 1);

    @Mock
    private KaryawanRepository karyawanRepository;

    @Mock
    private RekeningRepository rekeningRepository;

    @Mock
    private KaryawanTrainingRepository karyawanTrainingRepository;

    @Mock
    private DetailKaryawanWriter detailKaryawanWriter;

    @Mock
    private ImageStorageService imageStorageService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private KaryawanServiceImpl karyawanService;

    @Captor
    private ArgumentCaptor<Karyawan> karyawanTersimpan;

    @Test
    @DisplayName("findById mengembalikan karyawan aktif")
    void findById_karyawanAktif_mengembalikanResponse() {
        Karyawan karyawanAktif = new Karyawan();
        karyawanAktif.setId(7);
        karyawanAktif.setStatus("AKTIF");
        karyawanAktif.setNama("Budi");
        when(karyawanRepository.findByIdAndDeletedDateIsNull(7)).thenReturn(Optional.of(karyawanAktif));

        KaryawanResponse hasil = karyawanService.findById(ID);

        assertThat(hasil.getId()).isEqualTo(7);
        assertThat(hasil.getNama()).isEqualTo("Budi");
        assertThat(hasil.getStatus()).isEqualTo("AKTIF");
    }







    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("data valid disimpan dengan nilai yang sudah dirapikan")
        void create_dataValid_menyimpanKaryawanRapi() {
            CreateKaryawanRequest request =
                    new CreateKaryawanRequest("  Budi  ", "  Jakarta  ", DOB, "AKTIF", null);
            when(karyawanRepository.save(any(Karyawan.class))).thenAnswer(kembalikanArgumenPertama());

            KaryawanResponse hasil = karyawanService.create(request);

            verify(karyawanRepository).save(karyawanTersimpan.capture());
            Karyawan disimpan = karyawanTersimpan.getValue();
            assertThat(disimpan.getNama()).isEqualTo("Budi");
            assertThat(disimpan.getAlamat()).isEqualTo("Jakarta");
            assertThat(disimpan.getDob()).isEqualTo(DOB);
            assertThat(disimpan.getStatus()).isEqualTo("AKTIF");
            assertThat(disimpan.getCreatedDate()).isEqualTo(disimpan.getUpdatedDate());
            assertThat(disimpan.getDeletedDate()).isNull();
            assertThat(hasil.getNama()).isEqualTo("Budi");
            assertThat(hasil.getDetail()).isNull();
        }

        @Test
        @DisplayName("detail ikut dibuat ketika request menyertakannya")
        void create_denganDetail_membuatDetailKaryawan() {
            DetailKaryawanRequest detailRequest = new DetailKaryawanRequest("1234567890123456", "123456789012345");
            CreateKaryawanRequest request =
                    new CreateKaryawanRequest("Budi", "Jakarta", DOB, "AKTIF", detailRequest);
            when(detailKaryawanWriter.create(eq(detailRequest), any(Instant.class))).thenReturn(detailAktif());
            when(karyawanRepository.save(any(Karyawan.class))).thenAnswer(kembalikanArgumenPertama());

            KaryawanResponse hasil = karyawanService.create(request);

            verify(detailKaryawanWriter).create(eq(detailRequest), any(Instant.class));
            assertThat(hasil.getDetail()).isNotNull();
            assertThat(hasil.getDetail().getNik()).isEqualTo("************3456");
            assertThat(hasil.getDetail().getNpwp()).isEqualTo("***********2345");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("alamat kosong disimpan sebagai null, bukan string kosong")
        void create_alamatKosong_disimpanSebagaiNull(String alamat) {
            CreateKaryawanRequest request = new CreateKaryawanRequest("Budi", alamat, DOB, "AKTIF", null);
            when(karyawanRepository.save(any(Karyawan.class))).thenAnswer(kembalikanArgumenPertama());

            karyawanService.create(request);

            verify(karyawanRepository).save(karyawanTersimpan.capture());
            assertThat(karyawanTersimpan.getValue().getAlamat()).isNull();
        }

        @Test
        @DisplayName("request null ditolak sebelum menyentuh database")
        void create_requestNull_melemparInvalidRequest() {
            CreateKaryawanRequest request = null;

            Throwable dilempar = catchThrowable(() -> karyawanService.create(request));

            assertThat(dilempar)
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("data karyawan");
            verifyNoInteractions(karyawanRepository);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("nama kosong ditolak")
        void create_namaKosong_melemparInvalidRequest(String nama) {
            CreateKaryawanRequest request = new CreateKaryawanRequest(nama, "Jakarta", DOB, "AKTIF", null);

            Throwable dilempar = catchThrowable(() -> karyawanService.create(request));

            assertThat(dilempar)
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("nama");
            verifyNoInteractions(karyawanRepository);
        }

        @Test
        @DisplayName("dob di masa depan ditolak")
        void create_dobMasaDepan_melemparInvalidRequest() {
            LocalDate besok = LocalDate.now().plusDays(1);
            CreateKaryawanRequest request = new CreateKaryawanRequest("Budi", "Jakarta", besok, "AKTIF", null);

            Throwable dilempar = catchThrowable(() -> karyawanService.create(request));

            assertThat(dilempar)
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("dob");
            verifyNoInteractions(karyawanRepository);
        }

        @Test
        @DisplayName("dob null ditolak")
        void create_dobNull_melemparInvalidRequest() {
            CreateKaryawanRequest request = new CreateKaryawanRequest("Budi", "Jakarta", null, "AKTIF", null);

            Throwable dilempar = catchThrowable(() -> karyawanService.create(request));

            assertThat(dilempar).isInstanceOf(InvalidRequestException.class);
            verifyNoInteractions(karyawanRepository);
        }

        @Test
        @DisplayName("status kosong ditolak")
        void create_statusKosong_melemparInvalidRequest() {
            CreateKaryawanRequest request = new CreateKaryawanRequest("Budi", "Jakarta", DOB, null, null);

            Throwable dilempar = catchThrowable(() -> karyawanService.create(request));

            assertThat(dilempar)
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("status");
            verifyNoInteractions(karyawanRepository);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("karyawan aktif diperbarui dan updatedDate ikut bergeser")
        void update_karyawanAktif_memperbaruiField() {
            Karyawan tersimpan = karyawanAktif();
            Instant sebelum = tersimpan.getUpdatedDate();
            when(karyawanRepository.findByIdAndDeletedDateIsNull(ID)).thenReturn(Optional.of(tersimpan));
            when(karyawanRepository.save(any(Karyawan.class))).thenAnswer(kembalikanArgumenPertama());

            KaryawanResponse hasil = karyawanService.update(
                    ID, new UpdateKaryawanRequest("Budi Baru", "Bandung", DOB, "NONAKTIF"));

            assertThat(hasil.getNama()).isEqualTo("Budi Baru");
            assertThat(hasil.getAlamat()).isEqualTo("Bandung");
            assertThat(hasil.getStatus()).isEqualTo("NONAKTIF");
            assertThat(tersimpan.getUpdatedDate()).isAfter(sebelum);
            assertThat(tersimpan.getCreatedDate()).isEqualTo(sebelum);
        }

        @Test
        @DisplayName("id yang tidak ada atau sudah dihapus membalas ResourceNotFound")
        void update_idTidakDitemukan_melemparResourceNotFound() {
            when(karyawanRepository.findByIdAndDeletedDateIsNull(ID_TIDAK_ADA)).thenReturn(Optional.empty());

            Throwable dilempar = catchThrowable(() -> karyawanService.update(
                    ID_TIDAK_ADA, new UpdateKaryawanRequest("Budi", "Jakarta", DOB, "AKTIF")));

            assertThat(dilempar)
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Karyawan")
                    .hasMessageContaining(String.valueOf(ID_TIDAK_ADA));
            verify(karyawanRepository, never()).save(any(Karyawan.class));
        }

        @Test
        @DisplayName("request null ditolak sebelum data dibaca")
        void update_requestNull_melemparInvalidRequest() {
            Throwable dilempar = catchThrowable(() -> karyawanService.update(ID, null));

            assertThat(dilempar).isInstanceOf(InvalidRequestException.class);
            verifyNoInteractions(karyawanRepository);
        }

        @Test
        @DisplayName("id null ditolak sebagai permintaan tidak valid, bukan not found")
        void update_idNull_melemparInvalidRequest() {
            Throwable dilempar = catchThrowable(() -> karyawanService.update(
                    null, new UpdateKaryawanRequest("Budi", "Jakarta", DOB, "AKTIF")));

            assertThat(dilempar)
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("id karyawan");
            verifyNoInteractions(karyawanRepository);
        }
    }

    @Nested
    @DisplayName("pembacaan")
    class Pembacaan {

        @Test
        @DisplayName("findById mengembalikan karyawan aktif")
        void findById_karyawanAktif_mengembalikanResponse() {
            when(karyawanRepository.findByIdAndDeletedDateIsNull(ID)).thenReturn(Optional.of(karyawanAktif()));

            KaryawanResponse hasil = karyawanService.findById(ID);

            assertThat(hasil.getId()).isEqualTo(ID);
            assertThat(hasil.getNama()).isEqualTo("Budi");
        }

        @Test
        @DisplayName("findById pada karyawan terhapus membalas ResourceNotFound")
        void findById_karyawanTerhapus_melemparResourceNotFound() {
            when(karyawanRepository.findByIdAndDeletedDateIsNull(ID_TIDAK_ADA)).thenReturn(Optional.empty());

            Throwable dilempar = catchThrowable(() -> karyawanService.findById(ID_TIDAK_ADA));

            assertThat(dilempar).isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("findAll meneruskan Pageable apa adanya ke repository")
        void findAll_denganPageable_meneruskanPageable() {
            Pageable pageable = PageRequest.of(2, 5);
            when(karyawanRepository.findAllByDeletedDateIsNull(pageable))
                    .thenReturn(new PageImpl<>(List.of(karyawanAktif()), pageable, 11));

            Page<KaryawanResponse> hasil = karyawanService.findAll(pageable);

            assertThat(hasil.getContent()).singleElement()
                    .extracting(KaryawanResponse::getNama).isEqualTo("Budi");
            assertThat(hasil.getTotalElements()).isEqualTo(11);
            verify(karyawanRepository).findAllByDeletedDateIsNull(pageable);
        }

        @Test
        @DisplayName("findPage menyusun Pageable dari page dan size lalu mengurutkan berdasarkan id")
        void findPage_pageDanSize_menyusunPageableTerurutId() {
            ArgumentCaptor<Pageable> pageableDipakai = ArgumentCaptor.forClass(Pageable.class);
            when(karyawanRepository.findAllByDeletedDateIsNull(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(karyawanAktif())));

            karyawanService.findPage(3, 25);

            verify(karyawanRepository).findAllByDeletedDateIsNull(pageableDipakai.capture());
            Pageable pageable = pageableDipakai.getValue();
            assertThat(pageable.getPageNumber()).isEqualTo(3);
            assertThat(pageable.getPageSize()).isEqualTo(25);
            assertThat(pageable.getSort().getOrderFor("id")).isNotNull();
        }

        @Test
        @DisplayName("findPageByNama mempertahankan total elemen dari hasil pencarian")
        void findPageByNama_adaHasil_mempertahankanTotalElemen() {
            when(karyawanRepository.findAllByNamaContainingIgnoreCaseAndDeletedDateIsNull(
                    eq("bud"), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(karyawanAktif()), PageRequest.of(0, 10), 42));

            Page<KaryawanResponse> hasil = karyawanService.findPageByNama("bud", 0, 10);

            assertThat(hasil.getContent()).singleElement()
                    .extracting(KaryawanResponse::getNama).isEqualTo("Budi");
            assertThat(hasil.getTotalElements()).isEqualTo(42);
        }

        @Test
        @DisplayName("findPageByNama tanpa hasil mengembalikan halaman kosong")
        void findPageByNama_tanpaHasil_mengembalikanHalamanKosong() {
            when(karyawanRepository.findAllByNamaContainingIgnoreCaseAndDeletedDateIsNull(
                    eq("zzz"), any(Pageable.class)))
                    .thenReturn(Page.empty());

            Page<KaryawanResponse> hasil = karyawanService.findPageByNama("zzz", 0, 10);

            assertThat(hasil.getContent()).isEmpty();
            assertThat(hasil.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("findAll tanpa pagination mengembalikan daftar yang masih bisa diubah")
        void findAll_tanpaPagination_mengembalikanDaftarMutable() {
            when(karyawanRepository.findAllByDeletedDateIsNullOrderByIdAsc())
                    .thenReturn(List.of(karyawanAktif()));

            List<KaryawanResponse> hasil = karyawanService.findAll();

            assertThat(hasil).hasSize(1);
            assertThat(hasil.add(new KaryawanResponse())).isTrue();
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("soft delete karyawan beserta detail, rekening, dan training")
        void delete_karyawanDenganDetailAktif_menghapusSeluruhTurunan() {
            Karyawan tersimpan = karyawanAktif();
            DetailKaryawan detail = detailAktif();
            tersimpan.setDetailKaryawan(detail);
            when(karyawanRepository.findByIdAndDeletedDateIsNull(ID)).thenReturn(Optional.of(tersimpan));

            karyawanService.delete(ID);

            assertThat(tersimpan.getDeletedDate()).isNotNull();
            assertThat(tersimpan.getUpdatedDate()).isEqualTo(tersimpan.getDeletedDate());
            verify(karyawanRepository).save(tersimpan);
            verify(detailKaryawanWriter).softDelete(eq(detail), any(Instant.class));
            verify(rekeningRepository).softDeleteByKaryawanId(eq(ID), any(Instant.class));
            verify(karyawanTrainingRepository).softDeleteByKaryawanId(eq(ID), any(Instant.class));
        }

        @Test
        @DisplayName("karyawan tanpa detail tetap melepas rekening dan training")
        void delete_tanpaDetail_tetapMenghapusRekeningDanTraining() {
            Karyawan tersimpan = karyawanAktif();
            when(karyawanRepository.findByIdAndDeletedDateIsNull(ID)).thenReturn(Optional.of(tersimpan));

            karyawanService.delete(ID);

            verify(detailKaryawanWriter, never()).softDelete(any(DetailKaryawan.class), any(Instant.class));
            verify(rekeningRepository).softDeleteByKaryawanId(eq(ID), any(Instant.class));
            verify(karyawanTrainingRepository).softDeleteByKaryawanId(eq(ID), any(Instant.class));
        }

        @Test
        @DisplayName("detail yang sudah terhapus tidak dihapus ulang")
        void delete_detailSudahTerhapus_tidakSoftDeleteUlang() {
            Karyawan tersimpan = karyawanAktif();
            tersimpan.setDetailKaryawan(detailTerhapus());
            when(karyawanRepository.findByIdAndDeletedDateIsNull(ID)).thenReturn(Optional.of(tersimpan));

            karyawanService.delete(ID);

            verify(detailKaryawanWriter, never()).softDelete(any(DetailKaryawan.class), any(Instant.class));
        }

        @Test
        @DisplayName("id yang tidak ada membalas ResourceNotFound tanpa menyentuh tabel turunan")
        void delete_idTidakDitemukan_melemparResourceNotFound() {
            when(karyawanRepository.findByIdAndDeletedDateIsNull(ID_TIDAK_ADA)).thenReturn(Optional.empty());

            Throwable dilempar = catchThrowable(() -> karyawanService.delete(ID_TIDAK_ADA));

            assertThat(dilempar).isInstanceOf(ResourceNotFoundException.class);
            verifyNoInteractions(rekeningRepository, karyawanTrainingRepository);
        }
    }

    @Nested
    @DisplayName("detail karyawan")
    class Detail {

        @Test
        @DisplayName("upsert pada karyawan tanpa detail membuat detail baru")
        void upsertDetail_belumPunyaDetail_membuatDetailBaru() {
            Karyawan tersimpan = karyawanAktif();
            DetailKaryawanRequest request = new DetailKaryawanRequest("1234567890123456", "123456789012345");
            when(karyawanRepository.findByIdAndDeletedDateIsNull(ID)).thenReturn(Optional.of(tersimpan));
            when(detailKaryawanWriter.create(eq(request), any(Instant.class))).thenReturn(detailAktif());
            when(karyawanRepository.save(any(Karyawan.class))).thenAnswer(kembalikanArgumenPertama());

            KaryawanResponse hasil = karyawanService.upsertDetail(ID, request);

            verify(detailKaryawanWriter).create(eq(request), any(Instant.class));
            verify(detailKaryawanWriter, never()).update(any(), any(), any());
            assertThat(hasil.getDetail()).isNotNull();
        }

        @Test
        @DisplayName("upsert pada detail yang sudah terhapus membuat detail baru, bukan menghidupkan yang lama")
        void upsertDetail_detailSudahTerhapus_membuatDetailBaru() {
            Karyawan tersimpan = karyawanAktif();
            tersimpan.setDetailKaryawan(detailTerhapus());
            DetailKaryawanRequest request = new DetailKaryawanRequest("1234567890123456", "123456789012345");
            when(karyawanRepository.findByIdAndDeletedDateIsNull(ID)).thenReturn(Optional.of(tersimpan));
            when(detailKaryawanWriter.create(eq(request), any(Instant.class))).thenReturn(detailAktif());
            when(karyawanRepository.save(any(Karyawan.class))).thenAnswer(kembalikanArgumenPertama());

            karyawanService.upsertDetail(ID, request);

            verify(detailKaryawanWriter).create(eq(request), any(Instant.class));
            verify(detailKaryawanWriter, never()).update(any(), any(), any());
        }

        @Test
        @DisplayName("upsert pada detail aktif memperbarui detail yang ada")
        void upsertDetail_detailAktif_memperbaruiDetailLama() {
            Karyawan tersimpan = karyawanAktif();
            DetailKaryawan detail = detailAktif();
            tersimpan.setDetailKaryawan(detail);
            DetailKaryawanRequest request = new DetailKaryawanRequest("1234567890123456", "123456789012345");
            when(karyawanRepository.findByIdAndDeletedDateIsNull(ID)).thenReturn(Optional.of(tersimpan));
            when(karyawanRepository.save(any(Karyawan.class))).thenAnswer(kembalikanArgumenPertama());

            karyawanService.upsertDetail(ID, request);

            verify(detailKaryawanWriter).update(eq(detail), eq(request), any(Instant.class));
            verify(detailKaryawanWriter, never()).create(any(), any());
            assertThat(tersimpan.getDetailKaryawan()).isSameAs(detail);
        }

        @Test
        @DisplayName("upsert pada id yang tidak ada membalas ResourceNotFound")
        void upsertDetail_idTidakDitemukan_melemparResourceNotFound() {
            when(karyawanRepository.findByIdAndDeletedDateIsNull(ID_TIDAK_ADA)).thenReturn(Optional.empty());

            Throwable dilempar = catchThrowable(() -> karyawanService.upsertDetail(
                    ID_TIDAK_ADA, new DetailKaryawanRequest("1234567890123456", "123456789012345")));

            assertThat(dilempar).isInstanceOf(ResourceNotFoundException.class);
            verifyNoInteractions(detailKaryawanWriter);
        }

        @Test
        @DisplayName("remove men-soft-delete detail aktif dan melepas relasinya")
        void removeDetail_detailAktif_melepasRelasi() {
            Karyawan tersimpan = karyawanAktif();
            DetailKaryawan detail = detailAktif();
            tersimpan.setDetailKaryawan(detail);
            when(karyawanRepository.findByIdAndDeletedDateIsNull(ID)).thenReturn(Optional.of(tersimpan));
            when(karyawanRepository.save(any(Karyawan.class))).thenAnswer(kembalikanArgumenPertama());

            KaryawanResponse hasil = karyawanService.removeDetail(ID);

            verify(detailKaryawanWriter).softDelete(eq(detail), any(Instant.class));
            assertThat(tersimpan.getDetailKaryawan()).isNull();
            assertThat(hasil.getDetail()).isNull();
        }

        @Test
        @DisplayName("remove pada karyawan tanpa detail tetap memperbarui updatedDate")
        void removeDetail_tanpaDetail_tetapMemperbaruiUpdatedDate() {
            Karyawan tersimpan = karyawanAktif();
            Instant sebelum = tersimpan.getUpdatedDate();
            when(karyawanRepository.findByIdAndDeletedDateIsNull(ID)).thenReturn(Optional.of(tersimpan));
            when(karyawanRepository.save(any(Karyawan.class))).thenAnswer(kembalikanArgumenPertama());

            karyawanService.removeDetail(ID);

            verify(detailKaryawanWriter, never()).softDelete(any(DetailKaryawan.class), any(Instant.class));
            assertThat(tersimpan.getUpdatedDate()).isAfter(sebelum);
        }

        @Test
        @DisplayName("remove pada detail yang sudah terhapus tidak menghapus ulang")
        void removeDetail_detailSudahTerhapus_tidakSoftDeleteUlang() {
            Karyawan tersimpan = karyawanAktif();
            tersimpan.setDetailKaryawan(detailTerhapus());
            when(karyawanRepository.findByIdAndDeletedDateIsNull(ID)).thenReturn(Optional.of(tersimpan));
            when(karyawanRepository.save(any(Karyawan.class))).thenAnswer(kembalikanArgumenPertama());

            karyawanService.removeDetail(ID);

            verify(detailKaryawanWriter, never()).softDelete(any(DetailKaryawan.class), any(Instant.class));
            assertThat(tersimpan.getDetailKaryawan()).isNull();
        }
    }

    @Nested
    @DisplayName("uploadAvatar")
    class UploadAvatar {

        private final MockMultipartFile file =
                new MockMultipartFile("file", "foto.png", "image/png", "gambar".getBytes());

        @Test
        @DisplayName("menyimpan gambar dan mengisi kolom avatar")
        void uploadAvatar_karyawanAktif_mengisiKolomAvatar() {
            Karyawan tersimpan = karyawanAktif();
            when(karyawanRepository.findByIdAndDeletedDateIsNull(ID)).thenReturn(Optional.of(tersimpan));
            when(karyawanRepository.save(any(Karyawan.class))).thenAnswer(kembalikanArgumenPertama());
            when(imageStorageService.simpan(file, "karyawan")).thenReturn("karyawan/baru.png");

            KaryawanResponse response = karyawanService.uploadAvatar(ID, file);

            assertThat(response.getAvatar()).isEqualTo("karyawan/baru.png");
            assertThat(tersimpan.getAvatar()).isEqualTo("karyawan/baru.png");
        }

        @Test
        @DisplayName("menghapus avatar lama setelah yang baru tersimpan")
        void uploadAvatar_sudahPunyaAvatar_menghapusYangLama() {
            Karyawan tersimpan = karyawanAktif();
            tersimpan.setAvatar("karyawan/lama.png");
            when(karyawanRepository.findByIdAndDeletedDateIsNull(ID)).thenReturn(Optional.of(tersimpan));
            when(karyawanRepository.save(any(Karyawan.class))).thenAnswer(kembalikanArgumenPertama());
            when(imageStorageService.simpan(file, "karyawan")).thenReturn("karyawan/baru.png");

            karyawanService.uploadAvatar(ID, file);

            InOrder urutan = inOrder(imageStorageService, karyawanRepository);
            urutan.verify(imageStorageService).simpan(file, "karyawan");
            urutan.verify(karyawanRepository).save(tersimpan);
            urutan.verify(imageStorageService).hapus("karyawan/lama.png");
        }

        @Test
        @DisplayName("karyawan tidak ada -> tidak menyentuh penyimpanan gambar")
        void uploadAvatar_karyawanTidakAda_melemparNotFound() {
            when(karyawanRepository.findByIdAndDeletedDateIsNull(ID_TIDAK_ADA)).thenReturn(Optional.empty());

            Throwable error = catchThrowable(() -> karyawanService.uploadAvatar(ID_TIDAK_ADA, file));

            assertThat(error).isInstanceOf(ResourceNotFoundException.class);
            verifyNoInteractions(imageStorageService);
        }

        @Test
        @DisplayName("penyimpanan gambar gagal -> data karyawan tidak ikut berubah")
        void uploadAvatar_gambarDitolak_tidakMenyimpanKaryawan() {
            Karyawan tersimpan = karyawanAktif();
            tersimpan.setAvatar("karyawan/lama.png");
            when(karyawanRepository.findByIdAndDeletedDateIsNull(ID)).thenReturn(Optional.of(tersimpan));
            when(imageStorageService.simpan(file, "karyawan"))
                    .thenThrow(new InvalidRequestException("tipe gambar harus image/jpeg, image/png, atau image/webp"));

            Throwable error = catchThrowable(() -> karyawanService.uploadAvatar(ID, file));

            assertThat(error).isInstanceOf(InvalidRequestException.class);
            assertThat(tersimpan.getAvatar()).isEqualTo("karyawan/lama.png");
            verify(karyawanRepository, never()).save(any(Karyawan.class));
        }
    }

    private static Answer<Karyawan> kembalikanArgumenPertama() {
        return invocation -> invocation.getArgument(0);
    }

    private static Karyawan karyawanAktif() {
        Karyawan karyawan = new Karyawan();
        karyawan.setId(ID);
        karyawan.setNama("Budi");
        karyawan.setAlamat("Jakarta");
        karyawan.setDob(DOB);
        karyawan.setStatus("AKTIF");
        karyawan.setCreatedDate(Instant.parse("2024-01-01T00:00:00Z"));
        karyawan.setUpdatedDate(Instant.parse("2024-01-01T00:00:00Z"));
        return karyawan;
    }

    private static DetailKaryawan detailAktif() {
        DetailKaryawan detail = new DetailKaryawan();
        detail.setId(1);
        detail.setNik("1234567890123456");
        detail.setNpwp("123456789012345");
        detail.setCreatedDate(Instant.parse("2024-01-01T00:00:00Z"));
        detail.setUpdatedDate(Instant.parse("2024-01-01T00:00:00Z"));
        return detail;
    }

    private static DetailKaryawan detailTerhapus() {
        DetailKaryawan detail = detailAktif();
        detail.setDeletedDate(Instant.parse("2024-06-01T00:00:00Z"));
        return detail;
    }
}
