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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test TicketService — fokus pada aturan otorisasi (siapa boleh apa)
 * dan aturan bisnis status, bukan pada persistensi.
 */
@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TicketReadService ticketReadService;

    @Mock
    private TicketEventOutboxRepository outboxRepository;

    @InjectMocks
    private TicketService ticketService;

    // ---------- helper ----------

    private User user(String email, Role role) {
        return User.builder().id(1L).email(email).fullName("Nama " + email)
                .passwordHash("hash").role(role).build();
    }

    private Ticket ticket(Long id, User owner, TicketStatus status) {
        return Ticket.builder()
                .id(id).title("Printer error").description("Tidak bisa mencetak")
                .status(status).priority(TicketPriority.MEDIUM).owner(owner)
                .build();
    }

    private void mockUser(User u) {
        when(userRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
    }

    // ---------- create ----------

    @Test
    @DisplayName("Tiket baru selalu berstatus OPEN dan dimiliki pembuatnya")
    void create_menetapkan_status_open_dan_owner() {
        User owner = user("user@example.com", Role.ROLE_USER);
        mockUser(owner);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        ticketService.create(new TicketRequest("  Printer error  ", "  Tidak bisa mencetak  ",
                TicketPriority.HIGH), owner.getEmail());

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        Ticket saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(saved.getPriority()).isEqualTo(TicketPriority.HIGH);
        assertThat(saved.getOwner().getEmail()).isEqualTo("user@example.com");
        assertThat(saved.getTitle()).isEqualTo("Printer error");
        assertThat(saved.getDescription()).isEqualTo("Tidak bisa mencetak");
    }

    @Test
    @DisplayName("Create sukses menyimpan ticket.created.v1 ke transactional outbox")
    void create_sukses_menyimpan_event_ke_outbox() {
        User owner = user("user@example.com", Role.ROLE_USER);
        mockUser(owner);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        ticketService.create(new TicketRequest("Judul", "Deskripsi", TicketPriority.HIGH),
                owner.getEmail());

        ArgumentCaptor<TicketEventOutbox> outbox = ArgumentCaptor.forClass(TicketEventOutbox.class);
        verify(outboxRepository).save(outbox.capture());
        TicketEvent event = outbox.getValue().toEvent();
        assertThat(event.eventType()).isEqualTo(TicketEvent.CREATED);
        assertThat(event.eventVersion()).isEqualTo(1);
        assertThat(event.ticketId()).isEqualTo(42L);
        assertThat(event.ownerEmail()).isEqualTo(owner.getEmail());
        assertThat(event.status()).isEqualTo(TicketStatus.OPEN);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    @DisplayName("Create gagal menyimpan tidak boleh menerbitkan event")
    void create_gagal_save_tidak_menerbitkan_event() {
        User owner = user("user@example.com", Role.ROLE_USER);
        mockUser(owner);
        when(ticketRepository.save(any(Ticket.class)))
                .thenThrow(new IllegalStateException("database gagal"));

        assertThatThrownBy(() -> ticketService.create(
                new TicketRequest("Judul", "Deskripsi", null), owner.getEmail()))
                .isInstanceOf(IllegalStateException.class);

        verify(outboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("Priority null pada request di-default ke MEDIUM")
    void create_priority_null_default_medium() {
        User owner = user("user@example.com", Role.ROLE_USER);
        mockUser(owner);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        ticketService.create(new TicketRequest("Judul", "Deskripsi", null), owner.getEmail());

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        assertThat(captor.getValue().getPriority()).isEqualTo(TicketPriority.MEDIUM);
    }

    @Test
    @DisplayName("Membuat tiket dengan email aktor yang tidak terdaftar melempar ResourceNotFound")
    void create_user_tidak_ditemukan() {
        when(userRepository.findByEmail("hantu@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.create(
                new TicketRequest("Judul", "Deskripsi", null), "hantu@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(ticketRepository, never()).save(any());
    }

    // ---------- otorisasi baca ----------

    @Test
    @DisplayName("Pemilik boleh membaca tiketnya sendiri")
    void getById_pemilik_boleh_akses() {
        User owner = user("user@example.com", Role.ROLE_USER);
        mockUser(owner);
        when(ticketReadService.getById(10L))
                .thenReturn(TicketResponse.from(ticket(10L, owner, TicketStatus.OPEN)));

        TicketResponse res = ticketService.getById(10L, owner.getEmail());

        assertThat(res.id()).isEqualTo(10L);
        assertThat(res.ownerEmail()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("User biasa TIDAK boleh membaca tiket milik orang lain")
    void getById_user_lain_ditolak() {
        User owner = user("owner@example.com", Role.ROLE_USER);
        User penyusup = user("penyusup@example.com", Role.ROLE_USER);
        mockUser(penyusup);
        when(ticketReadService.getById(10L))
                .thenReturn(TicketResponse.from(ticket(10L, owner, TicketStatus.OPEN)));

        assertThatThrownBy(() -> ticketService.getById(10L, penyusup.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("AGENT boleh membaca tiket milik siapa pun")
    void getById_agent_boleh_akses_semua() {
        User owner = user("owner@example.com", Role.ROLE_USER);
        User agent = user("agent@example.com", Role.ROLE_AGENT);
        mockUser(agent);
        when(ticketReadService.getById(10L))
                .thenReturn(TicketResponse.from(ticket(10L, owner, TicketStatus.OPEN)));

        assertThat(ticketService.getById(10L, agent.getEmail()).id()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Setiap request detail tetap mengotorisasi aktor meskipun response dapat berasal dari cache")
    void getById_selalu_mengotorisasi_setiap_request() {
        User owner = user("user@example.com", Role.ROLE_USER);
        mockUser(owner);
        when(ticketReadService.getById(10L))
                .thenReturn(TicketResponse.from(ticket(10L, owner, TicketStatus.OPEN)));

        ticketService.getById(10L, owner.getEmail());
        ticketService.getById(10L, owner.getEmail());

        verify(userRepository, org.mockito.Mockito.times(2)).findByEmail(owner.getEmail());
        verify(ticketReadService, org.mockito.Mockito.times(2)).getById(10L);
    }

    @Test
    @DisplayName("Tiket yang tidak ada melempar ResourceNotFoundException")
    void getById_tiket_tidak_ada() {
        when(ticketReadService.getById(99L))
                .thenThrow(new ResourceNotFoundException("Tiket tidak ditemukan dengan id 99"));

        assertThatThrownBy(() -> ticketService.getById(99L, "user@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ---------- transisi status ----------

    @Test
    @DisplayName("User biasa tidak boleh mengubah status tiket, bahkan miliknya sendiri")
    void changeStatus_user_biasa_ditolak() {
        User owner = user("user@example.com", Role.ROLE_USER);
        mockUser(owner);

        assertThatThrownBy(() ->
                ticketService.changeStatus(10L, TicketStatus.RESOLVED, owner.getEmail()))
                .isInstanceOf(AccessDeniedException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("AGENT mengubah status ke RESOLVED dan resolvedAt otomatis terisi")
    void changeStatus_resolved_mengisi_resolvedAt() {
        User owner = user("owner@example.com", Role.ROLE_USER);
        User agent = user("agent@example.com", Role.ROLE_AGENT);
        mockUser(agent);
        Ticket t = ticket(10L, owner, TicketStatus.IN_PROGRESS);
        when(ticketRepository.findByIdWithOwner(10L)).thenReturn(Optional.of(t));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketResponse res = ticketService.changeStatus(10L, TicketStatus.RESOLVED, agent.getEmail());

        assertThat(res.status()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(res.resolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("Perubahan status sukses menyimpan status terbaru ke outbox")
    void change_status_sukses_menyimpan_event_terbaru_ke_outbox() {
        User owner = user("owner@example.com", Role.ROLE_USER);
        User agent = user("agent@example.com", Role.ROLE_AGENT);
        mockUser(agent);
        Ticket ticket = ticket(10L, owner, TicketStatus.IN_PROGRESS);
        when(ticketRepository.findByIdWithOwner(10L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ticketService.changeStatus(10L, TicketStatus.RESOLVED, agent.getEmail());

        ArgumentCaptor<TicketEventOutbox> outbox = ArgumentCaptor.forClass(TicketEventOutbox.class);
        verify(outboxRepository).save(outbox.capture());
        TicketEvent event = outbox.getValue().toEvent();
        assertThat(event.eventType()).isEqualTo(TicketEvent.STATUS_CHANGED);
        assertThat(event.ticketId()).isEqualTo(10L);
        assertThat(event.status()).isEqualTo(TicketStatus.RESOLVED);
    }

    @Test
    @DisplayName("Status yang sama tidak menyimpan ulang atau menerbitkan event palsu")
    void changeStatus_status_sama_adalah_noop() {
        User owner = user("owner@example.com", Role.ROLE_USER);
        User agent = user("agent@example.com", Role.ROLE_AGENT);
        mockUser(agent);
        Ticket ticket = ticket(10L, owner, TicketStatus.RESOLVED);
        java.time.Instant resolvedAt = java.time.Instant.parse("2026-09-24T13:00:00Z");
        ticket.setResolvedAt(resolvedAt);
        when(ticketRepository.findByIdWithOwner(10L)).thenReturn(Optional.of(ticket));

        TicketResponse response = ticketService.changeStatus(
                10L, TicketStatus.RESOLVED, agent.getEmail());

        assertThat(response.status()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(response.resolvedAt()).isEqualTo(resolvedAt);
        verify(ticketRepository, never()).save(any());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("Status dikembalikan ke IN_PROGRESS membuat resolvedAt kosong lagi")
    void changeStatus_buka_kembali_mengosongkan_resolvedAt() {
        User owner = user("owner@example.com", Role.ROLE_USER);
        User admin = user("admin@example.com", Role.ROLE_ADMIN);
        mockUser(admin);
        Ticket t = ticket(10L, owner, TicketStatus.RESOLVED);
        t.setResolvedAt(java.time.Instant.now());
        when(ticketRepository.findByIdWithOwner(10L)).thenReturn(Optional.of(t));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketResponse res = ticketService.changeStatus(10L, TicketStatus.IN_PROGRESS, admin.getEmail());

        assertThat(res.status()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(res.resolvedAt()).isNull();
    }

    // ---------- delete ----------

    @Test
    @DisplayName("User lain tidak boleh menghapus tiket milik orang")
    void delete_user_lain_ditolak() {
        User owner = user("owner@example.com", Role.ROLE_USER);
        User penyusup = user("penyusup@example.com", Role.ROLE_USER);
        mockUser(penyusup);
        when(ticketRepository.findByIdWithOwner(10L))
                .thenReturn(Optional.of(ticket(10L, owner, TicketStatus.OPEN)));

        assertThatThrownBy(() -> ticketService.delete(10L, penyusup.getEmail()))
                .isInstanceOf(AccessDeniedException.class);

        verify(ticketRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Pemilik boleh menghapus tiketnya sendiri")
    void delete_oleh_pemilik_berhasil() {
        User owner = user("owner@example.com", Role.ROLE_USER);
        mockUser(owner);
        Ticket t = ticket(10L, owner, TicketStatus.OPEN);
        when(ticketRepository.findByIdWithOwner(10L)).thenReturn(Optional.of(t));

        ticketService.delete(10L, owner.getEmail());

        verify(ticketRepository).delete(t);
    }
}
