package com.bitirme.demo_bitirme.controller;

import com.bitirme.demo_bitirme.data.dto.UserDTO;
import com.bitirme.demo_bitirme.service.UserService;
import com.bitirme.demo_bitirme.util.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** Create a new test user — no auth. POST /users body: { "username": "...", "displayName": "..." } */
    @PostMapping
    public ResponseEntity<ApiResponse<UserDTO>> createUser(@RequestBody Map<String, String> body) {
        UserDTO created = userService.createUser(body.get("username"), body.get("displayName"));
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("User created", created)
        );
    }

    /** List all users — used by the UI's user switcher. GET /users */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserDTO>>> listUsers() {
        return ResponseEntity.ok(ApiResponse.success("Users", userService.listUsers()));
    }
}
