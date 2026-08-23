package com.masesas.exercises.demo1.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

@Getter
@Setter
@AllArgsConstructor
public class AppUser implements UserDetails {

    public static final String TIPE_KARYAWAN = "KARYAWAN";
    public static final String TIPE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_SUPERADMIN = "SUPERADMIN";
    public static final String ROLE_GUEST = "ROLE_GUEST";
    public static final String PRINCIPAL_GUEST = "guest";

    private String username;
    private String password;
    private final List<String> roles;
    private final List<String> permissions;
    private final Integer idKaryawan;
    private final String tipe;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Stream.of(
                        roles.stream().map(role -> "ROLE_" + role),
                        permissions.stream(),
                        Stream.of(ROLE_GUEST))
                .flatMap(bagian -> bagian)
                .map(SimpleGrantedAuthority::new)
                .toList();
    }
}
