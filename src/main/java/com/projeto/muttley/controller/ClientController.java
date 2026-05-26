package com.projeto.muttley.controller;

import com.projeto.muttley.dto.ApiResponse;
import com.projeto.muttley.dto.ClientCreateRequestDTO;
import com.projeto.muttley.dto.ClientEventHistoryItemDTO;
import com.projeto.muttley.dto.ClientResponseDTO;
import com.projeto.muttley.service.ClientService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ClientResponseDTO>> create(
            @Valid @RequestBody ClientCreateRequestDTO request,
            HttpServletRequest httpRequest) {
        ClientResponseDTO response = clientService.create(request);
        ApiResponse<ClientResponseDTO> body = ApiResponse.success(
                HttpStatus.CREATED.value(),
                "Cliente criado com sucesso",
                httpRequest.getRequestURI(),
                response);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ClientResponseDTO>>> findAll(
            @RequestParam(required = false) String nome,
            @PageableDefault(size = 10, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable,
            HttpServletRequest httpRequest) {
        int pageIndex = Math.max(pageable.getPageNumber() - 1, 0);
        Pageable adjusted = PageRequest.of(pageIndex, pageable.getPageSize(), pageable.getSort());
        Page<ClientResponseDTO> response = clientService.findAll(nome, adjusted);
        ApiResponse<Page<ClientResponseDTO>> body = ApiResponse.success(
                HttpStatus.OK.value(),
                "Clientes listados com sucesso",
                httpRequest.getRequestURI(),
                response);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{clientId}/historico-eventos")
    public ResponseEntity<ApiResponse<Page<ClientEventHistoryItemDTO>>> listEventHistory(
            @PathVariable UUID clientId,
            @RequestParam(required = false) String titulo,
            @PageableDefault(size = 10, sort = "dataInicial", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpRequest) {
        int pageIndex = Math.max(pageable.getPageNumber() - 1, 0);
        Pageable adjusted = PageRequest.of(pageIndex, pageable.getPageSize(), pageable.getSort());
        Page<ClientEventHistoryItemDTO> response = clientService.listEventHistory(clientId, titulo, adjusted);
        ApiResponse<Page<ClientEventHistoryItemDTO>> body = ApiResponse.success(
                HttpStatus.OK.value(),
                "Historico de eventos listado com sucesso",
                httpRequest.getRequestURI(),
                response);
        return ResponseEntity.ok(body);
    }
}
