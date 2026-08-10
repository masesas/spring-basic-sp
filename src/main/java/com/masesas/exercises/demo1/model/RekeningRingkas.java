package com.masesas.exercises.demo1.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RekeningRingkas {

    private Integer id;
    private String nama;
    private String jenis;
    private String rekening;

    public static RekeningRingkas fromRow(Object[] row) {
        return new RekeningRingkas(
                ((Number) row[0]).intValue(),
                (String) row[1],
                (String) row[2],
                (String) row[3]);
    }
}
