package com.masesas.exercises.demo1.dto;

import com.masesas.exercises.demo1.entity.Training;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainingResponse {

    private Integer id;
    private String tema;
    private String pengajar;
    private Instant createdDate;
    private Instant updatedDate;

    public static TrainingResponse from(Training training) {
        return new TrainingResponse(
                training.getId(),
                training.getTema(),
                training.getPengajar(),
                training.getAudit().getCreatedDate(),
                training.getAudit().getUpdatedDate());
    }
}
