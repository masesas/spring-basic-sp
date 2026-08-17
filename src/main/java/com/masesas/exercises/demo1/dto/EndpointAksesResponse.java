package com.masesas.exercises.demo1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "Satu endpoint beserta peran yang boleh mengaksesnya")
public class EndpointAksesResponse {

    @Schema(description = "Metode HTTP, ANY bila endpoint menerima semua metode", example = "GET")
    private final String method;
    @Schema(description = "Pola path endpoint", example = "/api/karyawan/{id}")
    private final String path;
    @Schema(description = "Kelas dan method yang menangani", example = "KaryawanController.findById")
    private final String handler;
    private final boolean isPublic;
    @Schema(description = "Peran yang diizinkan, kosong bila tidak dibatasi peran")
    private final List<String> roles;
    @Schema(description = "True bila syarat aksesnya tidak hanya berupa pemeriksaan peran", example = "false")
    private final boolean conditional;
    @Schema(description = "Ekspresi @PreAuthorize apa adanya", example = "hasAnyRole('ADMIN','MANAGER')")
    private final String expressions;

    @Schema(description = "True bila endpoint bisa diakses tanpa token", example = "false")
    public boolean isPublic() {
        return isPublic;
    }
}
