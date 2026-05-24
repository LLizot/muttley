package com.projeto.muttley.service;

import com.projeto.muttley.dto.ClientResponseDTO;
import com.projeto.muttley.dto.EventRequestDTO;
import com.projeto.muttley.dto.EventResponseDTO;
import com.projeto.muttley.entity.Client;
import com.projeto.muttley.entity.Event;
import com.projeto.muttley.exception.ResourceNotFoundException;
import com.projeto.muttley.repository.ClientRepository;
import com.projeto.muttley.repository.EventRepository;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
        Event event = new Event();
        event.setNome(request.getNome());
        event.setClientes(resolveClients(request.getClientIds()));
        Event saved = eventRepository.save(event);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EventResponseDTO> findAll() {
        return eventRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EventResponseDTO findById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento nao encontrado"));
        return toResponse(event);
    }

    @Transactional
    public EventResponseDTO update(Long id, EventRequestDTO request) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento nao encontrado"));

        event.setNome(request.getNome());
        if (request.getClientIds() != null) {
            event.setClientes(resolveClients(request.getClientIds()));
        }

        Event saved = eventRepository.save(event);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento nao encontrado"));
        eventRepository.delete(event);
    }

    private Set<Client> resolveClients(List<Long> clientIds) {
        if (clientIds == null || clientIds.isEmpty()) {
            return Collections.emptySet();
        }

        List<Client> clients = clientRepository.findAllById(clientIds);
        if (clients.size() != clientIds.size()) {
            throw new ResourceNotFoundException("Um ou mais clientes nao encontrados");
        }
        return clients.stream().collect(Collectors.toSet());
    }

    private EventResponseDTO toResponse(Event event) {
        List<ClientResponseDTO> clients = event.getClientes().stream()
                .map(client -> ClientResponseDTO.builder()
                        .id(client.getId())
                        .nome(client.getNome())
                        .cpf(client.getCpf())
                        .build())
                .toList();

        return EventResponseDTO.builder()
                .id(event.getId())
                .nome(event.getNome())
                .clientes(clients)
                .build();
    }
}
