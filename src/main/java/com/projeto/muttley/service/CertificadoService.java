package com.projeto.muttley.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.projeto.muttley.dto.CertificadoInfoResponseDTO;
import com.projeto.muttley.entity.Certificado;
import com.projeto.muttley.entity.Event;
import com.projeto.muttley.entity.EventoParticipante;
import com.projeto.muttley.exception.ResourceNotFoundException;
import com.projeto.muttley.repository.CertificadoRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CertificadoService {

    private static final float QR_SIZE = 220f;

    private final CertificadoRepository certificadoRepository;
    private final String certificateFrontBaseUrl;

    public CertificadoService(
            CertificadoRepository certificadoRepository,
            @Value("${certificate.front.base-url}") String certificateFrontBaseUrl) {
        this.certificadoRepository = certificadoRepository;
        this.certificateFrontBaseUrl = certificateFrontBaseUrl;
    }

    public Certificado createAndStore(Event event, EventoParticipante participante, byte[] pdfOriginal) {
        Certificado certificado = Certificado.builder()
                .evento(event)
                .participante(participante)
                .build();

        Certificado saved = certificadoRepository.save(certificado);
        String qrUrl = buildCertificateUrl(saved.getId());
        byte[] pdfWithQr = appendQrPage(pdfOriginal, qrUrl);

        saved.setPdfData(pdfWithQr);
        saved.setPdfUrl(buildPdfUrl(saved.getId()));
        return certificadoRepository.save(saved);
    }

    public CertificadoInfoResponseDTO getCertificateInfo(UUID certificateId) {
        Certificado certificado = certificadoRepository.findById(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificado nao encontrado"));

        return CertificadoInfoResponseDTO.builder()
                .nomeEvento(certificado.getEvento().getTitulo())
                .nomeParticipante(certificado.getParticipante().getClient().getNome())
                .emailParticipante(certificado.getParticipante().getClient().getEmail())
                .build();
    }

    public byte[] getCertificatePdf(UUID certificateId) {
        Certificado certificado = certificadoRepository.findById(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificado nao encontrado"));
        if (certificado.getPdfData() == null || certificado.getPdfData().length == 0) {
            throw new IllegalStateException("PDF do certificado nao encontrado");
        }
        return certificado.getPdfData();
    }

    private String buildCertificateUrl(UUID certificateId) {
        String normalized = certificateFrontBaseUrl.endsWith("/")
                ? certificateFrontBaseUrl.substring(0, certificateFrontBaseUrl.length() - 1)
                : certificateFrontBaseUrl;
        return normalized + "/certificate?certificateId=" + certificateId;
    }

    private String buildPdfUrl(UUID certificateId) {
        return "/certificates/" + certificateId + "/pdf";
    }

    private byte[] appendQrPage(byte[] pdfBytes, String qrContent) {
        try {
            PdfReader reader = new PdfReader(pdfBytes);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            PdfStamper stamper = new PdfStamper(reader, output);

            int pageNumber = reader.getNumberOfPages() + 1;
            Rectangle pageSize = reader.getPageSizeWithRotation(1);
            stamper.insertPage(pageNumber, pageSize);

            PdfContentByte content = stamper.getOverContent(pageNumber);
            Image qrImage = Image.getInstance(generateQrPng(qrContent));
            qrImage.scaleAbsolute(QR_SIZE, QR_SIZE);
            float x = (pageSize.getWidth() - QR_SIZE) / 2f;
            float y = (pageSize.getHeight() - QR_SIZE) / 2f;
            qrImage.setAbsolutePosition(x, y);
            content.addImage(qrImage);

            stamper.close();
            reader.close();
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao adicionar pagina de QR code", ex);
        }
    }

    private byte[] generateQrPng(String content) throws WriterException, IOException {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, 300, 300);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(MatrixToImageWriter.toBufferedImage(matrix), "PNG", output);
        return output.toByteArray();
    }
}
