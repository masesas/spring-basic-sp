package com.masesas.exercises.demo1.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class RoleAksesResponse {

    private final String peran;
    private final int jumlah;
    private final List<EndpointAksesResponse> endpoint;
}
