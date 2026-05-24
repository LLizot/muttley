package com.projeto.muttley.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientResponseDTO {

    private Long id;
    private String nome;
    private String cpf;
}
