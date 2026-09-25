package com.arirahmat.helpdesk.repository;

import com.arirahmat.helpdesk.dto.report.AgentWorkloadDto;
import com.arirahmat.helpdesk.dto.report.MonthlyCountDto;
import com.arirahmat.helpdesk.dto.report.PriorityCountDto;
import com.arirahmat.helpdesk.dto.report.StatusCountDto;
import com.arirahmat.helpdesk.entity.Ticket;
import com.arirahmat.helpdesk.entity.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query("SELECT t FROM Ticket t JOIN FETCH t.owner WHERE t.id = :id")
    Optional<Ticket> findByIdWithOwner(@Param("id") Long id);

    @Query("SELECT t.updatedAt FROM Ticket t WHERE t.id = :id")
    Optional<Instant> findUpdatedAtById(@Param("id") Long id);

    @Query(value = "SELECT t FROM Ticket t JOIN FETCH t.owner o WHERE o.email = :email",
           countQuery = "SELECT COUNT(t) FROM Ticket t WHERE t.owner.email = :email")
    Page<Ticket> findAllByOwnerEmail(@Param("email") String email, Pageable pageable);

    @Query(value = "SELECT t FROM Ticket t JOIN FETCH t.owner WHERE (:status IS NULL OR t.status = :status)",
           countQuery = "SELECT COUNT(t) FROM Ticket t WHERE (:status IS NULL OR t.status = :status)")
    Page<Ticket> findAllByOptionalStatus(@Param("status") TicketStatus status, Pageable pageable);

    long countByStatus(TicketStatus status);

    // ---------- QUERY AGREGASI UNTUK REPORT ----------

    @Query("""
            SELECT new com.arirahmat.helpdesk.dto.report.StatusCountDto(t.status, COUNT(t))
            FROM Ticket t GROUP BY t.status ORDER BY COUNT(t) DESC
            """)
    List<StatusCountDto> countGroupByStatus();

    @Query("""
            SELECT new com.arirahmat.helpdesk.dto.report.PriorityCountDto(t.priority, COUNT(t))
            FROM Ticket t GROUP BY t.priority ORDER BY COUNT(t) DESC
            """)
    List<PriorityCountDto> countGroupByPriority();

    @Query("""
            SELECT new com.arirahmat.helpdesk.dto.report.MonthlyCountDto(
                   YEAR(t.createdAt), MONTH(t.createdAt), COUNT(t))
            FROM Ticket t
            WHERE YEAR(t.createdAt) = :year
            GROUP BY YEAR(t.createdAt), MONTH(t.createdAt)
            ORDER BY MONTH(t.createdAt)
            """)
    List<MonthlyCountDto> countMonthlyByYear(@Param("year") int year);

    @Query("""
            SELECT new com.arirahmat.helpdesk.dto.report.AgentWorkloadDto(
                   o.email, o.fullName, COUNT(t),
                   SUM(CASE WHEN t.status = com.arirahmat.helpdesk.entity.TicketStatus.OPEN THEN 1L ELSE 0L END))
            FROM Ticket t JOIN t.owner o
            GROUP BY o.email, o.fullName
            ORDER BY COUNT(t) DESC
            """)
    List<AgentWorkloadDto> findWorkloadPerUser();

    /** Rata-rata jam penyelesaian, dihitung di Java agar portable lintas H2/PostgreSQL. */
    @Query("SELECT t.createdAt, t.resolvedAt FROM Ticket t WHERE t.resolvedAt IS NOT NULL")
    List<Object[]> findResolutionTimestamps();
}
