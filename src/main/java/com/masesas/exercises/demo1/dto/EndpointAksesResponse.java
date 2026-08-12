package com.masesas.exercises.demo1.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class EndpointAksesResponse {

    private final String method;
    private final String path;
    private final String handler;
    private final boolean isPublic;
    private final List<String> roles;
    private final boolean conditional;
    private final String expressions;
}
