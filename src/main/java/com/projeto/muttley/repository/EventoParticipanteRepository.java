package com.projeto.muttley.repository;

import com.projeto.muttley.entity.EventoParticipante;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoParticipanteRepository extends JpaRepository<EventoParticipante, UUID> {
    Optional<EventoParticipante> findByEventoIdAndClientId(UUID eventoId, UUID clientId);

    Page<EventoParticipante> findByEventoId(UUID eventoId, Pageable pageable);

    long countByEventoId(UUID eventoId);

    long countByEventoIdAndPresencaConfirmadaTrue(UUID eventoId);

    List<EventoParticipante> findByEventoIdAndIdIn(UUID eventoId, List<UUID> ids);

    List<EventoParticipante> findByEventoIdAndClientIdIn(UUID eventoId, List<UUID> clientIds);
}
