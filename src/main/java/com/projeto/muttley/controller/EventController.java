package com.projeto.muttley.controller;

import com.projeto.muttley.dto.ApiResponse;
import com.projeto.muttley.dto.EventFormData;
import com.projeto.muttley.dto.EventResponseDTO;
import com.projeto.muttley.dto.EventSummaryDTO;
import com.projeto.muttley.service.EventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events")
public class EventController {

        private final EventService eventService;

        public EventController(EventService eventService) {
                this.eventService = eventService;
        }

        @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<ApiResponse<EventResponseDTO>> create(
                        @Valid @ModelAttribute EventFormData form,
                        HttpServletRequest httpRequest) {
                EventResponseDTO response = eventService.create(form);
                ApiResponse<EventResponseDTO> body = ApiResponse.success(
                                HttpStatus.CREATED.value(),
                                "Evento criado com sucesso",
                                httpRequest.getRequestURI(),
                                response);
                return ResponseEntity.status(HttpStatus.CREATED).body(body);
        }

        @GetMapping
        public ResponseEntity<ApiResponse<Page<EventSummaryDTO>>> findAll(
                        @RequestParam(required = false) String titulo,
                        @PageableDefault(size = 10, sort = "dataInicial", direction = Sort.Direction.DESC) Pageable pageable,
                        HttpServletRequest httpRequest) {
                int pageIndex = Math.max(pageable.getPageNumber() - 1, 0);
                Pageable adjusted = PageRequest.of(pageIndex, pageable.getPageSize(), pageable.getSort());
                Page<EventSummaryDTO> response = eventService.findAll(titulo, adjusted);
                ApiResponse<Page<EventSummaryDTO>> body = ApiResponse.success(
                                HttpStatus.OK.value(),
                                "Eventos listados com sucesso",
                                httpRequest.getRequestURI(),
                                response);
                return ResponseEntity.ok(body);
        }

        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<EventResponseDTO>> findById(
                        @PathVariable UUID id,
                        HttpServletRequest httpRequest) {
                EventResponseDTO response = eventService.findById(id);
                ApiResponse<EventResponseDTO> body = ApiResponse.success(
                                HttpStatus.OK.value(),
                                "Evento encontrado com sucesso",
                                httpRequest.getRequestURI(),
                                response);
                return ResponseEntity.ok(body);
        }

        @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<ApiResponse<EventResponseDTO>> update(
                        @PathVariable UUID id,
                        @Valid @ModelAttribute EventFormData form,
                        HttpServletRequest httpRequest) {
                EventResponseDTO response = eventService.update(id, form);
                ApiResponse<EventResponseDTO> body = ApiResponse.success(
                                HttpStatus.OK.value(),
                                "Evento atualizado com sucesso",
                                httpRequest.getRequestURI(),
                                response);
                return ResponseEntity.ok(body);
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<ApiResponse<Object>> delete(
                        @PathVariable UUID id,
                        HttpServletRequest httpRequest) {
                eventService.delete(id);
                ApiResponse<Object> body = ApiResponse.success(
                                HttpStatus.OK.value(),
                                "Evento removido com sucesso",
                                httpRequest.getRequestURI(),
                                null);
                return ResponseEntity.ok(body);
        }

        @PatchMapping("/{id}/finalizar")
        public ResponseEntity<ApiResponse<EventResponseDTO>> finalizeEvent(
                        @PathVariable UUID id,
                        HttpServletRequest httpRequest) {
                EventResponseDTO response = eventService.finalizeEvent(id);
                ApiResponse<EventResponseDTO> body = ApiResponse.success(
                                HttpStatus.OK.value(),
                                "Evento finalizado com sucesso",
                                httpRequest.getRequestURI(),
                                response);
                return ResponseEntity.ok(body);
        }
}
