package com.projeto.muttley.repository;

import com.projeto.muttley.entity.EventoParticipante;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoParticipanteRepository extends JpaRepository<EventoParticipante, UUID> {
    Optional<EventoParticipante> findByEventoIdAndClientId(UUID eventoId, UUID clientId);
}
