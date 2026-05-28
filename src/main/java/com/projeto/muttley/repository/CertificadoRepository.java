package com.projeto.muttley.repository;

import com.projeto.muttley.entity.Certificado;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificadoRepository extends JpaRepository<Certificado, UUID> {
    void deleteByEventoId(UUID eventoId);

    void deleteByParticipanteIdIn(java.util.List<UUID> participanteIds);
}
