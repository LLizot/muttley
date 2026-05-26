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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class EventoParticipanteService {

        private final ClientRepository clientRepository;
        private final EventRepository eventRepository;
        private final EventoParticipanteRepository eventoParticipanteRepository;
        private final JavaMailSender mailSender;
        private final RestTemplate restTemplate;
        private final String certificateServiceBaseUrl;

        public EventoParticipanteService(ClientRepository clientRepository,
                        EventRepository eventRepository,
                        EventoParticipanteRepository eventoParticipanteRepository,
                        JavaMailSender mailSender,
                        RestTemplateBuilder restTemplateBuilder,
                        @Value("${certificate.generator.base-url}") String certificateServiceBaseUrl) {
                this.clientRepository = clientRepository;
                this.eventRepository = eventRepository;
                this.eventoParticipanteRepository = eventoParticipanteRepository;
                this.mailSender = mailSender;
                this.restTemplate = restTemplateBuilder.build();
                this.certificateServiceBaseUrl = certificateServiceBaseUrl;
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
                byte[] planoDeFundo = readPlanoDeFundo(form);

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

                Event event = eventRepository.findById(form.getEventoId())
                                .orElseThrow(() -> new ResourceNotFoundException("Evento nao encontrado"));

                for (EventoParticipante participante : participantes) {
                        byte[] pdf = requestCertificatePdf(event, participante, planoDeFundo);
                        sendCertificateEmail(participante, pdf, event.getTitulo());
                }
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

        private byte[] readPlanoDeFundo(MedalhaEmLoteForm form) {
                if (form.getArquivoPlanoDeFundo() == null || form.getArquivoPlanoDeFundo().isEmpty()) {
                        return null;
                }
                try {
                        return form.getArquivoPlanoDeFundo().getBytes();
                } catch (IOException ex) {
                        throw new IllegalStateException("Falha ao ler arquivo de plano de fundo", ex);
                }
        }

        private byte[] requestCertificatePdf(Event event, EventoParticipante participante, byte[] planoDeFundo) {
                byte[] signatureImage = event.getAssinaturaSignatario();
                if (!isImageBuffer(signatureImage)) {
                        throw new IllegalStateException("Assinatura do signatario obrigatoria para gerar certificado");
                }

                byte[] backgroundImage = planoDeFundo;
                if (!isImageBuffer(backgroundImage)) {
                        backgroundImage = loadDefaultBackgroundImage();
                }

                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                body.add("name", participante.getClient().getNome());
                body.add("presentation", buildPresentation(participante));
                body.add("event", event.getTitulo());
                body.add("day", formatDay(event));
                body.add("hours", String.valueOf(toHours(event.getCargaHoraria())));
                body.add("responsible", event.getNomeSignatario());
                body.add("responsibleDescription", event.getCargoSignatario());
                body.add("backgroundImage", toFileResource(backgroundImage, "background.jpg"));
                body.add("signatureImage", toFileResource(signatureImage, "signature.png"));

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
                ResponseEntity<byte[]> response = restTemplate.postForEntity(
                                certificateServiceBaseUrl + "/api/certificate/generate",
                                request,
                                byte[].class);

                if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                        throw new IllegalStateException("Falha ao gerar PDF do certificado");
                }
                return response.getBody();
        }

        private void sendCertificateEmail(EventoParticipante participante, byte[] pdf, String tituloEvento) {
                try {
                        var message = mailSender.createMimeMessage();
                        var helper = new MimeMessageHelper(message, true);
                        helper.setFrom("lizotlucas06@gmail.com");
                        helper.setTo(participante.getClient().getEmail());
                        helper.setSubject("Certificado - " + tituloEvento);
                        helper.setText("Hello World", false);
                        helper.addAttachment("certificado.pdf", new org.springframework.core.io.ByteArrayResource(pdf));
                        mailSender.send(message);
                } catch (Exception ex) {
                        throw new IllegalStateException("Falha ao enviar certificado", ex);
                }
        }

        private String formatDay(Event event) {
                LocalDate date = event.getDataFinal() != null ? event.getDataFinal() : event.getDataInicial();
                if (date == null) {
                        date = LocalDate.now();
                }
                return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        }

        private String buildPresentation(EventoParticipante participante) {
                String descricao = participante.getDescricaoMedalha();
                String competencias = participante.getCompetenciasMedalha();
                if (descricao == null || descricao.isBlank()) {
                        descricao = "Participou do evento";
                }
                if (competencias == null || competencias.isBlank()) {
                        return descricao;
                }
                return descricao + ", " + competencias;
        }

        private int toHours(Integer cargaHoraria) {
                if (cargaHoraria == null) {
                        return 0;
                }
                return cargaHoraria;
        }

        private ByteArrayResource toFileResource(byte[] bytes, String filename) {
                return new ByteArrayResource(bytes) {
                        @Override
                        public String getFilename() {
                                return filename;
                        }
                };
        }

        private byte[] loadDefaultBackgroundImage() {
                try {
                        ClassPathResource resource = new ClassPathResource("templates/imagem teste.jpg");
                        return resource.getContentAsByteArray();
                } catch (IOException ex) {
                        throw new IllegalStateException("Falha ao carregar imagem padrao", ex);
                }
        }

        private boolean isImageBuffer(byte[] bytes) {
                if (bytes == null || bytes.length < 8) {
                        return false;
                }
                return isPng(bytes) || isJpeg(bytes);
        }

        private boolean isPng(byte[] bytes) {
                return (bytes[0] == (byte) 0x89
                                && bytes[1] == 0x50
                                && bytes[2] == 0x4E
                                && bytes[3] == 0x47
                                && bytes[4] == 0x0D
                                && bytes[5] == 0x0A
                                && bytes[6] == 0x1A
                                && bytes[7] == 0x0A);
        }

        private boolean isJpeg(byte[] bytes) {
                return (bytes[0] == (byte) 0xFF
                                && bytes[1] == (byte) 0xD8
                                && bytes[2] == (byte) 0xFF);
        }
}
