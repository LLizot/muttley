package com.projeto.muttley.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false)
    private LocalDate dataInicial;

    @Column(nullable = false)
    private LocalDate dataFinal;

    private Integer cargaHoraria;

    private Integer pontos;

    @Column(length = 120)
    private String tipo;

    @Column(length = 200)
    private String assuntoEvento;

    @Column(length = 2000)
    private String descricao;

    @Column(length = 2000)
    private String competencias;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Modalidade modalidade;

    @Column(length = 255)
    private String endereco;

    private Integer capacidade;

    @Lob
    @Column(name = "url_assinatura_signatario")
    private byte[] assinaturaSignatario;

    @Column(length = 120)
    private String nomeSignatario;

    @Column(length = 120)
    private String cargoSignatario;

    @Column(length = 1000)
    private String qrCodeInscricao;

    @Column(length = 500)
    private String urlInscricao;

    @Column(length = 1000)
    private String qrCodeConfirmacao;

    @Column(length = 500)
    private String urlConfirmacao;

    @Column(nullable = false)
    private LocalDateTime dataCriacao;

    @Column(nullable = false)
    private Boolean finalized;

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<EventoParticipante> participantes = new HashSet<>();

    @PrePersist
    private void prePersist() {
        if (dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }
        if (finalized == null) {
            finalized = Boolean.FALSE;
        }
    }
}
