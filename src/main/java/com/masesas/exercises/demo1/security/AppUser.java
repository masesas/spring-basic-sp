package com.masesas.exercises.demo1.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public class AppUser implements UserDetails {

    public static final String TIPE_KARYAWAN = "KARYAWAN";
    public static final String TIPE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_GUEST = "ROLE_GUEST";
    public static final String PRINCIPAL_GUEST = "guest";

    private final String username;
    private final String password;
    private final List<String> roles;
    private final Integer idKaryawan;
    private final String tipe;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Stream.concat(roles.stream().map(role -> "ROLE_" + role), Stream.of(ROLE_GUEST))
                .map(SimpleGrantedAuthority::new)
                .toList();
    }
}
