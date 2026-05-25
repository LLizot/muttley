package com.projeto.muttley.repository;

import com.projeto.muttley.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, UUID> {
    Page<Event> findByTituloContainingIgnoreCase(String titulo, Pageable pageable);
}
