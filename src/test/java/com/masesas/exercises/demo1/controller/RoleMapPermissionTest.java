package com.masesas.exercises.demo1.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RoleMapPermissionTest {

    private static final String APPROVE = "/api/loan-application/{id}/approve";
    private static final String DISBURSE = "/api/loan-application/{id}/disburse";
    private static final String DAFTAR_PENGAJUAN = "/api/loan-application";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("endpoint berpenjaga hasAuthority terbaca sebagai permission, bukan sebagai kondisional")
    void endpointPermissionTerbacaUtuh() throws Exception {
        mockMvc.perform(get("/api/rolemap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.path=='" + APPROVE + "')].permissions[0]")
                        .value("LOAN_APPLICATION_APPROVE"))
                .andExpect(jsonPath("$.data[?(@.path=='" + APPROVE + "')].conditional")
                        .value(false))
                .andExpect(jsonPath("$.data[?(@.path=='" + APPROVE + "')].roles[0]")
                        .doesNotExist());
    }

    @Test
    @DisplayName("MANAGER memegang approve tetapi tidak memegang disburse")
    void managerHanyaSampaiApprove() throws Exception {
        mockMvc.perform(get("/api/rolemap/MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.endpoint[?(@.path=='" + APPROVE + "')]").isNotEmpty())
                .andExpect(jsonPath("$.data.endpoint[?(@.path=='" + DISBURSE + "')]").isEmpty());
    }

    @Test
    @DisplayName("MARKETING hanya membaca pengajuan, tidak ikut satu pun transisi status")
    void marketingHanyaMembaca() throws Exception {
        mockMvc.perform(get("/api/rolemap/MARKETING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.endpoint[?(@.path=='" + DAFTAR_PENGAJUAN + "')]").isNotEmpty())
                .andExpect(jsonPath("$.data.endpoint[?(@.path=='" + APPROVE + "')]").isEmpty())
                .andExpect(jsonPath("$.data.endpoint[?(@.path=='" + DISBURSE + "')]").isEmpty());
    }

    @Test
    @DisplayName("HR tidak memegang permission pinjaman sama sekali")
    void hrTanpaAksesPinjaman() throws Exception {
        mockMvc.perform(get("/api/rolemap/HR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.endpoint[?(@.path=='" + DAFTAR_PENGAJUAN + "')]").isEmpty());
    }

    @Test
    @DisplayName("ADMIN memegang seluruh transisi status pengajuan")
    void adminMemegangSeluruhTransisi() throws Exception {
        mockMvc.perform(get("/api/rolemap/ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.endpoint[?(@.path=='" + APPROVE + "')]").isNotEmpty())
                .andExpect(jsonPath("$.data.endpoint[?(@.path=='" + DISBURSE + "')]").isNotEmpty());
    }
}
