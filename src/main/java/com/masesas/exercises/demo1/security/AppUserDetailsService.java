package com.masesas.exercises.demo1.security;

import com.masesas.exercises.demo1.entity.Karyawan;
import com.masesas.exercises.demo1.entity.KaryawanRole;
import com.masesas.exercises.demo1.repository.CustomerRepository;
import com.masesas.exercises.demo1.repository.KaryawanRepository;
import com.masesas.exercises.demo1.repository.KaryawanRoleRepository;
import com.masesas.exercises.demo1.repository.RolePermissionRepository;
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
    private final RolePermissionRepository rolePermissionRepository;


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
                        List.of(),
                        null,
                        AppUser.TIPE_CUSTOMER));
    }

    private AppUser toAppUser(Karyawan karyawan) {
        List<KaryawanRole> karyawanRoles = karyawanRoleRepository.findAllByKaryawan_Id(karyawan.getId());
        List<String> roles = karyawanRoles.stream()
                .map(karyawanRole -> karyawanRole.getRole().getNama())
                .toList();
        return new AppUser(
                karyawan.getEmail(),
                karyawan.getPassword(),
                roles,
                permissionDari(karyawanRoles),
                karyawan.getId(),
                AppUser.TIPE_KARYAWAN);
    }

    private List<String> permissionDari(List<KaryawanRole> karyawanRoles) {
        List<Integer> idRole = karyawanRoles.stream()
                .map(karyawanRole -> karyawanRole.getRole().getId())
                .toList();
        if (idRole.isEmpty()) {
            return List.of();
        }
        return rolePermissionRepository.findAllByRole_IdIn(idRole).stream()
                .map(rolePermission -> rolePermission.getPermission().getKode())
                .distinct()
                .toList();
    }
}
