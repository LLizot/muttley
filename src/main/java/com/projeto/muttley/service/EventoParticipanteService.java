package com.projeto.muttley.service;

import com.projeto.muttley.dto.ClientResponseDTO;
import com.projeto.muttley.dto.EventoParticipanteListItemDTO;
import com.projeto.muttley.dto.EventoParticipanteListResponseDTO;
import com.projeto.muttley.dto.EventoParticipanteResponseDTO;
import com.projeto.muttley.dto.MedalhaEmLoteForm;
import com.projeto.muttley.entity.Client;
import com.projeto.muttley.entity.Event;
import com.projeto.muttley.entity.EventoParticipante;
import com.projeto.muttley.entity.TipoParticipante;
import com.projeto.muttley.exception.ResourceNotFoundException;
import com.projeto.muttley.repository.ClientRepository;
import com.projeto.muttley.repository.EventRepository;
import com.projeto.muttley.repository.EventoParticipanteRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

        @Transactional(readOnly = true)
        public EventoParticipanteListResponseDTO listarParticipantes(UUID eventoId, Pageable pageable) {
                if (!eventRepository.existsById(eventoId)) {
                        throw new ResourceNotFoundException("Evento nao encontrado");
                }

                long totalInscritos = eventoParticipanteRepository.countByEventoId(eventoId);
                long totalPresentes = eventoParticipanteRepository.countByEventoIdAndPresencaConfirmadaTrue(eventoId);
                String proporcao = totalInscritos + "/" + totalPresentes;

                Page<EventoParticipanteListItemDTO> participantes = eventoParticipanteRepository
                                .findByEventoId(eventoId, pageable)
                                .map(this::toListItem);

                return EventoParticipanteListResponseDTO.builder()
                                .proporcaoParticipantes(proporcao)
                                .participantes(participantes)
                                .build();
        }

        @Transactional
        public void concederMedalhaEmLote(MedalhaEmLoteForm form) {
                byte[] planoDeFundo = toBytes(form);

                List<UUID> ids = form.getParticipanteIds();
                List<EventoParticipante> participantes = eventoParticipanteRepository
                                .findByEventoIdAndIdIn(form.getEventoId(), ids);

                if (participantes.isEmpty()) {
                        participantes = eventoParticipanteRepository
                                        .findByEventoIdAndClientIdIn(form.getEventoId(), ids);
                }

                if (participantes.isEmpty()) {
                        throw new ResourceNotFoundException("Participantes nao encontrados para o evento");
                }

                for (EventoParticipante participante : participantes) {
                        participante.setGanhouMedalha(Boolean.TRUE);
                        participante.setDescricaoMedalha(form.getDescricaoMedalha());
                        participante.setCompetenciasMedalha(form.getCompetenciasMedalha());
                        participante.setPlanoDeFundoMedalha(planoDeFundo);
                }

                eventoParticipanteRepository.saveAll(participantes);
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

        private EventoParticipanteListItemDTO toListItem(EventoParticipante participante) {
                String status = Boolean.TRUE.equals(participante.getPresencaConfirmada())
                                ? "PRESENTE"
                                : "INSCRITO";

                return EventoParticipanteListItemDTO.builder()
                                .id(participante.getId())
                                .nome(participante.getClient().getNome())
                                .email(participante.getClient().getEmail())
                                .tipoParticipante(participante.getTipoParticipante())
                                .dataInscricao(participante.getDataInscricao())
                                .statusPresenca(status)
                                .build();
        }

        private byte[] toBytes(MedalhaEmLoteForm form) {
                try {
                        if (form.getArquivoPlanoDeFundo() == null || form.getArquivoPlanoDeFundo().isEmpty()) {
                                throw new IllegalStateException("Arquivo de plano de fundo obrigatorio");
                        }
                        return form.getArquivoPlanoDeFundo().getBytes();
                } catch (IOException ex) {
                        throw new IllegalStateException("Falha ao ler arquivo de plano de fundo", ex);
                }
        }
}
