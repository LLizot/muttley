package com.projeto.muttley.repository;

import com.projeto.muttley.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
}
