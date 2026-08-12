package com.masesas.exercises.demo1.controller;

import com.masesas.exercises.demo1.dto.EndpointAksesResponse;
import com.masesas.exercises.demo1.dto.RoleAksesResponse;
import com.masesas.exercises.demo1.service.RoleMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rolemap")
@RequiredArgsConstructor
public class RoleMapController {

    private final RoleMapService roleMapService;

    @GetMapping
    public ResponseEntity<List<EndpointAksesResponse>> semua() {
        return ResponseEntity.ok(roleMapService.semua());
    }

    @GetMapping("/matriks")
    public ResponseEntity<List<RoleAksesResponse>> matriks() {
        return ResponseEntity.ok(roleMapService.semuaPeran());
    }

    @GetMapping("/{peran}")
    public ResponseEntity<RoleAksesResponse> perPeran(@PathVariable String peran) {
        return ResponseEntity.ok(roleMapService.untukPeran(peran));
    }
}
