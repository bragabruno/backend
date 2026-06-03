package com.bragdev.frauddetection.security.controller;

import com.bragdev.frauddetection.common.dto.CreateUserRequest;
import com.bragdev.frauddetection.common.dto.UserDto;
import com.bragdev.frauddetection.security.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public UserDto getUser(@PathVariable UUID id) {
        return userService.getUser(id);
    }

    @PostMapping
    @Operation(summary = "Create a new user")
    public UserDto createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }
}
