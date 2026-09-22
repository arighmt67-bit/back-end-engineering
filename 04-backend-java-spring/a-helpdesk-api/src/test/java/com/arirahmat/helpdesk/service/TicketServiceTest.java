package com.arirahmat.helpdesk.service;

import com.arirahmat.helpdesk.dto.TicketRequest;
import com.arirahmat.helpdesk.dto.TicketResponse;
import com.arirahmat.helpdesk.entity.Role;
import com.arirahmat.helpdesk.entity.Ticket;
import com.arirahmat.helpdesk.entity.TicketPriority;
import com.arirahmat.helpdesk.entity.TicketStatus;
import com.arirahmat.helpdesk.entity.User;
import com.arirahmat.helpdesk.exception.ResourceNotFoundException;
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
        when(ticketRepository.findByIdWithOwner(10L))
                .thenReturn(Optional.of(ticket(10L, owner, TicketStatus.OPEN)));

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
        when(ticketRepository.findByIdWithOwner(10L))
                .thenReturn(Optional.of(ticket(10L, owner, TicketStatus.OPEN)));

        assertThatThrownBy(() -> ticketService.getById(10L, penyusup.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("AGENT boleh membaca tiket milik siapa pun")
    void getById_agent_boleh_akses_semua() {
        User owner = user("owner@example.com", Role.ROLE_USER);
        User agent = user("agent@example.com", Role.ROLE_AGENT);
        mockUser(agent);
        when(ticketRepository.findByIdWithOwner(10L))
                .thenReturn(Optional.of(ticket(10L, owner, TicketStatus.OPEN)));

        assertThat(ticketService.getById(10L, agent.getEmail()).id()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Tiket yang tidak ada melempar ResourceNotFoundException")
    void getById_tiket_tidak_ada() {
        when(ticketRepository.findByIdWithOwner(99L)).thenReturn(Optional.empty());

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
