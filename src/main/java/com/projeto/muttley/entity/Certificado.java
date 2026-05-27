package com.projeto.muttley.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "certificados")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Certificado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Event evento;

    @ManyToOne(optional = false)
    @JoinColumn(name = "participante_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private EventoParticipante participante;

    @Column(length = 1000)
    private String pdfUrl;

    @Lob
    private byte[] pdfData;

    @Column(nullable = false)
    private LocalDateTime dataCriacao;

    @PrePersist
    private void prePersist() {
        if (dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }
    }
}
