package com.arirahmat.helpdesk.repository;

import com.arirahmat.helpdesk.entity.TicketEventOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketEventOutboxRepository extends JpaRepository<TicketEventOutbox, UUID> {
    List<TicketEventOutbox> findTop100ByPublishedAtIsNullOrderByOccurredAtAsc();
}
