package com.arirahmat.helpdesk.dto.report;

public record AgentWorkloadDto(String email, String fullName, long totalTickets, long openTickets) {
}
