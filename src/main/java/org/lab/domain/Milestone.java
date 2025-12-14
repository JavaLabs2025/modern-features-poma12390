package org.lab.domain;

import org.lab.domain.enums.MilestoneStatus;

import java.time.Instant;
import java.util.Objects;

public record Milestone(
        MilestoneId id,
        ProjectId projectId,
        String name,
        DateRange range,
        MilestoneStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public Milestone { }

    public static DomainResult<Milestone> create(MilestoneId id,
                                                 ProjectId projectId,
                                                 String name,
                                                 DateRange range,
                                                 Instant now) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(range, "range");
        return Validation.nonBlank("milestoneName", name)
                .flatMap(n -> Validation.maxLen("milestoneName", n, 200))
                .flatMap(n ->
                        Validation.nonNullInstant("now", now)
                                .map(ts -> new Milestone(id, projectId, n, range, MilestoneStatus.OPEN, ts, ts))
                );
    }

    Milestone withStatus(MilestoneStatus next, Instant now) {
        Objects.requireNonNull(next, "next");
        Objects.requireNonNull(now, "now");
        return new Milestone(id, projectId, name, range, next, createdAt, now);
    }
}
