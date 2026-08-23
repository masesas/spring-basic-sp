package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pemberian satu izin kepada satu peran")
public class RolePermissionRequest {

    @NotNull
    @Schema(description = "ID peran", example = "2")
    private Integer idRole;

    @NotNull
    @Schema(description = "ID izin", example = "13")
    private Integer idPermission;
}
