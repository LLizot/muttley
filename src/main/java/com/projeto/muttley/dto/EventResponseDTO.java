package com.projeto.muttley.dto;

import com.projeto.muttley.entity.Modalidade;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponseDTO {

    private UUID id;
    private String titulo;
    private LocalDate dataInicial;
    private LocalDate dataFinal;
    private Integer cargaHoraria;
    private Integer pontos;
    private String tipo;
    private String assuntoEvento;
    private String descricao;
    private String competencias;
    private Modalidade modalidade;
    private String endereco;
    private Integer capacidade;
    private String urlAssinaturaSignatario;
    private String nomeSignatario;
    private String cargoSignatario;
    private String qrCodeInscricao;
    private String urlInscricao;
    private String qrCodeConfirmacao;
    private String urlConfirmacao;
    private LocalDateTime dataCriacao;
    private Boolean finalized;
    private List<EventoParticipanteResponseDTO> participantes;
}
