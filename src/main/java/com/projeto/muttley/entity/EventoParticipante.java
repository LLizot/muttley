package com.projeto.muttley.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "evento_participantes", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "event_id", "client_id" })
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoParticipante {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Event evento;

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoParticipante tipoParticipante;

    @Column(nullable = false)
    private LocalDateTime dataInscricao;

    @Column(nullable = false)
    private Boolean presencaConfirmada;

    @PrePersist
    private void prePersist() {
        if (dataInscricao == null) {
            dataInscricao = LocalDateTime.now();
        }
        if (presencaConfirmada == null) {
            presencaConfirmada = Boolean.FALSE;
        }
    }
}
