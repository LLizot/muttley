package com.projeto.muttley.dto;

import com.projeto.muttley.entity.Modalidade;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSummaryDTO {

    private UUID id;
    private String titulo;
    private Modalidade modalidade;
    private LocalDate dataInicial;
    private LocalDate dataFinal;
    private Boolean finalized;
}
