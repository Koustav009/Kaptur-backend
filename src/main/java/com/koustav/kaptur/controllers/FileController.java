package com.koustav.kaptur.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.koustav.kaptur.services.FileServices;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileServices fileServices;

    public ResponseEntity<?> uploadFiles() {
        return ResponseEntity.ok(null);
    }

}
