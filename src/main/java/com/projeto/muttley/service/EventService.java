package com.projeto.muttley.service;

import com.projeto.muttley.dto.EventFormData;
import com.projeto.muttley.dto.EventResponseDTO;
import com.projeto.muttley.dto.EventSummaryDTO;
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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
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
    public EventResponseDTO create(EventFormData form) {
        Event event = buildEventFromForm(form);
        attachEquipeParticipantes(event, form.getParticipantesEquipe());
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
    public EventResponseDTO update(UUID id, EventFormData form) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento nao encontrado"));

        validateNotFinalized(event);

        updateEventFromForm(event, form, false);
        if (form.getParticipantesEquipe() != null) {
            attachEquipeParticipantes(event, form.getParticipantesEquipe());
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

    private Event buildEventFromForm(EventFormData form) {
        Event event = new Event();
        updateEventFromForm(event, form, true);
        return event;
    }

    private void updateEventFromForm(Event event, EventFormData form, boolean requiresSignature) {
        event.setTitulo(form.getTitulo());
        event.setDataInicial(form.getDataInicial());
        event.setDataFinal(form.getDataFinal());
        event.setCargaHoraria(form.getCargaHoraria());
        event.setPontos(form.getPontos());
        event.setTipo(form.getTipo());
        event.setAssuntoEvento(form.getAssuntoEvento());
        event.setDescricao(form.getDescricao());
        event.setCompetencias(form.getCompetencias());
        event.setModalidade(form.getModalidade());
        event.setEndereco(form.getEndereco());
        event.setCapacidade(form.getCapacidade());
        if (form.getArquivoAssinaturaSignatario() != null && !form.getArquivoAssinaturaSignatario().isEmpty()) {
            event.setAssinaturaSignatario(toBytes(form.getArquivoAssinaturaSignatario()));
        } else if (requiresSignature) {
            throw new IllegalStateException("Arquivo de assinatura do signatario obrigatorio");
        }
        event.setNomeSignatario(form.getNomeSignatario());
        event.setCargoSignatario(form.getCargoSignatario());
        event.setQrCodeInscricao(form.getQrCodeInscricao());
        event.setUrlInscricao(form.getUrlInscricao());
        event.setQrCodeConfirmacao(form.getQrCodeConfirmacao());
        event.setUrlConfirmacao(form.getUrlConfirmacao());
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
                .assinaturaSignatario(event.getAssinaturaSignatario())
                .nomeSignatario(event.getNomeSignatario())
                .cargoSignatario(event.getCargoSignatario())
                .qrCodeInscricao(event.getQrCodeInscricao())
                .urlInscricao(event.getUrlInscricao())
                .qrCodeConfirmacao(event.getQrCodeConfirmacao())
                .urlConfirmacao(event.getUrlConfirmacao())
                .dataCriacao(event.getDataCriacao())
                .finalized(event.getFinalized())
                .build();
    }

    private byte[] toBytes(org.springframework.web.multipart.MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao ler arquivo de assinatura", ex);
        }
    }

    private EventSummaryDTO toSummary(Event event) {
        return EventSummaryDTO.builder()
                .id(event.getId())
                .titulo(event.getTitulo())
                .modalidade(event.getModalidade())
                .dataInicial(event.getDataInicial())
                .dataFinal(event.getDataFinal())
                .finalized(event.getFinalized())
                .build();
    }
}
