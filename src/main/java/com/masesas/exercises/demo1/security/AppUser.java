package com.masesas.exercises.demo1.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record AppUser(String username, String password, List<String> roles, Integer idKaryawan, String tipe)
        implements UserDetails {

    public static final String TIPE_KARYAWAN = "KARYAWAN";
    public static final String TIPE_CUSTOMER = "CUSTOMER";

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public Integer getIdKaryawan() {
        return idKaryawan;
    }
}
