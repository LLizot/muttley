package com.projeto.muttley.service;

import com.projeto.muttley.dto.ClientResponseDTO;
import com.projeto.muttley.dto.EventoParticipanteResponseDTO;
import com.projeto.muttley.entity.Client;
import com.projeto.muttley.entity.Event;
import com.projeto.muttley.entity.EventoParticipante;
import com.projeto.muttley.entity.TipoParticipante;
import com.projeto.muttley.exception.ResourceNotFoundException;
import com.projeto.muttley.repository.ClientRepository;
import com.projeto.muttley.repository.EventRepository;
import com.projeto.muttley.repository.EventoParticipanteRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventoParticipanteService {

    private final ClientRepository clientRepository;
    private final EventRepository eventRepository;
    private final EventoParticipanteRepository eventoParticipanteRepository;

    public EventoParticipanteService(ClientRepository clientRepository,
            EventRepository eventRepository,
            EventoParticipanteRepository eventoParticipanteRepository) {
        this.clientRepository = clientRepository;
        this.eventRepository = eventRepository;
        this.eventoParticipanteRepository = eventoParticipanteRepository;
    }

    @Transactional
    public EventoParticipanteResponseDTO registrarInscricaoOuvinte(UUID eventoId,
            String nome,
            String email,
            String cpf) {
        Event event = eventRepository.findById(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento nao encontrado"));

        Client client = clientRepository.findByCpf(cpf)
                .orElseGet(() -> clientRepository.save(Client.builder()
                        .nome(nome)
                        .email(email)
                        .cpf(cpf)
                        .dataCriacao(LocalDateTime.now())
                        .build()));

        EventoParticipante participante = eventoParticipanteRepository
                .findByEventoIdAndClientId(event.getId(), client.getId())
                .orElseGet(() -> EventoParticipante.builder()
                        .evento(event)
                        .client(client)
                        .tipoParticipante(TipoParticipante.OUVINTE)
                        .dataInscricao(LocalDateTime.now())
                        .presencaConfirmada(Boolean.FALSE)
                        .build());

        EventoParticipante saved = eventoParticipanteRepository.save(participante);
        return toResponse(saved);
    }

    @Transactional
    public EventoParticipanteResponseDTO confirmarPresenca(UUID eventoId, String cpf) {
        Client client = clientRepository.findByCpf(cpf)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente nao encontrado"));

        EventoParticipante participante = eventoParticipanteRepository
                .findByEventoIdAndClientId(eventoId, client.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Inscricao nao encontrada"));

        participante.setPresencaConfirmada(Boolean.TRUE);
        EventoParticipante saved = eventoParticipanteRepository.save(participante);
        return toResponse(saved);
    }

    private EventoParticipanteResponseDTO toResponse(EventoParticipante participante) {
        return EventoParticipanteResponseDTO.builder()
                .id(participante.getId())
                .tipoParticipante(participante.getTipoParticipante())
                .dataInscricao(participante.getDataInscricao())
                .presencaConfirmada(participante.getPresencaConfirmada())
                .client(ClientResponseDTO.builder()
                        .id(participante.getClient().getId())
                        .nome(participante.getClient().getNome())
                        .email(participante.getClient().getEmail())
                        .cpf(participante.getClient().getCpf())
                        .build())
                .build();
    }
}
