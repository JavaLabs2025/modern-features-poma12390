package org.lab.domain;

import org.lab.domain.enums.BugStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record BugReport(
        BugReportId id,
        ProjectId projectId,
        Title title,
        Description description,
        BugStatus status,
        UserId createdBy,
        UserId assignedTo,
        UserId fixedBy,
        UserId testedBy,
        Instant createdAt,
        Instant updatedAt
) {

    public BugReport { }

    public static DomainResult<BugReport> create(BugReportId id,
                                                 ProjectId projectId,
                                                 Title title,
                                                 Description description,
                                                 UserId createdBy,
                                                 Instant now) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(createdBy, "createdBy");
        return Validation.nonNullInstant("now", now)
                .map(ts -> new BugReport(
                        id,
                        projectId,
                        title,
                        description,
                        BugStatus.NEW,
                        createdBy,
                        null,
                        null,
                        null,
                        ts,
                        ts
                ));
    }

    public BugReport assignTo(UserId developer, Instant now) {
        Objects.requireNonNull(developer, "developer");
        Objects.requireNonNull(now, "now");
        return new BugReport(id, projectId, title, description, status, createdBy, developer, fixedBy, testedBy, createdAt, now);
    }

    public Optional<UserId> assignedToOpt() {
        return Optional.ofNullable(assignedTo);
    }
    /**
     * Modern Java:
     * - Pattern matching for switch: switch по sealed FailureCause с record-pattern’ами
     *   (case FailureCause.Domain(var error) -> ..., case AccessDenied denied -> ...).
     * - Text Blocks ("""...""") + formatted(): человекочитаемые блоки ошибок домена и ошибок доступа.
     */
    public DomainResult<BugReport> apply(BugReportAction action, Instant now) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(now, "now");

        return switch (action) {
            case BugReportAction.Fix(var actor) -> fix(actor, now);
            case BugReportAction.Test(var actor) -> test(actor, now);
            case BugReportAction.Close(var actor) -> close(actor, now);
        };
    }

    private DomainResult<BugReport> fix(UserId actor, Instant now) {
        Objects.requireNonNull(actor, "actor");
        if (status != BugStatus.NEW) {
            return DomainResult.err(new DomainError.InvalidTransition(
                    "BugReport",
                    status.name(),
                    BugStatus.FIXED.name(),
                    "fix allowed only from NEW"
            ));
        }
        // Если assignedTo не задан — допускаем "взял и исправил", но фиксируем назначение.
        var effectiveAssigned = assignedTo != null ? assignedTo : actor;
        if (!effectiveAssigned.equals(actor)) {
            return DomainResult.err(new DomainError.InvariantViolation(
                    "bug.assignee",
                    "only assigned developer can fix the bug"
            ));
        }
        return DomainResult.ok(new BugReport(
                id, projectId, title, description,
                BugStatus.FIXED,
                createdBy,
                effectiveAssigned,
                actor,
                testedBy,
                createdAt,
                now
        ));
    }

    private DomainResult<BugReport> test(UserId actor, Instant now) {
        Objects.requireNonNull(actor, "actor");
        if (status != BugStatus.FIXED) {
            return DomainResult.err(new DomainError.InvalidTransition(
                    "BugReport",
                    status.name(),
                    BugStatus.TESTED.name(),
                    "test allowed only from FIXED"
            ));
        }
        return DomainResult.ok(new BugReport(
                id, projectId, title, description,
                BugStatus.TESTED,
                createdBy,
                assignedTo,
                fixedBy,
                actor,
                createdAt,
                now
        ));
    }

    private DomainResult<BugReport> close(UserId actor, Instant now) {
        Objects.requireNonNull(actor, "actor");
        if (status != BugStatus.TESTED) {
            return DomainResult.err(new DomainError.InvalidTransition(
                    "BugReport",
                    status.name(),
                    BugStatus.CLOSED.name(),
                    "close allowed only from TESTED"
            ));
        }
        return DomainResult.ok(new BugReport(
                id, projectId, title, description,
                BugStatus.CLOSED,
                createdBy,
                assignedTo,
                fixedBy,
                testedBy,
                createdAt,
                now
        ));
    }
}
