package com.masesas.exercises.demo1.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " tidak ditemukan: id=" + id);
    }
}
