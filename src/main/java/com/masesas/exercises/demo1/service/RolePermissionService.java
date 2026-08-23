package com.masesas.exercises.demo1.service;

import com.masesas.exercises.demo1.dto.RolePermissionRequest;
import com.masesas.exercises.demo1.dto.RolePermissionResponse;
import com.masesas.exercises.demo1.entity.Permission;
import com.masesas.exercises.demo1.entity.Role;
import com.masesas.exercises.demo1.entity.RolePermission;
import com.masesas.exercises.demo1.exception.DuplicateResourceException;
import com.masesas.exercises.demo1.exception.ResourceNotFoundException;
import com.masesas.exercises.demo1.repository.RoleRepository;
import com.masesas.exercises.demo1.repository.RolePermissionRepository;
import com.masesas.exercises.demo1.service.support.Validators;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RolePermissionService {

    private final RolePermissionRepository rolePermissionRepository;
    private final RoleRepository roleRepository;
    private final PermissionService permissionService;
    private final Clock clock;

    public List<RolePermissionResponse> findAll() {
        return rolePermissionRepository.findAllBy().stream()
                .map(RolePermissionResponse::from)
                .toList();
    }

    public List<RolePermissionResponse> findByRole(Integer idRole) {
        requireRole(idRole);
        return rolePermissionRepository.findAllByRole_Id(idRole).stream()
                .map(RolePermissionResponse::from)
                .toList();
    }

    @Transactional
    public RolePermissionResponse grant(RolePermissionRequest request) {
        Validators.requireNotNull(request, "data role permission");
        Role role = requireRole(request.getIdRole());
        Permission permission = permissionService.requireActive(request.getIdPermission());

        rolePermissionRepository.findByRole_IdAndPermission_Id(role.getId(), permission.getId())
                .ifPresent(ada -> {
                    throw new DuplicateResourceException(
                            "Peran tersebut sudah memegang permission " + permission.getKode());
                });

        RolePermission rolePermission = new RolePermission();
        rolePermission.setRole(role);
        rolePermission.setPermission(permission);
        rolePermission.setCreatedDate(Instant.now(clock));

        return RolePermissionResponse.from(rolePermissionRepository.save(rolePermission));
    }

    @Transactional
    public void revoke(Integer idRole, Integer idPermission) {
        Validators.requireNotNull(idRole, "id role");
        Validators.requireNotNull(idPermission, "id permission");

        RolePermission rolePermission = rolePermissionRepository
                .findByRole_IdAndPermission_Id(idRole, idPermission)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RolePermission", "role=" + idRole + ", permission=" + idPermission));

        rolePermissionRepository.delete(rolePermission);
    }

    private Role requireRole(Integer idRole) {
        Validators.requireNotNull(idRole, "id role");
        return roleRepository.findById(idRole)
                .orElseThrow(() -> new ResourceNotFoundException("Role", idRole));
    }
}
