package com.projeto.muttley.dto;

import com.projeto.muttley.entity.TipoParticipante;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipanteVinculoRequestDTO {

    @NotNull
    private UUID clientId;

    @NotNull
    private TipoParticipante tipoParticipante;
}
