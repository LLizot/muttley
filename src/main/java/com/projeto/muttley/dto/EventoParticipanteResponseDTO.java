package com.projeto.muttley.dto;

import com.projeto.muttley.entity.TipoParticipante;
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
public class EventoParticipanteResponseDTO {

    private UUID id;
    private TipoParticipante tipoParticipante;
    private LocalDateTime dataInscricao;
    private Boolean presencaConfirmada;
    private ClientResponseDTO client;
}
