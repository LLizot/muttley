package com.projeto.muttley.controller;

import com.projeto.muttley.dto.ApiResponse;
import com.projeto.muttley.dto.CertificadoInfoResponseDTO;
import com.projeto.muttley.service.CertificadoService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/certificates")
public class CertificadoController {

    private final CertificadoService certificadoService;

    public CertificadoController(CertificadoService certificadoService) {
        this.certificadoService = certificadoService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CertificadoInfoResponseDTO>> getCertificateInfo(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        CertificadoInfoResponseDTO response = certificadoService.getCertificateInfo(id);
        ApiResponse<CertificadoInfoResponseDTO> body = ApiResponse.success(
                HttpStatus.OK.value(),
                "Certificado encontrado com sucesso",
                httpRequest.getRequestURI(),
                response);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getCertificatePdf(@PathVariable UUID id) {
        byte[] pdf = certificadoService.getCertificatePdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=certificado.pdf")
                .body(pdf);
    }
}
