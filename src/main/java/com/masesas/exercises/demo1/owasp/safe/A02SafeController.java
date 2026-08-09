package com.masesas.exercises.demo1.owasp.safe;

import com.masesas.exercises.demo1.dto.DetailKaryawanResponse;
import com.masesas.exercises.demo1.entity.Karyawan;
import com.masesas.exercises.demo1.exception.ResourceNotFoundException;
import com.masesas.exercises.demo1.repository.KaryawanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/safe")
@RequiredArgsConstructor
public class A02SafeController {

    private final KaryawanRepository karyawanRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/karyawan/{id}/detail")
    @PreAuthorize("hasRole('HR')")
    public DetailKaryawanResponse detail(@PathVariable Integer id) {
        Karyawan karyawan = karyawanRepository.findByIdAndDeletedDateIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Karyawan", id));
        return DetailKaryawanResponse.from(karyawan.getDetailKaryawan());
    }

    @GetMapping("/hash")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> hash(@RequestParam String password) {
        return Map.of("hash", passwordEncoder.encode(password));
    }
}
