package com.projeto.muttley.repository.projection;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ClientAggregateProjection {

    UUID getId();

    String getNome();

    String getCpf();

    String getEmail();

    LocalDateTime getDataCriacao();

    Long getTotalPontos();

    Long getTotalCertificados();

    Long getTotalMedalhas();
}
