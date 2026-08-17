package com.masesas.exercises.demo1.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Hasil sp_proses_karyawan: data karyawan beserta rekening dan pelatihannya")
public class HasilProsesKaryawan {

    @Schema(description = "Data karyawan setelah diproses")
    private KaryawanLengkap karyawan;
    @Schema(description = "Seluruh rekening milik karyawan tersebut")
    private List<RekeningRingkas> daftarRekening;
    @Schema(description = "Seluruh pelatihan yang pernah diikuti")
    private List<TrainingRingkas> daftarTraining;
}
