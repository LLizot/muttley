package com.projeto.muttley.controller;

import com.projeto.muttley.dto.ApiResponse;
import com.projeto.muttley.dto.MedalhaEmLoteForm;
import com.projeto.muttley.service.EventoParticipanteService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events")
public class EventoParticipanteMedalhaController {

    private final EventoParticipanteService eventoParticipanteService;

    public EventoParticipanteMedalhaController(EventoParticipanteService eventoParticipanteService) {
        this.eventoParticipanteService = eventoParticipanteService;
    }

    @PostMapping("/participantes/medalha")
    public ResponseEntity<ApiResponse<Object>> concederMedalhaEmLote(
            @ModelAttribute MedalhaEmLoteForm form,
            HttpServletRequest httpRequest) {
        eventoParticipanteService.concederMedalhaEmLote(form);
        ApiResponse<Object> body = ApiResponse.success(
                HttpStatus.OK.value(),
                "Medalhas concedidas com sucesso",
                httpRequest.getRequestURI(),
                null);
        return ResponseEntity.ok(body);
    }
}
