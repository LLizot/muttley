package com.projeto.muttley.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponseDTO {

    private Long id;
    private String nome;
    private List<ClientResponseDTO> clientes;
}
