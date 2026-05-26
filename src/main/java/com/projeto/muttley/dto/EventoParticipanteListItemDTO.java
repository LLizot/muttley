package com.projeto.muttley.dto;

import com.projeto.muttley.entity.TipoParticipante;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoParticipanteListItemDTO {

    private String nome;
    private String email;
    private TipoParticipante tipoParticipante;
    private LocalDateTime dataInscricao;
    private String statusPresenca;
}
