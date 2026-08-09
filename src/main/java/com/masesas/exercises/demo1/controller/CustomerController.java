package com.masesas.exercises.demo1.controller;

import com.masesas.exercises.demo1.dto.CustomerResponse;
import com.masesas.exercises.demo1.security.AppUser;
import com.masesas.exercises.demo1.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public CustomerResponse profil(@AuthenticationPrincipal AppUser user) {
        return customerService.profil(user.getUsername());
    }
}
