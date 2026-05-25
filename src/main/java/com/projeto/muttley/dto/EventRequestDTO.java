package com.projeto.muttley.dto;

import com.projeto.muttley.entity.Modalidade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestDTO {

    @NotBlank
    private String titulo;

    @NotNull
    private LocalDate dataInicial;

    @NotNull
    private LocalDate dataFinal;

    private Integer cargaHoraria;

    private Integer pontos;

    private String tipo;

    private String assuntoEvento;

    private String descricao;

    private String competencias;

    @NotNull
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

    @Valid
    private List<ParticipanteVinculoRequestDTO> participantesEquipe;
}
