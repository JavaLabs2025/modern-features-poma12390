package org.lab.infra;

import org.lab.domain.BugReport;
import org.lab.domain.BugReportId;
import org.lab.domain.DomainError;
import org.lab.domain.DomainResult;
import org.lab.domain.ProjectId;
import org.lab.domain.UserId;
import org.lab.domain.enums.BugStatus;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

public final class BugReportRepository {

    private final ConcurrentHashMap<BugReportId, BugReport> byId = new ConcurrentHashMap<>();

    public BugReportId nextId() {
        return BugReportId.newId();
    }

    public DomainResult<BugReport> insert(BugReport bug) {
        Objects.requireNonNull(bug, "bug");

        var prev = byId.putIfAbsent(bug.id(), bug);
        if (prev != null) {
            return DomainResult.err(new DomainError.Conflict("BugReport already exists: " + bug.id()));
        }
        return DomainResult.ok(bug);
    }

    public DomainResult<BugReport> upsert(BugReport bug) {
        Objects.requireNonNull(bug, "bug");
        byId.put(bug.id(), bug);
        return DomainResult.ok(bug);
    }

    public Optional<BugReport> findById(BugReportId id) {
        Objects.requireNonNull(id, "id");
        return Optional.ofNullable(byId.get(id));
    }

    public List<BugReport> findAll() {
        return byId.values().stream()
                .sorted((a, b) -> a.id().toString().compareTo(b.id().toString()))
                .collect(Collectors.toUnmodifiableList());
    }

    public List<BugReport> findByProject(ProjectId projectId) {
        Objects.requireNonNull(projectId, "projectId");
        return byId.values().stream()
                .filter(b -> b.projectId().equals(projectId))
                .sorted((a, c) -> a.id().toString().compareTo(c.id().toString()))
                .collect(Collectors.toUnmodifiableList());
    }

    public List<BugReport> findByStatus(BugStatus status) {
        Objects.requireNonNull(status, "status");
        return byId.values().stream()
                .filter(b -> b.status() == status)
                .sorted((a, c) -> a.id().toString().compareTo(c.id().toString()))
                .collect(Collectors.toUnmodifiableList());
    }

    public List<BugReport> findByAssignedTo(UserId userId) {
        Objects.requireNonNull(userId, "userId");
        return byId.values().stream()
                .filter(b -> userId.equals(b.assignedTo()))
                .sorted((a, c) -> a.id().toString().compareTo(c.id().toString()))
                .collect(Collectors.toUnmodifiableList());
    }

    public List<BugReport> findToFix(UserId userId) {
        Objects.requireNonNull(userId, "userId");
        return byId.values().stream()
                .filter(b -> b.status() == BugStatus.NEW)
                .filter(b -> userId.equals(b.assignedTo()))
                .sorted((a, c) -> a.id().toString().compareTo(c.id().toString()))
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Атомарное обновление сущности BugReport (замена record целиком).
     * Сохраняем только при Success.
     */
    public DomainResult<BugReport> update(BugReportId id, java.util.function.Function<BugReport, DomainResult<BugReport>> updater) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(updater, "updater");

        final var ref = new java.util.concurrent.atomic.AtomicReference<DomainResult<BugReport>>();
        byId.compute(id, (k, old) -> {
            if (old == null) {
                ref.set(DomainResult.err(new DomainError.NotFound("BugReport", id.toString())));
                return null;
            }
            var updatedRes = updater.apply(old);
            if (updatedRes == null) {
                ref.set(DomainResult.err(new DomainError.InvariantViolation("repo.update", "updater returned null")));
                return old;
            }
            if (updatedRes.isFailure()) {
                ref.set(updatedRes);
                return old;
            }
            var updated = updatedRes.orElseThrow();
            if (!updated.id().equals(id)) {
                ref.set(DomainResult.err(new DomainError.InvariantViolation("bugReport.idImmutable", "bug report id cannot change")));
                return old;
            }
            ref.set(DomainResult.ok(updated));
            return updated;
        });

        var res = ref.get();
        if (res == null) {
            return DomainResult.err(new DomainError.InvariantViolation("repo.update", "unexpected null result"));
        }
        return res;
    }

    public boolean delete(BugReportId id) {
        Objects.requireNonNull(id, "id");
        return byId.remove(id) != null;
    }
}
