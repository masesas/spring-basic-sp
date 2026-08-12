package com.masesas.exercises.demo1.security;

import com.masesas.exercises.demo1.entity.Karyawan;
import com.masesas.exercises.demo1.repository.CustomerRepository;
import com.masesas.exercises.demo1.repository.KaryawanRepository;
import com.masesas.exercises.demo1.repository.KaryawanRoleRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    public static final String ROLE_CUSTOMER = "CUSTOMER";

    private final KaryawanRepository karyawanRepository;
    private final KaryawanRoleRepository karyawanRoleRepository;
    private final CustomerRepository customerRepository;


    @NonNull
    @Override
    public AppUser loadUserByUsername(@NonNull String username) {
        return find(username).orElseThrow(
                () -> new UsernameNotFoundException("Pengguna tidak dikenal: " + username));
    }

    public Optional<AppUser> find(String username) {
        return findKaryawan(username).or(() -> findCustomer(username));
    }

    public Optional<AppUser> findKaryawan(String email) {
        return karyawanRepository.findByEmailAndDeletedDateIsNull(email)
                .filter(karyawan -> karyawan.getPassword() != null)
                .map(this::toAppUser);
    }

    public Optional<AppUser> findCustomer(String email) {
        return customerRepository.findByEmailAndDeletedDateIsNull(email)
                .map(customer -> new AppUser(
                        customer.getEmail(),
                        customer.getPassword(),
                        List.of(ROLE_CUSTOMER),
                        null,
                        AppUser.TIPE_CUSTOMER));
    }

    private AppUser toAppUser(Karyawan karyawan) {
        List<String> roles = karyawanRoleRepository.findAllByKaryawan_Id(karyawan.getId()).stream()
                .map(karyawanRole -> karyawanRole.getRole().getNama())
                .toList();
        return new AppUser(
                karyawan.getEmail(),
                karyawan.getPassword(),
                roles,
                karyawan.getId(),
                AppUser.TIPE_KARYAWAN);
    }
}
