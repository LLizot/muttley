package com.projeto.muttley.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientResponseDTO {

    private UUID id;
    private String nome;
    private String email;
    private String cpf;
    private LocalDateTime dataCriacao;
    private Integer totalPontos;
    private Integer totalCertificados;
    private Integer totalMedalhas;
}
