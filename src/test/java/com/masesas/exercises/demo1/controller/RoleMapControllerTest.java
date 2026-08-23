package com.masesas.exercises.demo1.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RoleMapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("daftar seluruh endpoint bisa diambil tanpa token")
    void semua_tanpaToken() throws Exception {
        mockMvc.perform(get("/api/rolemap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.path=='/api/karyawan/all')]").isNotEmpty());
    }

    @Test
    @DisplayName("endpoint dengan @PreAuthorize class-level memakai peran method-level")
    void semua_methodLevelMenimpaClassLevel() throws Exception {
        mockMvc.perform(get("/api/rolemap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.path=='/api/karyawan' && @.method=='POST')].roles[0]")
                        .value("ADMIN"))
                .andExpect(jsonPath("$.data[?(@.path=='/api/karyawan' && @.method=='POST')].roles[1]")
                        .value("MANAGER"));
    }

    @Test
    @DisplayName("tidak ada aturan yang bergantung data request, jadi seluruh peta pasti")
    void semua_tidakAdaEkspresiKondisional() throws Exception {
        mockMvc.perform(get("/api/rolemap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.conditional==true)]").isEmpty());
    }

    @Test
    @DisplayName("GUEST hanya mendapat endpoint publik")
    void perPeran_guestHanyaPublik() throws Exception {
        mockMvc.perform(get("/api/rolemap/GUEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.peran").value("GUEST"))
                .andExpect(jsonPath("$.data.endpoint[?(@.public==false)]").isEmpty())
                .andExpect(jsonPath("$.data.endpoint[?(@.path=='/api/auth/karyawan/login')]").isNotEmpty());
    }

    @Test
    @DisplayName("CUSTOMER mendapat endpoint customer tapi tidak endpoint karyawan")
    void perPeran_customer() throws Exception {
        mockMvc.perform(get("/api/rolemap/customer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.peran").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.endpoint[?(@.path=='/api/customer/me')]").isNotEmpty())
                .andExpect(jsonPath("$.data.endpoint[?(@.path=='/api/karyawan/all')]").isEmpty());
    }

    @Test
    @DisplayName("matriks memuat peran dari tabel role, SUPERADMIN, CUSTOMER, dan GUEST")
    void matriks_memuatSeluruhSumberPeran() throws Exception {
        mockMvc.perform(get("/api/rolemap/matriks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].peran").value(hasItems(
                        "SUPERADMIN", "ADMIN", "MANAGER", "MARKETING", "SALES", "HR", "KARYAWAN",
                        "CUSTOMER", "GUEST")));
    }

    @Test
    @DisplayName("matriks memberi hasil yang sama dengan pencarian satu peran")
    void matriks_konsistenDenganPerPeran() throws Exception {
        mockMvc.perform(get("/api/rolemap/matriks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.peran=='GUEST')].endpoint[?(@.public==false)]").isEmpty())
                .andExpect(jsonPath("$.data[?(@.peran=='CUSTOMER')].endpoint[?(@.path=='/api/customer/me')]")
                        .isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.peran=='CUSTOMER')].endpoint[?(@.path=='/api/karyawan/all')]")
                        .isEmpty());
    }

    @Test
    @DisplayName("path matriks tidak tertangkap oleh pola satu peran")
    void matriks_tidakBentrokDenganPolaPeran() throws Exception {
        mockMvc.perform(get("/api/rolemap/matriks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("HR tidak mendapat endpoint yang dibatasi ADMIN dan MANAGER")
    void perPeran_hr() throws Exception {
        mockMvc.perform(get("/api/rolemap/HR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.endpoint[?(@.path=='/api/karyawan/all')]").isNotEmpty())
                .andExpect(jsonPath("$.data.endpoint[?(@.path=='/api/karyawan' && @.method=='POST')]").isEmpty());
    }
}
