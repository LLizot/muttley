package com.projeto.muttley.controller;

import com.projeto.muttley.dto.ApiResponse;
import com.projeto.muttley.dto.LoginRequestDTO;
import com.projeto.muttley.dto.LoginResponseDTO;
import com.projeto.muttley.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO request,
            HttpServletRequest httpRequest) {
        LoginResponseDTO response = authService.login(request);
        ApiResponse<LoginResponseDTO> body = ApiResponse.success(
                HttpStatus.OK.value(),
                "Login realizado com sucesso",
                httpRequest.getRequestURI(),
                response);
        return ResponseEntity.ok(body);
    }
}
