package com.projeto.muttley.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoParticipanteListResponseDTO {

    private String proporcaoParticipantes;
    private Page<EventoParticipanteListItemDTO> participantes;
}
