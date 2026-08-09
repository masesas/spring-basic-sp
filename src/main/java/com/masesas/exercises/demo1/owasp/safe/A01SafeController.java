package com.masesas.exercises.demo1.owasp.safe;

import com.masesas.exercises.demo1.dto.PayrollResponse;
import com.masesas.exercises.demo1.service.KaryawanService;
import com.masesas.exercises.demo1.service.PayrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/safe")
@RequiredArgsConstructor
public class A01SafeController {

    private final PayrollService payrollService;
    private final KaryawanService karyawanService;

    @GetMapping("/payroll/{idKaryawan}")
    @PreAuthorize("hasRole('HR') or #idKaryawan == authentication.principal.idKaryawan")
    public List<PayrollResponse> riwayatGaji(@PathVariable Integer idKaryawan) {
        return payrollService.findRiwayatKaryawan(idKaryawan);
    }

    @DeleteMapping("/karyawan/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void hapusKaryawan(@PathVariable Integer id) {
        karyawanService.delete(id);
    }
}
