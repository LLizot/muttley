package com.projeto.muttley.dto;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedalhaEmLoteForm {

    private UUID eventoId;
    private List<UUID> participanteIds;
    private String descricaoMedalha;
    private String competenciasMedalha;
    private MultipartFile arquivoPlanoDeFundo;
}
