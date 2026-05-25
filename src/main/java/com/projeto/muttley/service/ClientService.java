package com.projeto.muttley.service;

import com.projeto.muttley.dto.ClientCreateRequestDTO;
import com.projeto.muttley.dto.ClientResponseDTO;
import com.projeto.muttley.entity.Client;
import com.projeto.muttley.exception.ResourceAlreadyExistsException;
import com.projeto.muttley.repository.ClientRepository;
import com.projeto.muttley.repository.projection.ClientAggregateProjection;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional
    public ClientResponseDTO create(ClientCreateRequestDTO request) {
        clientRepository.findByCpf(request.getCpf())
                .ifPresent(existing -> {
                    throw new ResourceAlreadyExistsException("CPF ja cadastrado");
                });

        Client client = Client.builder()
                .nome(request.getNome())
                .cpf(request.getCpf())
                .email(request.getEmail())
                .dataCriacao(LocalDateTime.now())
                .build();

        Client saved = clientRepository.save(client);
        return ClientResponseDTO.builder()
                .id(saved.getId())
                .nome(saved.getNome())
                .cpf(saved.getCpf())
                .email(saved.getEmail())
                .dataCriacao(saved.getDataCriacao())
                .totalPontos(0)
                .totalCertificados(0)
                .totalMedalhas(0)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<ClientResponseDTO> findAll(String nome, Pageable pageable) {
        Page<ClientAggregateProjection> page = clientRepository.findAllWithAggregates(nome, pageable);
        return page.map(this::toResponse);
    }

    private ClientResponseDTO toResponse(ClientAggregateProjection projection) {
        return ClientResponseDTO.builder()
                .id(projection.getId())
                .nome(projection.getNome())
                .cpf(projection.getCpf())
                .email(projection.getEmail())
                .dataCriacao(projection.getDataCriacao())
                .totalPontos(toInt(projection.getTotalPontos()))
                .totalCertificados(toInt(projection.getTotalCertificados()))
                .totalMedalhas(toInt(projection.getTotalMedalhas()))
                .build();
    }

    private int toInt(Long value) {
        if (value == null) {
            return 0;
        }
        return Math.toIntExact(value);
    }
}
