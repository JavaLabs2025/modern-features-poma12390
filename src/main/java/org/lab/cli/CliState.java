package org.lab.cli;

import org.lab.domain.*;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CliState {

    private final ConcurrentHashMap<String, UserId> userByLogin = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ProjectId> projectByKey = new ConcurrentHashMap<>();

    private volatile ProjectId lastProjectId;
    private volatile String lastProjectKey;

    private final ConcurrentHashMap<ProjectId, MilestoneId> lastMilestoneByProject = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ProjectId, TicketId> lastTicketByProject = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ProjectId, BugReportId> lastBugByProject = new ConcurrentHashMap<>();

    public void rememberUser(String login, UserId id) {
        Objects.requireNonNull(login, "login");
        Objects.requireNonNull(id, "id");
        userByLogin.put(login, id);
    }

    public Optional<UserId> userId(String login) {
        Objects.requireNonNull(login, "login");
        return Optional.ofNullable(userByLogin.get(login));
    }

    public void rememberProject(ProjectId id, String key) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(key, "key");
        projectByKey.put(key, id);
        lastProjectId = id;
        lastProjectKey = key;
    }

    public Optional<ProjectId> projectIdByKey(String key) {
        Objects.requireNonNull(key, "key");
        return Optional.ofNullable(projectByKey.get(key));
    }

    public Optional<ProjectId> lastProjectId() {
        return Optional.ofNullable(lastProjectId);
    }

    public Optional<String> lastProjectKey() {
        return Optional.ofNullable(lastProjectKey);
    }

    public void rememberMilestone(ProjectId projectId, MilestoneId milestoneId) {
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(milestoneId, "milestoneId");
        lastMilestoneByProject.put(projectId, milestoneId);
    }

    public Optional<MilestoneId> lastMilestone(ProjectId projectId) {
        Objects.requireNonNull(projectId, "projectId");
        return Optional.ofNullable(lastMilestoneByProject.get(projectId));
    }

    public void rememberTicket(ProjectId projectId, TicketId ticketId) {
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(ticketId, "ticketId");
        lastTicketByProject.put(projectId, ticketId);
    }

    public Optional<TicketId> lastTicket(ProjectId projectId) {
        Objects.requireNonNull(projectId, "projectId");
        return Optional.ofNullable(lastTicketByProject.get(projectId));
    }

    public void rememberBug(ProjectId projectId, BugReportId bugId) {
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(bugId, "bugId");
        lastBugByProject.put(projectId, bugId);
    }

    public Optional<BugReportId> lastBug(ProjectId projectId) {
        Objects.requireNonNull(projectId, "projectId");
        return Optional.ofNullable(lastBugByProject.get(projectId));
    }

    /**
     * Modern Java:
     * - Стандартная библиотека UUID: строгая валидация/парсинг идентификаторов для ссылок вида UUID в CLI.
     */
    public static boolean looksLikeUuid(String s) {
        try {
            UUID.fromString(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static UUID parseUuidStrict(String s, String fieldName) {
        Objects.requireNonNull(s, fieldName);
        try {
            return UUID.fromString(s);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid UUID for " + fieldName + ": " + s);
        }
    }
}
