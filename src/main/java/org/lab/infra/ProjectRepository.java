package org.lab.infra;

import org.lab.domain.DomainError;
import org.lab.domain.DomainResult;
import org.lab.domain.Project;
import org.lab.domain.ProjectId;
import org.lab.domain.UserId;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public final class ProjectRepository {

    private final ConcurrentHashMap<ProjectId, Project> byId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ProjectId> idByKey = new ConcurrentHashMap<>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final AtomicLong keySeq = new AtomicLong(0);

    public ProjectId nextId() {
        return ProjectId.newId();
    }

    /**
     * Генерация человекочитаемого ключа проекта (потокобезопасно).
     * Пример: PRJ-000001
     */
    public String nextProjectKey() {
        long n = keySeq.incrementAndGet();
        return String.format(Locale.ROOT, "PRJ-%06d", n);
    }

    public DomainResult<Project> insert(Project project) {
        Objects.requireNonNull(project, "project");

        lock.writeLock().lock();
        try {
            var key = project.key().value();
            var existingByKey = idByKey.get(key);
            if (existingByKey != null && !existingByKey.equals(project.id())) {
                return DomainResult.err(new DomainError.Conflict("Project key already exists: " + key));
            }
            if (byId.containsKey(project.id())) {
                return DomainResult.err(new DomainError.Conflict("Project already exists: " + project.id()));
            }

            idByKey.put(key, project.id());
            byId.put(project.id(), project);
            return DomainResult.ok(project);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Optional<Project> findById(ProjectId id) {
        Objects.requireNonNull(id, "id");
        return Optional.ofNullable(byId.get(id));
    }

    /**
     * Modern Java:
     * - Stream API: функциональная фильтрация проектов по участнику.
     * - Возвращает неизменяемые коллекции через Collectors.toUnmodifiableList() (гарантия отсутствия side-effects у вызывающего кода).
     */
    public List<Project> findByMember(UserId userId) {
        Objects.requireNonNull(userId, "userId");
        return byId.values().stream()
                .filter(p -> p.members().containsKey(userId))
                .sorted((a, b) -> a.key().value().compareToIgnoreCase(b.key().value()))
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Атомарное обновление aggregate root Project:
     * updater возвращает DomainResult<Project>; сохраняем только при Success.
     */
    public DomainResult<Project> update(ProjectId id, java.util.function.Function<Project, DomainResult<Project>> updater) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(updater, "updater");

        lock.writeLock().lock();
        try {
            var current = byId.get(id);
            if (current == null) {
                return DomainResult.err(new DomainError.NotFound("Project", id.toString()));
            }

            var updatedRes = updater.apply(current);
            if (updatedRes.isFailure()) {
                return updatedRes;
            }

            var updated = updatedRes.orElseThrow();
            if (!updated.id().equals(id)) {
                return DomainResult.err(new DomainError.InvariantViolation("project.idImmutable", "project id cannot change"));
            }

            if (!updated.key().value().equals(current.key().value())) {
                return DomainResult.err(new DomainError.InvariantViolation("project.keyImmutable", "project key cannot change"));
            }

            byId.put(id, updated);
            return DomainResult.ok(updated);
        } finally {
            lock.writeLock().unlock();
        }
    }

}
