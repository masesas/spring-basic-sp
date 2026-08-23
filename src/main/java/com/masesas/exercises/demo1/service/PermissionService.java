package com.masesas.exercises.demo1.service;

import com.masesas.exercises.demo1.dto.PermissionRequest;
import com.masesas.exercises.demo1.dto.PermissionResponse;
import com.masesas.exercises.demo1.entity.Permission;
import com.masesas.exercises.demo1.exception.BusinessRuleException;
import com.masesas.exercises.demo1.exception.DuplicateResourceException;
import com.masesas.exercises.demo1.exception.ResourceNotFoundException;
import com.masesas.exercises.demo1.repository.PermissionRepository;
import com.masesas.exercises.demo1.repository.RolePermissionRepository;
import com.masesas.exercises.demo1.service.support.Validators;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionService {

    private static final String RESOURCE = "Permission";

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final Clock clock;

    @Transactional
    public PermissionResponse create(PermissionRequest request) {
        Validators.requireNotNull(request, "data permission");
        String kode = kodeDari(request);

        if (permissionRepository.existsByKodeIgnoreCaseAndDeletedDateIsNull(kode)) {
            throw new DuplicateResourceException("Permission dengan kode tersebut sudah terdaftar");
        }

        Instant sekarang = Instant.now(clock);
        Permission permission = new Permission();
        permission.setKode(kode);
        permission.setDeskripsi(Validators.trimOrNull(request.getDeskripsi()));
        permission.setCreatedDate(sekarang);
        permission.setUpdatedDate(sekarang);

        return PermissionResponse.from(permissionRepository.save(permission));
    }

    @Transactional
    public PermissionResponse update(Integer id, PermissionRequest request) {
        Validators.requireNotNull(request, "data permission");
        Permission permission = requireActive(id);
        String kode = kodeDari(request);

        if (permissionRepository.existsByKodeIgnoreCaseAndDeletedDateIsNullAndIdNot(kode, id)) {
            throw new DuplicateResourceException("Permission dengan kode tersebut sudah terdaftar");
        }

        permission.setKode(kode);
        permission.setDeskripsi(Validators.trimOrNull(request.getDeskripsi()));
        permission.setUpdatedDate(Instant.now(clock));

        return PermissionResponse.from(permissionRepository.save(permission));
    }

    public PermissionResponse findById(Integer id) {
        return PermissionResponse.from(requireActive(id));
    }

    public Page<PermissionResponse> findAll(Pageable pageable) {
        return permissionRepository.findAllByDeletedDateIsNull(pageable).map(PermissionResponse::from);
    }

    @Transactional
    public void delete(Integer id) {
        Permission permission = requireActive(id);
        if (rolePermissionRepository.existsByPermission_Id(id)) {
            throw new BusinessRuleException(
                    "Permission masih dipegang peran, cabut dulu lewat /api/role-permission");
        }
        permission.setDeletedDate(Instant.now(clock));
        permission.setUpdatedDate(permission.getDeletedDate());
        permissionRepository.save(permission);
    }

    Permission requireActive(Integer id) {
        Validators.requireNotNull(id, "id permission");
        return permissionRepository.findByIdAndDeletedDateIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }

    private String kodeDari(PermissionRequest request) {
        return Validators.requireText(request.getKode(), "kode").toUpperCase(Locale.ROOT);
    }
}
