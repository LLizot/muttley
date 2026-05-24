package com.projeto.muttley.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestDTO {

    @NotBlank
    @Size(min = 2, max = 120)
    private String nome;

    private List<Long> clientIds;
}
