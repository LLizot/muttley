package com.projeto.muttley.controller;

import com.projeto.muttley.dto.ApiResponse;
import com.projeto.muttley.dto.EmailTestRequestDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    private final JavaMailSender mailSender;
    private final String mailFrom;

    public TestController(JavaMailSender mailSender, @Value("${app.mail.from}") String mailFrom) {
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
    }

    @GetMapping("/")
    public String hello() {
        return "API funcionando";
    }

    @PostMapping("/emails/test")
    public ResponseEntity<ApiResponse<Object>> sendTestEmail(
            @Valid @RequestBody EmailTestRequestDTO request,
            HttpServletRequest httpRequest) {
        sendEmail(request.getEmail());
        ApiResponse<Object> body = ApiResponse.success(
                HttpStatus.OK.value(),
                "Email de teste enviado",
                httpRequest.getRequestURI(),
                null);
        return ResponseEntity.ok(body);
    }

    private void sendEmail(String email) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false);
            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("Email de teste");
            helper.setText("Hello World", false);
            mailSender.send(message);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao enviar email de teste", ex);
        }
    }
}