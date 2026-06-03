package com.bragdev.frauddetection.common.config;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ResourceNotFoundException extends ResponseStatusException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String resource, String id) {
        super(HttpStatus.NOT_FOUND, String.format("%s with id %s not found", resource, id));
    }

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
