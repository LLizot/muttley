package com.projeto.muttley.service;

import com.projeto.muttley.dto.LoginRequestDTO;
import com.projeto.muttley.dto.LoginResponseDTO;
import com.projeto.muttley.exception.UnauthorizedException;
import com.projeto.muttley.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final String encodedAdminPassword;

    public AuthService(JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.encodedAdminPassword = passwordEncoder.encode(ADMIN_PASSWORD);
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        if (!ADMIN_USERNAME.equals(request.getUsername())) {
            throw new UnauthorizedException("Credenciais invalidas");
        }

        if (!passwordEncoder.matches(request.getPassword(), encodedAdminPassword)) {
            throw new UnauthorizedException("Credenciais invalidas");
        }

        String token = jwtService.generateToken(ADMIN_USERNAME);
        return LoginResponseDTO.builder().token(token).build();
    }
}
