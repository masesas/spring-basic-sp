package com.masesas.exercises.demo1.controller;

import com.masesas.exercises.demo1.dto.BaseApiResponse;
import com.masesas.exercises.demo1.dto.CustomerResponse;
import com.masesas.exercises.demo1.security.AppUser;
import com.masesas.exercises.demo1.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
@Tag(name = "Customer", description = "Data milik customer yang sedang login.")
@SecurityRequirement(name = "customerAuth")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
            summary = "Profil customer yang sedang login",
            description = "Identitas diambil dari token, bukan dari parameter, "
                    + "sehingga satu customer tidak bisa membaca profil customer lain.")
    @ApiResponse(responseCode = "200", description = "Profil ditemukan")
    public BaseApiResponse<CustomerResponse> profil(@AuthenticationPrincipal AppUser user) {
        return BaseApiResponse.ok("Profil ditemukan", customerService.profil(user.getUsername()));
    }

}
