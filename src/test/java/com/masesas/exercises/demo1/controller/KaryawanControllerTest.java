package com.masesas.exercises.demo1.controller;

import com.masesas.exercises.demo1.dto.*;
import com.masesas.exercises.demo1.exception.DuplicateResourceException;
import com.masesas.exercises.demo1.exception.InvalidRequestException;
import com.masesas.exercises.demo1.exception.ResourceNotFoundException;
import com.masesas.exercises.demo1.service.KaryawanService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ini adalah integration test spring boot")
class KaryawanControllerTest {

    private static final int ID = 7;
    private static final int ID_TIDAK_ADA = 404;
    private static final String BASE = "/api/karyawan";
    private static final LocalDate DOB = LocalDate.of(1990, 1, 1);

    private static final String BODY_VALID = """
            {"nama":"Budi","alamat":"Jakarta","dob":"1990-01-01","status":"AKTIF"}
            """;
    private static final String BODY_DETAIL_VALID = """
            {"nik":"1234567890123456","npwp":"123456789012345"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KaryawanService karyawanService;

    @Nested
    @DisplayName("POST /api/karyawan")
    class Create {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("ADMIN dengan body valid membalas 201 dan data yang tersimpan")
        void create_adminBodyValid_membalas201() throws Exception {
            when(karyawanService.create(any(CreateKaryawanRequest.class))).thenReturn(responseKaryawan());

            mockMvc.perform(post("/api/karyawan").contentType(MediaType.APPLICATION_JSON).content(BODY_VALID))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(ID))
                    .andExpect(jsonPath("$.nama").value("Budi"))
                    .andExpect(jsonPath("$.status").value("AKTIF"));

            verify(karyawanService).create(any(CreateKaryawanRequest.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("body diikat ke DTO apa adanya sebelum diteruskan ke service")
        void create_bodyValid_mengikatSeluruhField() throws Exception {
            when(karyawanService.create(any(CreateKaryawanRequest.class))).thenReturn(responseKaryawan());

            mockMvc.perform(post("/api/karyawan").contentType(MediaType.APPLICATION_JSON).content("""
                            {"nama":"Budi","alamat":"Jakarta","dob":"1990-01-01","status":"AKTIF",
                             "detail":{"nik":"1234567890123456","npwp":"123456789012345"}}
                            """))
                    .andExpect(status().isCreated());

            var terkirim = org.mockito.ArgumentCaptor.forClass(CreateKaryawanRequest.class);
            verify(karyawanService).create(terkirim.capture());
            assertThat(terkirim.getValue().getNama()).isEqualTo("Budi");
            assertThat(terkirim.getValue().getDob()).isEqualTo(DOB);
            assertThat(terkirim.getValue().getDetail().getNik()).isEqualTo("1234567890123456");
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("nama kosong ditolak Bean Validation dengan 400 sebelum masuk service")
        void create_namaKosong_membalas400() throws Exception {
            mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
                            {"nama":"","alamat":"Jakarta","dob":"1990-01-01","status":"AKTIF"}
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("nama")));

            verifyNoInteractions(karyawanService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("status di luar AKTIF/NONAKTIF ditolak dengan 400")
        void create_statusTidakDikenal_membalas400() throws Exception {
            mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
                            {"nama":"Budi","alamat":"Jakarta","dob":"1990-01-01","status":"CUTI"}
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("status")));

            verifyNoInteractions(karyawanService);
        }

        @Test
        @WithMockUser(roles = "HR")
        @DisplayName("HR tidak berwenang membuat karyawan dan dibalas 403")
        void create_peranTidakBerwenang_membalas403() throws Exception {
            mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(BODY_VALID))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(karyawanService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("NIK duplikat dari service dipetakan menjadi 409")
        void create_nikDuplikat_membalas409() throws Exception {
            when(karyawanService.create(any(CreateKaryawanRequest.class)))
                    .thenThrow(new DuplicateResourceException("NIK sudah terdaftar pada karyawan lain"));

            mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(BODY_VALID))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("NIK sudah terdaftar pada karyawan lain"));
        }
    }

    @Nested
    @DisplayName("GET /api/karyawan")
    class Baca {

        @Test
        @WithMockUser(roles = "KARYAWAN")
        @DisplayName("findById membalas 200 beserta detail yang sudah disamarkan")
        void findById_idAda_membalas200() throws Exception {
            when(karyawanService.findById(ID)).thenReturn(responseKaryawanDenganDetail());

            mockMvc.perform(get(BASE + "/" + ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ID))
                    .andExpect(jsonPath("$.detail.nik").value("************3456"));
        }


        @Test
        @WithMockUser(roles = "KARYAWAN")
        @DisplayName("findById pada id yang tidak ada membalas 404")
        void findById_idTidakAda_membalas404() throws Exception {
            when(karyawanService.findById(ID_TIDAK_ADA))
                    .thenThrow(new ResourceNotFoundException("Karyawan", ID_TIDAK_ADA));

            mockMvc.perform(get(BASE + "/" + ID_TIDAK_ADA))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @WithMockUser(roles = "KARYAWAN")
        @DisplayName("id bukan angka membalas 400, bukan 500")
        void findById_idBukanAngka_membalas400() throws Exception {
            mockMvc.perform(get(BASE + "/bukan-angka"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(karyawanService);
        }

        @Test
        @WithMockUser(roles = "KARYAWAN")
        @DisplayName("daftar berpaginasi meneruskan page dan size dari query string")
        void findAll_denganQueryPageable_meneruskanKeService() throws Exception {
            Pageable diminta = PageRequest.of(1, 5);
            when(karyawanService.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(responseKaryawan()), diminta, 6));

            mockMvc.perform(get(BASE).param("page", "1").param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].nama").value("Budi"));

            var pageable = org.mockito.ArgumentCaptor.forClass(Pageable.class);
            verify(karyawanService).findAll(pageable.capture());
            assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
            assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("/page memakai nilai bawaan ketika query string kosong")
        void findPage_tanpaParam_memakaiNilaiBawaan() throws Exception {
            when(karyawanService.findPage(0, 10)).thenReturn(new PageImpl<>(List.of(responseKaryawan())));

            mockMvc.perform(get(BASE + "/page"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(ID));

            verify(karyawanService).findPage(0, 10);
        }

        @Test
        @WithMockUser(roles = "MANAGER")
        @DisplayName("/page hanya untuk ADMIN, MANAGER dibalas 403")
        void findPage_bukanAdmin_membalas403() throws Exception {
            mockMvc.perform(get(BASE + "/page"))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(karyawanService);
        }

        @Test
        @WithMockUser(roles = "SALES")
        @DisplayName("/search meneruskan nama dan pagination ke service")
        void findPageByNama_adaParamNama_membalas200() throws Exception {
            when(karyawanService.findPageByNama("bud", 0, 10))
                    .thenReturn(new PageImpl<>(List.of(responseKaryawan())));

            mockMvc.perform(get(BASE + "/search").param("nama", "bud"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].nama").value("Budi"));

            verify(karyawanService).findPageByNama("bud", 0, 10);
        }

        @Test
        @WithMockUser(roles = "SALES")
        @DisplayName("/search tanpa parameter nama membalas 400")
        void findPageByNama_tanpaParamNama_membalas400() throws Exception {
            mockMvc.perform(get(BASE + "/search"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(karyawanService);
        }

        @Test
        @WithMockUser(roles = "MARKETING")
        @DisplayName("/all membalas daftar tanpa pagination")
        void findAllWithoutPaging_membalasDaftar() throws Exception {
            when(karyawanService.findAll()).thenReturn(List.of(responseKaryawan()));

            mockMvc.perform(get(BASE + "/all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].nama").value("Budi"));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("tanpa autentikasi tidak ada satu pun endpoint karyawan yang bisa dibaca")
        void findAll_tanpaAutentikasi_ditolak() throws Exception {
            mockMvc.perform(get(BASE + "/all"))
                    .andExpect(status().is4xxClientError());

            verifyNoInteractions(karyawanService);
        }
    }

    @Nested
    @DisplayName("PUT dan DELETE /api/karyawan/{id}")
    class Ubah {

        @Test
        @WithMockUser(roles = "MANAGER")
        @DisplayName("MANAGER boleh mengubah karyawan dan dibalas 200")
        void update_managerBodyValid_membalas200() throws Exception {
            when(karyawanService.update(eq(ID), any(UpdateKaryawanRequest.class))).thenReturn(responseKaryawan());

            mockMvc.perform(put(BASE + "/" + ID).contentType(MediaType.APPLICATION_JSON).content(BODY_VALID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ID));

            verify(karyawanService).update(eq(ID), any(UpdateKaryawanRequest.class));
        }

        @Test
        @WithMockUser(roles = "MARKETING")
        @DisplayName("MARKETING hanya boleh membaca, PUT dibalas 403")
        void update_peranTidakBerwenang_membalas403() throws Exception {
            mockMvc.perform(put(BASE + "/" + ID).contentType(MediaType.APPLICATION_JSON).content(BODY_VALID))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(karyawanService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("nama melebihi 100 karakter ditolak dengan 400")
        void update_namaTerlaluPanjang_membalas400() throws Exception {
            String namaPanjang = "B".repeat(101);

            mockMvc.perform(put(BASE + "/" + ID).contentType(MediaType.APPLICATION_JSON).content(
                            "{\"nama\":\"" + namaPanjang + "\",\"status\":\"AKTIF\"}"))
                    .andExpect(status().isBadRequest());

            verify(karyawanService, never()).update(anyInt(), any(UpdateKaryawanRequest.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("validasi service yang gagal dipetakan menjadi 400")
        void update_dobMasaDepan_membalas400() throws Exception {
            when(karyawanService.update(eq(ID), any(UpdateKaryawanRequest.class)))
                    .thenThrow(new InvalidRequestException("dob tidak boleh melebihi tanggal hari ini"));

            mockMvc.perform(put(BASE + "/" + ID).contentType(MediaType.APPLICATION_JSON).content(BODY_VALID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("dob tidak boleh melebihi tanggal hari ini"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("DELETE oleh ADMIN membalas 204 tanpa body")
        void delete_admin_membalas204() throws Exception {
            doNothing().when(karyawanService).delete(ID);

            mockMvc.perform(delete(BASE + "/" + ID))
                    .andExpect(status().isNoContent());

            verify(karyawanService).delete(ID);
        }

        @Test
        @WithMockUser(roles = "MANAGER")
        @DisplayName("DELETE hanya untuk ADMIN, MANAGER dibalas 403")
        void delete_bukanAdmin_membalas403() throws Exception {
            mockMvc.perform(delete(BASE + "/" + ID))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(karyawanService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("DELETE pada id yang tidak ada membalas 404")
        void delete_idTidakAda_membalas404() throws Exception {
            doThrow(new ResourceNotFoundException("Karyawan", ID_TIDAK_ADA))
                    .when(karyawanService).delete(ID_TIDAK_ADA);

            mockMvc.perform(delete(BASE + "/" + ID_TIDAK_ADA))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("/api/karyawan/{id}/detail")
    class Detail {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("upsert detail dengan body valid membalas 200")
        void upsertDetail_bodyValid_membalas200() throws Exception {
            when(karyawanService.upsertDetail(eq(ID), any(DetailKaryawanRequest.class)))
                    .thenReturn(responseKaryawanDenganDetail());

            mockMvc.perform(put(BASE + "/" + ID + "/detail")
                            .contentType(MediaType.APPLICATION_JSON).content(BODY_DETAIL_VALID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.detail.npwp").value("***********2345"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("NIK yang bukan 16 digit ditolak dengan 400")
        void upsertDetail_nikBukan16Digit_membalas400() throws Exception {
            mockMvc.perform(put(BASE + "/" + ID + "/detail")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nik\":\"123\",\"npwp\":\"123456789012345\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("nik")));

            verifyNoInteractions(karyawanService);
        }

        @Test
        @WithMockUser(roles = "HR")
        @DisplayName("HR tidak boleh mengubah detail dan dibalas 403")
        void upsertDetail_peranTidakBerwenang_membalas403() throws Exception {
            mockMvc.perform(put(BASE + "/" + ID + "/detail")
                            .contentType(MediaType.APPLICATION_JSON).content(BODY_DETAIL_VALID))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(karyawanService);
        }

        @Test
        @WithMockUser(roles = "MANAGER")
        @DisplayName("hapus detail membalas 200 dengan detail sudah kosong")
        void removeDetail_membalas200() throws Exception {
            when(karyawanService.removeDetail(ID)).thenReturn(responseKaryawan());

            mockMvc.perform(delete(BASE + "/" + ID + "/detail"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.detail").doesNotExist());

            verify(karyawanService).removeDetail(ID);
        }

        @Test
        @WithMockUser(roles = "MANAGER")
        @DisplayName("hapus detail pada karyawan yang tidak ada membalas 404")
        void removeDetail_idTidakAda_membalas404() throws Exception {
            when(karyawanService.removeDetail(ID_TIDAK_ADA))
                    .thenThrow(new ResourceNotFoundException("Karyawan", ID_TIDAK_ADA));

            mockMvc.perform(delete(BASE + "/" + ID_TIDAK_ADA + "/detail"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/karyawan/{id}/avatar")
    class UploadAvatar {

        private MockMultipartFile berkas() {
            return new MockMultipartFile(
                    "file", "foto.png", MediaType.IMAGE_PNG_VALUE, "isi-berkas".getBytes(UTF_8));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("berkas diteruskan ke service dan avatar terisi di response")
        void uploadAvatar_berkasMultipart_membalasAvatar() throws Exception {
            KaryawanResponse response = responseKaryawan();
            response.setAvatar("karyawan/baru.png");
            when(karyawanService.uploadAvatar(eq(ID), any(MultipartFile.class))).thenReturn(response);

            mockMvc.perform(multipart(BASE + "/" + ID + "/avatar").file(berkas()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ID))
                    .andExpect(jsonPath("$.avatar").value("karyawan/baru.png"));

            verify(karyawanService).uploadAvatar(eq(ID), any(MultipartFile.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("karyawan tidak ada membalas 404")
        void uploadAvatar_karyawanTidakAda_membalas404() throws Exception {
            when(karyawanService.uploadAvatar(eq(ID_TIDAK_ADA), any(MultipartFile.class)))
                    .thenThrow(new ResourceNotFoundException("Karyawan", ID_TIDAK_ADA));

            mockMvc.perform(multipart(BASE + "/" + ID_TIDAK_ADA + "/avatar").file(berkas()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("gambar ditolak service membalas 400")
        void uploadAvatar_gambarTidakValid_membalas400() throws Exception {
            when(karyawanService.uploadAvatar(eq(ID), any(MultipartFile.class)))
                    .thenThrow(new InvalidRequestException("tipe gambar harus image/jpeg, image/png, atau image/webp"));

            mockMvc.perform(multipart(BASE + "/" + ID + "/avatar").file(berkas()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("upload tanpa autentikasi ditolak")
        void uploadAvatar_tanpaAutentikasi_ditolak() throws Exception {
            mockMvc.perform(multipart(BASE + "/" + ID + "/avatar").file(berkas()))
                    .andExpect(status().is4xxClientError());

            verifyNoInteractions(karyawanService);
        }
    }

    private static KaryawanResponse responseKaryawan() {
        return new KaryawanResponse(
                ID, "Budi", "Jakarta", DOB, "AKTIF", null, null,
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-01T00:00:00Z"));
    }

    private static KaryawanResponse responseKaryawanDenganDetail() {
        KaryawanResponse response = responseKaryawan();
        response.setDetail(new DetailKaryawanResponse(
                1, "************3456", "***********2345",
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-01T00:00:00Z")));
        return response;
    }
}
