package com.masesas.exercises.demo1.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainingRingkas {

    private Integer id;
    private String tema;
    private String pengajar;
    private LocalDate tanggal;

    public static TrainingRingkas fromRow(Object[] row) {
        return new TrainingRingkas(
                ((Number) row[0]).intValue(),
                (String) row[1],
                (String) row[2],
                SpRowUtils.toLocalDate(row[3]));
    }
}
