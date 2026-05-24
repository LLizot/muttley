package com.projeto.muttley.controller;

import com.projeto.muttley.dto.ApiResponse;
import com.projeto.muttley.dto.EventRequestDTO;
import com.projeto.muttley.dto.EventResponseDTO;
import com.projeto.muttley.service.EventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EventResponseDTO>> create(
            @Valid @RequestBody EventRequestDTO request,
            HttpServletRequest httpRequest) {
        EventResponseDTO response = eventService.create(request);
        ApiResponse<EventResponseDTO> body = ApiResponse.success(
                HttpStatus.CREATED.value(),
                "Evento criado com sucesso",
                httpRequest.getRequestURI(),
                response);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EventResponseDTO>>> findAll(HttpServletRequest httpRequest) {
        List<EventResponseDTO> response = eventService.findAll();
        ApiResponse<List<EventResponseDTO>> body = ApiResponse.success(
                HttpStatus.OK.value(),
                "Eventos listados com sucesso",
                httpRequest.getRequestURI(),
                response);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponseDTO>> findById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        EventResponseDTO response = eventService.findById(id);
        ApiResponse<EventResponseDTO> body = ApiResponse.success(
                HttpStatus.OK.value(),
                "Evento encontrado com sucesso",
                httpRequest.getRequestURI(),
                response);
        return ResponseEntity.ok(body);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody EventRequestDTO request,
            HttpServletRequest httpRequest) {
        EventResponseDTO response = eventService.update(id, request);
        ApiResponse<EventResponseDTO> body = ApiResponse.success(
                HttpStatus.OK.value(),
                "Evento atualizado com sucesso",
                httpRequest.getRequestURI(),
                response);
        return ResponseEntity.ok(body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        eventService.delete(id);
        ApiResponse<Object> body = ApiResponse.success(
                HttpStatus.OK.value(),
                "Evento removido com sucesso",
                httpRequest.getRequestURI(),
                null);
        return ResponseEntity.ok(body);
    }
}
