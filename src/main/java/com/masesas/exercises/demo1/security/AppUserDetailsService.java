package com.masesas.exercises.demo1.security;

import com.masesas.exercises.demo1.entity.Karyawan;
import com.masesas.exercises.demo1.repository.CustomerRepository;
import com.masesas.exercises.demo1.repository.KaryawanRepository;
import com.masesas.exercises.demo1.repository.KaryawanRoleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private static final String ROLE_CUSTOMER = "CUSTOMER";

    private final KaryawanRepository karyawanRepository;
    private final KaryawanRoleRepository karyawanRoleRepository;
    private final CustomerRepository customerRepository;
    private final Map<String, AppUser> demoUsers;
    private final String rawPassword;

    public AppUserDetailsService(
            KaryawanRepository karyawanRepository,
            KaryawanRoleRepository karyawanRoleRepository,
            CustomerRepository customerRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.demo.password}") String rawPassword) {
        this.karyawanRepository = karyawanRepository;
        this.karyawanRoleRepository = karyawanRoleRepository;
        this.customerRepository = customerRepository;
        this.rawPassword = rawPassword;
        String encoded = passwordEncoder.encode(rawPassword);

        Map<String, AppUser> registry = new LinkedHashMap<>();
        registry.put("admin", new AppUser("admin", encoded, List.of("ADMIN"), null, AppUser.TIPE_KARYAWAN));
        registry.put("hr", new AppUser("hr", encoded, List.of("HR"), null, AppUser.TIPE_KARYAWAN));
        registry.put("karyawan", new AppUser("karyawan", encoded, List.of("KARYAWAN"), 1, AppUser.TIPE_KARYAWAN));
        this.demoUsers = Map.copyOf(registry);
    }

    @Override
    public AppUser loadUserByUsername(String username) {
        return find(username).orElseThrow(
                () -> new UsernameNotFoundException("Pengguna tidak dikenal: " + username));
    }

    public Optional<AppUser> find(String username) {
        return findKaryawan(username)
                .or(() -> findCustomer(username))
                .or(() -> Optional.ofNullable(demoUsers.get(username)));
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

    public String rawPassword() {
        return rawPassword;
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
