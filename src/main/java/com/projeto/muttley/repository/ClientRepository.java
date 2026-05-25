package com.projeto.muttley.repository;

import com.projeto.muttley.entity.Client;
import com.projeto.muttley.repository.projection.ClientAggregateProjection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClientRepository extends JpaRepository<Client, UUID> {
    Optional<Client> findByCpf(String cpf);

    @Query(value = """
            select c.id as id,
                   c.nome as nome,
                   c.cpf as cpf,
                   c.email as email,
                   c.dataCriacao as dataCriacao,
                   coalesce(sum(case when ep.presencaConfirmada = true then coalesce(e.pontos, 0) else 0 end), 0) as totalPontos,
                   coalesce(sum(case when ep.presencaConfirmada = true and e.finalized = true then 1 else 0 end), 0) as totalCertificados,
                   coalesce(sum(case when ep.ganhouMedalha = true then 1 else 0 end), 0) as totalMedalhas
              from Client c
              left join c.participacoes ep
              left join ep.evento e
             where (:nome is null or lower(c.nome) like lower(concat('%', :nome, '%')))
             group by c.id, c.nome, c.cpf, c.email, c.dataCriacao
            """, countQuery = """
            select count(c)
              from Client c
             where (:nome is null or lower(c.nome) like lower(concat('%', :nome, '%')))
            """)
    Page<ClientAggregateProjection> findAllWithAggregates(@Param("nome") String nome, Pageable pageable);
}
