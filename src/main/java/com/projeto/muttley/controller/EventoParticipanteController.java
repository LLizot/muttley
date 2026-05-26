package com.projeto.muttley.controller;

import com.projeto.muttley.dto.ApiResponse;
import com.projeto.muttley.dto.ConfirmacaoPresencaRequestDTO;
import com.projeto.muttley.dto.EventoParticipanteListResponseDTO;
import com.projeto.muttley.dto.EventoParticipanteResponseDTO;
import com.projeto.muttley.dto.InscricaoOuvinteRequestDTO;
import com.projeto.muttley.service.EventoParticipanteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events/{eventoId}")
public class EventoParticipanteController {

        private final EventoParticipanteService eventoParticipanteService;

        public EventoParticipanteController(EventoParticipanteService eventoParticipanteService) {
                this.eventoParticipanteService = eventoParticipanteService;
        }

        @PostMapping("/inscricoes")
        public ResponseEntity<ApiResponse<EventoParticipanteResponseDTO>> registrarInscricaoOuvinte(
                        @PathVariable UUID eventoId,
                        @Valid @RequestBody InscricaoOuvinteRequestDTO request,
                        HttpServletRequest httpRequest) {
                EventoParticipanteResponseDTO response = eventoParticipanteService.registrarInscricaoOuvinte(
                                eventoId,
                                request.getNome(),
                                request.getEmail(),
                                request.getCpf());

                ApiResponse<EventoParticipanteResponseDTO> body = ApiResponse.success(
                                HttpStatus.CREATED.value(),
                                "Inscricao registrada com sucesso",
                                httpRequest.getRequestURI(),
                                response);
                return ResponseEntity.status(HttpStatus.CREATED).body(body);
        }

        @PostMapping("/confirmacoes")
        public ResponseEntity<ApiResponse<EventoParticipanteResponseDTO>> confirmarPresenca(
                        @PathVariable UUID eventoId,
                        @Valid @RequestBody ConfirmacaoPresencaRequestDTO request,
                        HttpServletRequest httpRequest) {
                EventoParticipanteResponseDTO response = eventoParticipanteService.confirmarPresenca(
                                eventoId,
                                request.getCpf());

                ApiResponse<EventoParticipanteResponseDTO> body = ApiResponse.success(
                                HttpStatus.OK.value(),
                                "Presenca confirmada com sucesso",
                                httpRequest.getRequestURI(),
                                response);
                return ResponseEntity.ok(body);
        }

        @GetMapping("/participantes")
        public ResponseEntity<ApiResponse<EventoParticipanteListResponseDTO>> listarParticipantes(
                        @PathVariable UUID eventoId,
                        @PageableDefault(size = 10, sort = "dataInscricao", direction = Sort.Direction.DESC) Pageable pageable,
                        HttpServletRequest httpRequest) {
                int pageIndex = Math.max(pageable.getPageNumber() - 1, 0);
                Pageable adjusted = PageRequest.of(pageIndex, pageable.getPageSize(), pageable.getSort());
                EventoParticipanteListResponseDTO response = eventoParticipanteService.listarParticipantes(eventoId,
                                adjusted);
                ApiResponse<EventoParticipanteListResponseDTO> body = ApiResponse.success(
                                HttpStatus.OK.value(),
                                "Participantes listados com sucesso",
                                httpRequest.getRequestURI(),
                                response);
                return ResponseEntity.ok(body);
        }
}
