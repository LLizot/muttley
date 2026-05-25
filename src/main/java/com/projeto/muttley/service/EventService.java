package com.projeto.muttley.service;

import com.projeto.muttley.dto.ClientResponseDTO;
import com.projeto.muttley.dto.EventRequestDTO;
import com.projeto.muttley.dto.EventResponseDTO;
import com.projeto.muttley.dto.EventSummaryDTO;
import com.projeto.muttley.dto.EventoParticipanteResponseDTO;
import com.projeto.muttley.dto.ParticipanteVinculoRequestDTO;
import com.projeto.muttley.entity.Client;
import com.projeto.muttley.entity.Event;
import com.projeto.muttley.entity.EventoParticipante;
import com.projeto.muttley.entity.TipoParticipante;
import com.projeto.muttley.exception.EventFinalizedException;
import com.projeto.muttley.exception.ResourceNotFoundException;
import com.projeto.muttley.repository.ClientRepository;
import com.projeto.muttley.repository.EventRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final ClientRepository clientRepository;

    public EventService(EventRepository eventRepository, ClientRepository clientRepository) {
        this.eventRepository = eventRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional
    public EventResponseDTO create(EventRequestDTO request) {
        Event event = buildEventFromRequest(request);
        attachEquipeParticipantes(event, request.getParticipantesEquipe());
        Event saved = eventRepository.save(event);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<EventSummaryDTO> findAll(String titulo, Pageable pageable) {
        Page<Event> page = (titulo == null || titulo.isBlank())
                ? eventRepository.findAll(pageable)
                : eventRepository.findByTituloContainingIgnoreCase(titulo, pageable);
        return page.map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public EventResponseDTO findById(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento nao encontrado"));
        return toResponse(event);
    }

    @Transactional
    public EventResponseDTO update(UUID id, EventRequestDTO request) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento nao encontrado"));

        validateNotFinalized(event);

        updateEventFromRequest(event, request);
        if (request.getParticipantesEquipe() != null) {
            attachEquipeParticipantes(event, request.getParticipantesEquipe());
        }

        Event saved = eventRepository.save(event);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento nao encontrado"));
        validateNotFinalized(event);
        eventRepository.delete(event);
    }

    @Transactional
    public EventResponseDTO finalizeEvent(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento nao encontrado"));

        if (Boolean.FALSE.equals(event.getFinalized())) {
            event.setFinalized(Boolean.TRUE);
        }

        Event saved = eventRepository.save(event);
        return toResponse(saved);
    }

    private Event buildEventFromRequest(EventRequestDTO request) {
        Event event = new Event();
        updateEventFromRequest(event, request);
        return event;
    }

    private void updateEventFromRequest(Event event, EventRequestDTO request) {
        event.setTitulo(request.getTitulo());
        event.setDataInicial(request.getDataInicial());
        event.setDataFinal(request.getDataFinal());
        event.setCargaHoraria(request.getCargaHoraria());
        event.setPontos(request.getPontos());
        event.setTipo(request.getTipo());
        event.setAssuntoEvento(request.getAssuntoEvento());
        event.setDescricao(request.getDescricao());
        event.setCompetencias(request.getCompetencias());
        event.setModalidade(request.getModalidade());
        event.setEndereco(request.getEndereco());
        event.setCapacidade(request.getCapacidade());
        event.setUrlAssinaturaSignatario(request.getUrlAssinaturaSignatario());
        event.setNomeSignatario(request.getNomeSignatario());
        event.setCargoSignatario(request.getCargoSignatario());
        event.setQrCodeInscricao(request.getQrCodeInscricao());
        event.setUrlInscricao(request.getUrlInscricao());
        event.setQrCodeConfirmacao(request.getQrCodeConfirmacao());
        event.setUrlConfirmacao(request.getUrlConfirmacao());
    }

    private void validateNotFinalized(Event event) {
        if (Boolean.TRUE.equals(event.getFinalized())) {
            throw new EventFinalizedException("Evento finalizado nao pode ser alterado ou removido");
        }
    }

    private void attachEquipeParticipantes(Event event, List<ParticipanteVinculoRequestDTO> equipe) {
        if (equipe == null || equipe.isEmpty()) {
            return;
        }

        Map<UUID, EventoParticipante> existentes = new HashMap<>();
        for (EventoParticipante participante : event.getParticipantes()) {
            existentes.put(participante.getClient().getId(), participante);
        }

        for (ParticipanteVinculoRequestDTO item : equipe) {
            Client client = clientRepository.findById(item.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente nao encontrado"));

            EventoParticipante participante = existentes.get(client.getId());
            if (participante == null) {
                participante = EventoParticipante.builder()
                        .evento(event)
                        .client(client)
                        .dataInscricao(LocalDateTime.now())
                        .presencaConfirmada(Boolean.FALSE)
                        .build();
                event.getParticipantes().add(participante);
                existentes.put(client.getId(), participante);
            }

            TipoParticipante tipo = item.getTipoParticipante();
            if (tipo != null) {
                participante.setTipoParticipante(tipo);
            }
        }
    }

    private EventResponseDTO toResponse(Event event) {
        List<EventoParticipanteResponseDTO> participantes = event.getParticipantes().stream()
                .map(participante -> EventoParticipanteResponseDTO.builder()
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
                        .build())
                .toList();

        return EventResponseDTO.builder()
                .id(event.getId())
                .titulo(event.getTitulo())
                .dataInicial(event.getDataInicial())
                .dataFinal(event.getDataFinal())
                .cargaHoraria(event.getCargaHoraria())
                .pontos(event.getPontos())
                .tipo(event.getTipo())
                .assuntoEvento(event.getAssuntoEvento())
                .descricao(event.getDescricao())
                .competencias(event.getCompetencias())
                .modalidade(event.getModalidade())
                .endereco(event.getEndereco())
                .capacidade(event.getCapacidade())
                .urlAssinaturaSignatario(event.getUrlAssinaturaSignatario())
                .nomeSignatario(event.getNomeSignatario())
                .cargoSignatario(event.getCargoSignatario())
                .qrCodeInscricao(event.getQrCodeInscricao())
                .urlInscricao(event.getUrlInscricao())
                .qrCodeConfirmacao(event.getQrCodeConfirmacao())
                .urlConfirmacao(event.getUrlConfirmacao())
                .dataCriacao(event.getDataCriacao())
                .finalized(event.getFinalized())
                .participantes(participantes)
                .build();
    }

    private EventSummaryDTO toSummary(Event event) {
        return EventSummaryDTO.builder()
                .titulo(event.getTitulo())
                .modalidade(event.getModalidade())
                .dataInicial(event.getDataInicial())
                .dataFinal(event.getDataFinal())
                .finalized(event.getFinalized())
                .build();
    }
}
