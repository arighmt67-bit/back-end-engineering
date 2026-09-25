package com.arirahmat.helpdesk.service;

import com.arirahmat.helpdesk.cache.TicketReadService;
import com.arirahmat.helpdesk.dto.TicketRequest;
import com.arirahmat.helpdesk.dto.TicketResponse;
import com.arirahmat.helpdesk.entity.Role;
import com.arirahmat.helpdesk.entity.Ticket;
import com.arirahmat.helpdesk.entity.TicketPriority;
import com.arirahmat.helpdesk.entity.TicketStatus;
import com.arirahmat.helpdesk.entity.User;
import com.arirahmat.helpdesk.entity.TicketEventOutbox;
import com.arirahmat.helpdesk.event.TicketEvent;
import com.arirahmat.helpdesk.exception.ResourceNotFoundException;
import com.arirahmat.helpdesk.repository.TicketEventOutboxRepository;
import com.arirahmat.helpdesk.repository.TicketRepository;
import com.arirahmat.helpdesk.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketReadService ticketReadService;
    private final TicketEventOutboxRepository outboxRepository;

    public TicketService(
            TicketRepository ticketRepository,
            UserRepository userRepository,
            TicketReadService ticketReadService,
            TicketEventOutboxRepository outboxRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.ticketReadService = ticketReadService;
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public TicketResponse create(TicketRequest req, String actorEmail) {
        User owner = requireUser(actorEmail);
        Ticket ticket = Ticket.builder()
                .title(req.title().trim())
                .description(req.description().trim())
                .priority(req.priority() == null ? TicketPriority.MEDIUM : req.priority())
                .status(TicketStatus.OPEN)
                .owner(owner)
                .build();
        Ticket saved = ticketRepository.save(ticket);
        outboxRepository.save(TicketEventOutbox.from(TicketEvent.created(saved)));
        return TicketResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> list(TicketStatus status, String actorEmail, Pageable pageable) {
        User actor = requireUser(actorEmail);
        Page<Ticket> page = isPrivileged(actor)
                ? ticketRepository.findAllByOptionalStatus(status, pageable)
                : ticketRepository.findAllByOwnerEmail(actor.getEmail(), pageable);
        return page.map(TicketResponse::from);
    }

    @Transactional(readOnly = true)
    public TicketResponse getById(Long id, String actorEmail) {
        TicketResponse ticket = ticketReadService.getById(id);
        assertCanAccess(ticket.ownerEmail(), actorEmail);
        return ticket;
    }

    @Transactional
    @CacheEvict(cacheNames = "ticketById", key = "#id")
    public TicketResponse update(Long id, TicketRequest req, String actorEmail) {
        Ticket ticket = requireTicket(id);
        assertCanAccess(ticket, actorEmail);

        ticket.setTitle(req.title().trim());
        ticket.setDescription(req.description().trim());
        if (req.priority() != null) {
            ticket.setPriority(req.priority());
        }
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    /** Transisi status hanya boleh dilakukan AGENT/ADMIN. */
    @Transactional
    @CacheEvict(cacheNames = "ticketById", key = "#id")
    public TicketResponse changeStatus(Long id, TicketStatus newStatus, String actorEmail) {
        User actor = requireUser(actorEmail);
        if (!isPrivileged(actor)) {
            throw new AccessDeniedException("Hanya AGENT atau ADMIN yang boleh mengubah status tiket");
        }

        Ticket ticket = requireTicket(id);
        if (ticket.getStatus() == newStatus) {
            return TicketResponse.from(ticket);
        }
        ticket.setStatus(newStatus);

        boolean done = newStatus == TicketStatus.RESOLVED || newStatus == TicketStatus.CLOSED;
        ticket.setResolvedAt(done ? Instant.now() : null);

        Ticket saved = ticketRepository.save(ticket);
        outboxRepository.save(TicketEventOutbox.from(TicketEvent.statusChanged(saved)));
        return TicketResponse.from(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = "ticketById", key = "#id")
    public void delete(Long id, String actorEmail) {
        Ticket ticket = requireTicket(id);
        assertCanAccess(ticket, actorEmail);
        ticketRepository.delete(ticket);
    }

    // ---------- helper ----------

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan: " + email));
    }

    private Ticket requireTicket(Long id) {
        return ticketRepository.findByIdWithOwner(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tiket tidak ditemukan dengan id " + id));
    }

    private boolean isPrivileged(User user) {
        return user.getRole() == Role.ROLE_AGENT || user.getRole() == Role.ROLE_ADMIN;
    }

    /** Pemilik tiket boleh akses miliknya sendiri; AGENT/ADMIN boleh akses semua. */
    private void assertCanAccess(Ticket ticket, String actorEmail) {
        assertCanAccess(ticket.getOwner().getEmail(), actorEmail);
    }

    private void assertCanAccess(String ownerEmail, String actorEmail) {
        User actor = requireUser(actorEmail);
        if (isPrivileged(actor)) {
            return;
        }
        if (!ownerEmail.equals(actor.getEmail())) {
            throw new AccessDeniedException("Tiket ini bukan milik Anda");
        }
    }
}
