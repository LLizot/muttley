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
import com.projeto.muttley.repository.EventoParticipanteRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final ClientRepository clientRepository;
    private final EventoParticipanteRepository eventoParticipanteRepository;
    private final JavaMailSender mailSender;
    private final RestTemplate restTemplate;
    private final String certificateServiceBaseUrl;
    private final String registrationBaseUrl;
    private final String presenceBaseUrl;

    public EventService(EventRepository eventRepository,
            ClientRepository clientRepository,
            EventoParticipanteRepository eventoParticipanteRepository,
            JavaMailSender mailSender,
            RestTemplateBuilder restTemplateBuilder,
            @Value("${certificate.generator.base-url}") String certificateServiceBaseUrl,
            @Value("${event.registration.base-url}") String registrationBaseUrl,
            @Value("${event.presence.base-url}") String presenceBaseUrl) {
        this.eventRepository = eventRepository;
        this.clientRepository = clientRepository;
        this.eventoParticipanteRepository = eventoParticipanteRepository;
        this.mailSender = mailSender;
        this.restTemplate = restTemplateBuilder.build();
        this.certificateServiceBaseUrl = certificateServiceBaseUrl;
        this.registrationBaseUrl = registrationBaseUrl;
        this.presenceBaseUrl = presenceBaseUrl;
    }

    @Transactional
    public EventResponseDTO create(EventFormData form) {
        Event event = buildEventFromForm(form);
        attachEquipeParticipantes(event, form.getParticipantesEquipe());
        Event saved = eventRepository.save(event);
        populateAccessLinks(saved);
        Event updated = eventRepository.save(saved);
        return toResponse(updated);
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

        List<EventoParticipante> presentes = eventoParticipanteRepository
                .findByEventoIdAndPresencaConfirmadaTrue(saved.getId());

        for (EventoParticipante participante : presentes) {
            byte[] pdf = requestCertificatePdf(saved, participante);
            sendCertificateEmail(participante, pdf, saved);
        }
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
    }

    private void populateAccessLinks(Event event) {
        String registrationUrl = buildEventUrl(registrationBaseUrl, event.getId());
        String presenceUrl = buildEventUrl(presenceBaseUrl, event.getId());

        event.setUrlInscricao(registrationUrl);
        event.setUrlConfirmacao(presenceUrl);
        event.setQrCodeInscricao(generateQrCodeDataUri(registrationUrl));
        event.setQrCodeConfirmacao(generateQrCodeDataUri(presenceUrl));
    }

    private String buildEventUrl(String baseUrl, UUID eventId) {
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalized + "?eventId=" + eventId;
    }

    private String generateQrCodeDataUri(String content) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, 150, 150);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(MatrixToImageWriter.toBufferedImage(matrix), "PNG", output);
            String base64 = Base64.getEncoder().encodeToString(output.toByteArray());
            return "data:image/png;base64," + base64;
        } catch (WriterException | IOException ex) {
            throw new IllegalStateException("Falha ao gerar QR code", ex);
        }
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

    private byte[] requestCertificatePdf(Event event, EventoParticipante participante) {
        byte[] signatureImage = event.getAssinaturaSignatario();
        if (!isImageBuffer(signatureImage)) {
            throw new IllegalStateException("Assinatura do signatario obrigatoria para gerar certificado");
        }

        byte[] backgroundImage = loadDefaultBackgroundImage();

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("name", participante.getClient().getNome());
        body.add("presentation", buildPresentation(event));
        body.add("event", event.getTitulo());
        body.add("day", formatDay(event));
        body.add("hours", String.valueOf(toHours(event.getCargaHoraria())));
        body.add("responsible", event.getNomeSignatario());
        body.add("responsibleDescription", event.getCargoSignatario());
        body.add("backgroundImage", toFileResource(backgroundImage, "background.png"));
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
            String subject = "Seu certificado do evento " + event.getTitulo() + " esta disponivel";
            helper.setSubject(subject);
            helper.setText(buildCertificateEmailBody(event), false);
            helper.addAttachment("certificado.pdf", new org.springframework.core.io.ByteArrayResource(pdf));
            mailSender.send(message);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao enviar certificado", ex);
        }
    }

    private String buildCertificateEmailBody(Event event) {
        String competenciasLine = buildCompetenciasLine(event.getCompetencias());
        return "Ola!\n\n"
                + "Seu certificado de participacao no evento " + event.getTitulo() + " ja esta disponivel em anexo.\n\n"
                + "Para adicionar essa conquista ao seu perfil no LinkedIn, siga este passo a passo:\n\n"
                + "1. Acesse seu perfil no LinkedIn.\n"
                + "2. Va ate a secao Licencas e certificados.\n"
                + "3. Clique em Adicionar licenca ou certificado.\n"
                + "4. Preencha as informacoes do certificado conforme constam no arquivo em anexo.\n"
                + "5. No campo de organizacao emissora, informe Fatec Zona Leste.\n"
                + "6. Na parte de competencias, adicione manualmente as seguintes competencias:\n"
                + "   " + competenciasLine + ".\n"
                + "7. Salve o cadastro para exibir o certificado no seu perfil.\n\n"
                + "Alem disso, nao deixe essa conquista apenas no perfil e publique o certificado no seu feed do LinkedIn para destacar sua participacao no evento. "
                + "Esse e um passo importante para reforcar sua presenca profissional e ampliar a visibilidade do seu perfil para recrutadores e contatos da area. "
                + "Ao publicar, nao se esqueca de mencionar tambem a Fatec Zona Leste no texto!\n\n"
                + "Atenciosamente,\n"
                + "Fatec Zona Leste";
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

    private String buildPresentation(Event event) {
        if (event.getDescricao() != null && !event.getDescricao().isBlank()) {
            return event.getDescricao();
        }
        if (event.getAssuntoEvento() != null && !event.getAssuntoEvento().isBlank()) {
            return event.getAssuntoEvento();
        }
        return "Participou do evento";
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

    private byte[] defaultTransparentPng() {
        return new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89,
                0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41, 0x54,
                0x78, (byte) 0x9C, 0x63, 0x60, 0x00, 0x00, 0x00, 0x02,
                0x00, 0x01, (byte) 0xE2, 0x26, 0x05, (byte) 0x9B,
                0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
                (byte) 0xAE, 0x42, 0x60, (byte) 0x82
        };
    }

    private byte[] loadDefaultBackgroundImage() {
        try {
            ClassPathResource resource = new ClassPathResource("templates/background.png");
            return resource.getContentAsByteArray();
        } catch (IOException ex) {
            return defaultTransparentPng();
        }
    }
}
