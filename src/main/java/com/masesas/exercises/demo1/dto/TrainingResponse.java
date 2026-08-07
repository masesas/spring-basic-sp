package com.masesas.exercises.demo1.dto;

import com.masesas.exercises.demo1.entity.Training;

import java.time.Instant;

public record TrainingResponse(
        Integer id,
        String tema,
        String pengajar,
        Instant createdDate,
        Instant updatedDate) {

    public static TrainingResponse from(Training training) {
        return new TrainingResponse(
                training.getId(),
                training.getTema(),
                training.getPengajar(),
                training.getAudit().getCreatedDate(),
                training.getAudit().getUpdatedDate());
    }
}
