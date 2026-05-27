package com.projeto.muttley.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificadoInfoResponseDTO {

    private String nomeEvento;
    private String nomeParticipante;
    private String emailParticipante;
}
