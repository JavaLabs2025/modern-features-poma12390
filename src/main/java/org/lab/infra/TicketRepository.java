package org.lab.infra;

import org.lab.domain.DomainError;
import org.lab.domain.DomainResult;
import org.lab.domain.MilestoneId;
import org.lab.domain.ProjectId;
import org.lab.domain.Ticket;
import org.lab.domain.TicketId;
import org.lab.domain.UserId;
import org.lab.domain.enums.TicketStatus;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class TicketRepository {

    private final ConcurrentHashMap<TicketId, Ticket> byId = new ConcurrentHashMap<>();

    public TicketId nextId() {
        return TicketId.newId();
    }

    public DomainResult<Ticket> upsert(Ticket ticket) {
        Objects.requireNonNull(ticket, "ticket");
        byId.put(ticket.id(), ticket);
        return DomainResult.ok(ticket);
    }

    /**
     * Modern Java:
     * - Stream API: функциональные выборки/фильтрации по индексу в памяти.
     * - Collectors.toUnmodifiableList(): возвращает неизменяемые результаты наружу.
     */
    public List<Ticket> findByAssignee(UserId userId) {
        Objects.requireNonNull(userId, "userId");
        return byId.values().stream()
                .filter(t -> t.assignees().contains(userId))
                .sorted((a, b) -> a.id().toString().compareTo(b.id().toString()))
                .collect(Collectors.toUnmodifiableList());
    }

}
