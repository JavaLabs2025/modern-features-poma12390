package org.lab.domain;

import org.lab.domain.enums.TicketStatus;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record Ticket(
        TicketId id,
        ProjectId projectId,
        MilestoneId milestoneId,
        Title title,
        Description description,
        TicketStatus status,
        Set<UserId> assignees,
        UserId createdBy,
        Instant createdAt,
        Instant updatedAt
) {

    public Ticket { }

    public static DomainResult<Ticket> create(TicketId id,
                                              ProjectId projectId,
                                              MilestoneId milestoneId,
                                              Title title,
                                              Description description,
                                              UserId createdBy,
                                              Instant now) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(milestoneId, "milestoneId");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(createdBy, "createdBy");
        return Validation.nonNullInstant("now", now)
                .map(ts -> new Ticket(
                        id,
                        projectId,
                        milestoneId,
                        title,
                        description,
                        TicketStatus.NEW,
                        Set.of(),
                        createdBy,
                        ts,
                        ts
                ));
    }

    public Ticket assign(UserId developer, Instant now) {
        Objects.requireNonNull(developer, "developer");
        Objects.requireNonNull(now, "now");
        var next = new LinkedHashSet<>(assignees);
        next.add(developer);
        return new Ticket(id, projectId, milestoneId, title, description, status, Set.copyOf(next), createdBy, createdAt, now);
    }

    public boolean isDone() {
        return status == TicketStatus.DONE;
    }

    /**
     * Pattern matching for switch (демонстрация modern Java).
     * Переходы статусов делаем чисто доменно (RBAC будет в application, но домен держит корректные переходы).
     */
    public DomainResult<Ticket> apply(TicketAction action, Instant now) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(now, "now");

        return switch (action) {
            case TicketAction.Accept(var actor) -> accept(actor, now);
            case TicketAction.Start(var actor) -> start(actor, now);
            case TicketAction.Complete(var actor) -> complete(actor, now);
        };
    }

    private DomainResult<Ticket> accept(UserId actor, Instant now) {
        Objects.requireNonNull(actor, "actor");
        if (status != TicketStatus.NEW) {
            return DomainResult.err(new DomainError.InvalidTransition(
                    "Ticket",
                    status.name(),
                    TicketStatus.ACCEPTED.name(),
                    "accept allowed only from NEW"
            ));
        }
        if (!assignees.contains(actor)) {
            return DomainResult.err(new DomainError.InvariantViolation(
                    "ticket.assignee",
                    "actor must be assigned to accept the ticket"
            ));
        }
        return DomainResult.ok(withStatus(TicketStatus.ACCEPTED, now));
    }

    private DomainResult<Ticket> start(UserId actor, Instant now) {
        Objects.requireNonNull(actor, "actor");
        if (status != TicketStatus.ACCEPTED) {
            return DomainResult.err(new DomainError.InvalidTransition(
                    "Ticket",
                    status.name(),
                    TicketStatus.IN_PROGRESS.name(),
                    "start allowed only from ACCEPTED"
            ));
        }
        if (!assignees.contains(actor)) {
            return DomainResult.err(new DomainError.InvariantViolation(
                    "ticket.assignee",
                    "actor must be assigned to start the ticket"
            ));
        }
        return DomainResult.ok(withStatus(TicketStatus.IN_PROGRESS, now));
    }

    private DomainResult<Ticket> complete(UserId actor, Instant now) {
        Objects.requireNonNull(actor, "actor");
        if (status != TicketStatus.IN_PROGRESS) {
            return DomainResult.err(new DomainError.InvalidTransition(
                    "Ticket",
                    status.name(),
                    TicketStatus.DONE.name(),
                    "complete allowed only from IN_PROGRESS"
            ));
        }
        if (!assignees.contains(actor)) {
            return DomainResult.err(new DomainError.InvariantViolation(
                    "ticket.assignee",
                    "actor must be assigned to complete the ticket"
            ));
        }
        return DomainResult.ok(withStatus(TicketStatus.DONE, now));
    }

    private Ticket withStatus(TicketStatus next, Instant now) {
        return new Ticket(id, projectId, milestoneId, title, description, next, assignees, createdBy, createdAt, now);
    }
}
