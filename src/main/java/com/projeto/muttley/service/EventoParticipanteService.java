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
import java.util.ArrayList;
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
                        sendCertificateEmail(participante, pdf, event);
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

        private void sendCertificateEmail(EventoParticipante participante, byte[] pdf, Event event) {
                try {
                        var message = mailSender.createMimeMessage();
                        var helper = new MimeMessageHelper(message, true);
                        helper.setFrom("lizotlucas06@gmail.com");
                        helper.setTo(participante.getClient().getEmail());
                        String subject = "Parabens! Seu certificado especial do evento " + event.getTitulo()
                                        + " esta disponivel";
                        helper.setSubject(subject);
                        helper.setText(buildCertificateEmailBody(event, participante), false);
                        helper.addAttachment("certificado.pdf", new org.springframework.core.io.ByteArrayResource(pdf));
                        mailSender.send(message);
                } catch (Exception ex) {
                        throw new IllegalStateException("Falha ao enviar certificado", ex);
                }
        }

        private String buildCertificateEmailBody(Event event, EventoParticipante participante) {
                String competenciasLine = buildCompetenciasLine(chooseCompetencias(event, participante));
                String competenciasStep = "";
                String saveStep = "7. Salve o cadastro para exibir o certificado no seu perfil.\n\n";
                if (competenciasLine != null && !competenciasLine.isBlank()) {
                        competenciasStep = "6. Na parte de competencias, adicione manualmente as seguintes competencias:\n"
                                        + "   " + competenciasLine + ".\n";
                } else {
                        saveStep = "6. Salve o cadastro para exibir o certificado no seu perfil.\n\n";
                }

                return "Ola!\n\n"
                                + "Parabens pela sua conquista! Seu certificado especial referente ao evento "
                                + event.getTitulo() + " ja esta disponivel em anexo.\n\n"
                                + "Para adicionar essa conquista ao seu perfil no LinkedIn, siga este passo a passo:\n\n"
                                + "1. Acesse seu perfil no LinkedIn.\n"
                                + "2. Va ate a secao Licencas e certificados.\n"
                                + "3. Clique em Adicionar licenca ou certificado.\n"
                                + "4. Preencha as informacoes do certificado conforme constam no arquivo em anexo.\n"
                                + "5. No campo de organizacao emissora, informe Fatec Zona Leste.\n"
                                + competenciasStep
                                + saveStep
                                + "Alem disso, aproveite essa conquista para publicar o certificado especial no seu feed do LinkedIn! "
                                + "Compartilhar esse reconhecimento e uma forma direta de destacar sua participacao, fortalecer sua imagem profissional e ampliar a visibilidade do seu perfil. "
                                + "Ao publicar, nao se esqueca de mencionar tambem a Fatec Zona Leste no texto!\n\n"
                                + "Atenciosamente,\n"
                                + "Fatec Zona Leste";
        }

        private String chooseCompetencias(Event event, EventoParticipante participante) {
                String competenciasMedalha = participante.getCompetenciasMedalha();
                if (competenciasMedalha != null && !competenciasMedalha.isBlank()) {
                        return competenciasMedalha;
                }
                return event.getCompetencias();
        }

        private String buildCompetenciasLine(String competenciasRaw) {
                List<String> itens = new ArrayList<>();
                if (competenciasRaw != null && !competenciasRaw.isBlank()) {
                        String[] parts = competenciasRaw.split("[,;\\n]");
                        for (String part : parts) {
                                String trimmed = part.trim();
                                if (!trimmed.isEmpty()) {
                                        itens.add(trimmed);
                                }
                        }
                }

                return String.join(", ", itens);
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
                if (descricao == null || descricao.isBlank()) {
                        descricao = "Participou do evento";
                }
                return descricao;
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
                        ClassPathResource resource = new ClassPathResource("templates/background.png");
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
