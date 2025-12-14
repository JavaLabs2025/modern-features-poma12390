package org.lab.domain;

import org.lab.domain.enums.BugStatus;
import org.lab.domain.enums.MilestoneStatus;
import org.lab.domain.enums.ProjectRole;
import org.lab.domain.enums.TicketStatus;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record Project(
        ProjectId id,
        ProjectKey key,
        String name,
        Description description,
        UserId managerId,
        UserId teamLeadId,
        Map<UserId, ProjectRole> members,
        Map<MilestoneId, Milestone> milestones,
        Map<TicketId, Ticket> tickets,
        Map<BugReportId, BugReport> bugReports,
        Instant createdAt,
        Instant updatedAt
) {

    public Project {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(managerId, "managerId");
        Objects.requireNonNull(members, "members");
        Objects.requireNonNull(milestones, "milestones");
        Objects.requireNonNull(tickets, "tickets");
        Objects.requireNonNull(bugReports, "bugReports");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");

        members = Map.copyOf(members);
        milestones = Map.copyOf(milestones);
        tickets = Map.copyOf(tickets);
        bugReports = Map.copyOf(bugReports);

        var mgrRole = members.get(managerId);
        if (mgrRole != ProjectRole.MANAGER) {
            throw new IllegalStateException("Project invariant broken: managerId must have MANAGER role");
        }

        if (teamLeadId != null) {
            var tlRole = members.get(teamLeadId);
            if (tlRole != ProjectRole.TEAM_LEAD) {
                throw new IllegalStateException("Project invariant broken: teamLeadId must have TEAM_LEAD role");
            }
        }

        long activeCount = milestones.values().stream().filter(m -> m.status() == MilestoneStatus.ACTIVE).count();
        if (activeCount > 1) {
            throw new IllegalStateException("Project invariant broken: only one ACTIVE milestone allowed");
        }
    }

    public static DomainResult<Project> create(ProjectId id,
                                               String rawKey,
                                               String rawName,
                                               String rawDescription,
                                               UserId managerId,
                                               Instant now) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(managerId, "managerId");

        return ProjectKey.of(rawKey)
                .flatMap(k -> Validation.nonBlank("projectName", rawName)
                        .flatMap(n -> Validation.maxLen("projectName", n, 200))
                        .flatMap(n -> {
                            var desc = rawDescription == null ? "" : rawDescription.trim();
                            if (desc.length() > 4000) {
                                return DomainResult.err(new DomainError.InvalidValue("projectDescription", "length must be <= 4000"));
                            }
                            return Validation.nonNullInstant("now", now).map(ts -> {
                                var members = new HashMap<UserId, ProjectRole>();
                                members.put(managerId, ProjectRole.MANAGER);
                                return new Project(
                                        id, k, n, new Description(desc),
                                        managerId, null,
                                        members,
                                        Map.of(),
                                        Map.of(),
                                        Map.of(),
                                        ts, ts
                                );
                            });
                        })
                );
    }

    public Optional<UserId> teamLeadIdOpt() {
        return Optional.ofNullable(teamLeadId);
    }

    public Optional<ProjectRole> roleOf(UserId userId) {
        return Optional.ofNullable(members.get(userId));
    }

    public boolean isMember(UserId userId) {
        return members.containsKey(userId);
    }

    public DomainResult<Project> addDeveloper(UserId userId, Instant now) {
        return addMember(userId, ProjectRole.DEVELOPER, now);
    }

    public DomainResult<Project> addTester(UserId userId, Instant now) {
        return addMember(userId, ProjectRole.TESTER, now);
    }

    public DomainResult<Project> assignTeamLead(UserId userId, Instant now) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(now, "now");

        var current = members.get(userId);
        if (current == ProjectRole.TESTER) {
            return DomainResult.err(new DomainError.Conflict("Cannot promote TESTER to TEAM_LEAD in this simplified model"));
        }

        var nextMembers = new HashMap<>(members);
        nextMembers.put(userId, ProjectRole.TEAM_LEAD);

        return DomainResult.ok(new Project(
                id, key, name, description,
                managerId, userId,
                nextMembers,
                milestones,
                tickets,
                bugReports,
                createdAt,
                now
        ));
    }

    private DomainResult<Project> addMember(UserId userId, ProjectRole role, Instant now) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(now, "now");

        var existing = members.get(userId);
        if (existing != null) {
            if (existing == role) {
                return DomainResult.ok(this); // идемпотентность
            }
            return DomainResult.err(new DomainError.Conflict(
                    "User already has role " + existing + " in project; simplified model allows only one role"
            ));
        }

        var next = new HashMap<>(members);
        next.put(userId, role);

        return DomainResult.ok(new Project(
                id, key, name, description,
                managerId, teamLeadId,
                next,
                milestones,
                tickets,
                bugReports,
                createdAt,
                now
        ));
    }

    // ---------- Milestones ----------

    public DomainResult<Project> createMilestone(MilestoneId milestoneId,
                                                 String milestoneName,
                                                 DateRange range,
                                                 Instant now) {
        Objects.requireNonNull(milestoneId, "milestoneId");
        Objects.requireNonNull(range, "range");
        Objects.requireNonNull(now, "now");

        if (milestones.containsKey(milestoneId)) {
            return DomainResult.err(new DomainError.Conflict("Milestone already exists: " + milestoneId));
        }

        return Milestone.create(milestoneId, id, milestoneName, range, now)
                .map(ms -> {
                    var next = new HashMap<>(milestones);
                    next.put(milestoneId, ms);
                    return new Project(
                            id, key, name, description,
                            managerId, teamLeadId,
                            members,
                            next,
                            tickets,
                            bugReports,
                            createdAt,
                            now
                    );
                });
    }

    public DomainResult<Project> activateMilestone(MilestoneId milestoneId, Instant now) {
        Objects.requireNonNull(milestoneId, "milestoneId");
        Objects.requireNonNull(now, "now");

        var ms = milestones.get(milestoneId);
        if (ms == null) {
            return DomainResult.err(new DomainError.NotFound("Milestone", milestoneId.toString()));
        }
        if (ms.status() == MilestoneStatus.CLOSED) {
            return DomainResult.err(new DomainError.InvalidTransition(
                    "Milestone",
                    MilestoneStatus.CLOSED.name(),
                    MilestoneStatus.ACTIVE.name(),
                    "cannot activate closed milestone"
            ));
        }
        if (ms.status() == MilestoneStatus.ACTIVE) {
            return DomainResult.ok(this); // идемпотентность
        }

        boolean hasAnotherActive = milestones.values().stream()
                .anyMatch(m -> m.status() == MilestoneStatus.ACTIVE && !m.id().equals(milestoneId));
        if (hasAnotherActive) {
            return DomainResult.err(new DomainError.InvariantViolation(
                    "project.singleActiveMilestone",
                    "another ACTIVE milestone already exists"
            ));
        }

        var next = new HashMap<>(milestones);
        next.put(milestoneId, ms.withStatus(MilestoneStatus.ACTIVE, now));

        return DomainResult.ok(new Project(
                id, key, name, description,
                managerId, teamLeadId,
                members,
                next,
                tickets,
                bugReports,
                createdAt,
                now
        ));
    }

    public DomainResult<Project> closeMilestone(MilestoneId milestoneId, Instant now) {
        Objects.requireNonNull(milestoneId, "milestoneId");
        Objects.requireNonNull(now, "now");

        var ms = milestones.get(milestoneId);
        if (ms == null) {
            return DomainResult.err(new DomainError.NotFound("Milestone", milestoneId.toString()));
        }
        if (ms.status() == MilestoneStatus.CLOSED) {
            return DomainResult.ok(this); // идемпотентность
        }
        if (ms.status() != MilestoneStatus.ACTIVE) {
            return DomainResult.err(new DomainError.InvalidTransition(
                    "Milestone",
                    ms.status().name(),
                    MilestoneStatus.CLOSED.name(),
                    "can close only ACTIVE milestone"
            ));
        }

        boolean allDone = tickets.values().stream()
                .filter(t -> t.milestoneId().equals(milestoneId))
                .allMatch(Ticket::isDone);

        if (!allDone) {
            return DomainResult.err(new DomainError.InvariantViolation(
                    "milestone.closeRequiresAllTicketsDone",
                    "cannot close milestone while it has not DONE tickets"
            ));
        }

        var next = new HashMap<>(milestones);
        next.put(milestoneId, ms.withStatus(MilestoneStatus.CLOSED, now));

        return DomainResult.ok(new Project(
                id, key, name, description,
                managerId, teamLeadId,
                members,
                next,
                tickets,
                bugReports,
                createdAt,
                now
        ));
    }

    // ---------- Tickets ----------

    public DomainResult<Project> createTicket(TicketId ticketId,
                                              MilestoneId milestoneId,
                                              Title title,
                                              Description description,
                                              UserId createdBy,
                                              Instant now) {
        Objects.requireNonNull(ticketId, "ticketId");
        Objects.requireNonNull(milestoneId, "milestoneId");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(createdBy, "createdBy");
        Objects.requireNonNull(now, "now");

        if (tickets.containsKey(ticketId)) {
            return DomainResult.err(new DomainError.Conflict("Ticket already exists: " + ticketId));
        }

        var ms = milestones.get(milestoneId);
        if (ms == null) {
            return DomainResult.err(new DomainError.NotFound("Milestone", milestoneId.toString()));
        }
        if (ms.status() == MilestoneStatus.CLOSED) {
            return DomainResult.err(new DomainError.InvariantViolation(
                    "ticket.milestoneNotClosed",
                    "cannot create ticket in CLOSED milestone"
            ));
        }

        if (!isMember(createdBy)) {
            return DomainResult.err(new DomainError.InvariantViolation(
                    "ticket.creatorMustBeMember",
                    "creator must be a project member"
            ));
        }

        return Ticket.create(ticketId, id, milestoneId, title, description, createdBy, now)
                .map(t -> {
                    var next = new HashMap<>(tickets);
                    next.put(ticketId, t);
                    return new Project(
                            id, key, name, description,
                            managerId, teamLeadId,
                            members,
                            milestones,
                            next,
                            bugReports,
                            createdAt,
                            now
                    );
                });
    }

    public DomainResult<Project> assignDeveloperToTicket(TicketId ticketId, UserId developerId, Instant now) {
        Objects.requireNonNull(ticketId, "ticketId");
        Objects.requireNonNull(developerId, "developerId");
        Objects.requireNonNull(now, "now");

        var t = tickets.get(ticketId);
        if (t == null) {
            return DomainResult.err(new DomainError.NotFound("Ticket", ticketId.toString()));
        }
        if (t.status() == TicketStatus.DONE) {
            return DomainResult.err(new DomainError.InvariantViolation(
                    "ticket.notDoneForAssign",
                    "cannot assign developers to DONE ticket"
            ));
        }

        var role = members.get(developerId);
        if (role != ProjectRole.DEVELOPER && role != ProjectRole.TEAM_LEAD) {
            return DomainResult.err(new DomainError.InvariantViolation(
                    "ticket.assigneeRole",
                    "assignee must be DEVELOPER or TEAM_LEAD"
            ));
        }

        var updated = t.assign(developerId, now);
        var next = new HashMap<>(tickets);
        next.put(ticketId, updated);

        return DomainResult.ok(new Project(
                id, key, name, description,
                managerId, teamLeadId,
                members,
                milestones,
                next,
                bugReports,
                createdAt,
                now
        ));
    }

    public DomainResult<Project> applyTicketAction(TicketId ticketId, TicketAction action, Instant now) {
        Objects.requireNonNull(ticketId, "ticketId");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(now, "now");

        var t = tickets.get(ticketId);
        if (t == null) {
            return DomainResult.err(new DomainError.NotFound("Ticket", ticketId.toString()));
        }

        return t.apply(action, now).map(updated -> {
            var next = new HashMap<>(tickets);
            next.put(ticketId, updated);
            return new Project(
                    id, key, name, description,
                    managerId, teamLeadId,
                    members,
                    milestones,
                    next,
                    bugReports,
                    createdAt,
                    now
            );
        });
    }

    // ---------- BugReports ----------

    public DomainResult<Project> createBugReport(BugReportId bugId,
                                                 Title title,
                                                 Description description,
                                                 UserId createdBy,
                                                 Instant now) {
        Objects.requireNonNull(bugId, "bugId");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(createdBy, "createdBy");
        Objects.requireNonNull(now, "now");

        if (bugReports.containsKey(bugId)) {
            return DomainResult.err(new DomainError.Conflict("BugReport already exists: " + bugId));
        }

        var role = members.get(createdBy);
        if (role != ProjectRole.DEVELOPER && role != ProjectRole.TESTER && role != ProjectRole.TEAM_LEAD) {
            return DomainResult.err(new DomainError.InvariantViolation(
                    "bug.creatorRole",
                    "only DEVELOPER/TEAM_LEAD/TESTER can create bug reports"
            ));
        }

        return BugReport.create(bugId, id, title, description, createdBy, now)
                .map(b -> {
                    var next = new HashMap<>(bugReports);
                    next.put(bugId, b);
                    return new Project(
                            id, key, name, description,
                            managerId, teamLeadId,
                            members,
                            milestones,
                            tickets,
                            next,
                            createdAt,
                            now
                    );
                });
    }

    public DomainResult<Project> assignBugToDeveloper(BugReportId bugId, UserId developerId, Instant now) {
        Objects.requireNonNull(bugId, "bugId");
        Objects.requireNonNull(developerId, "developerId");
        Objects.requireNonNull(now, "now");

        var b = bugReports.get(bugId);
        if (b == null) {
            return DomainResult.err(new DomainError.NotFound("BugReport", bugId.toString()));
        }
        if (b.status() == BugStatus.CLOSED) {
            return DomainResult.err(new DomainError.InvariantViolation(
                    "bug.notClosedForAssign",
                    "cannot assign developer to CLOSED bug report"
            ));
        }

        var role = members.get(developerId);
        if (role != ProjectRole.DEVELOPER && role != ProjectRole.TEAM_LEAD) {
            return DomainResult.err(new DomainError.InvariantViolation(
                    "bug.assigneeRole",
                    "assignee must be DEVELOPER or TEAM_LEAD"
            ));
        }

        var updated = b.assignTo(developerId, now);
        var next = new HashMap<>(bugReports);
        next.put(bugId, updated);

        return DomainResult.ok(new Project(
                id, key, name, description,
                managerId, teamLeadId,
                members,
                milestones,
                tickets,
                next,
                createdAt,
                now
        ));
    }

    public DomainResult<Project> applyBugReportAction(BugReportId bugId, BugReportAction action, Instant now) {
        Objects.requireNonNull(bugId, "bugId");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(now, "now");

        var b = bugReports.get(bugId);
        if (b == null) {
            return DomainResult.err(new DomainError.NotFound("BugReport", bugId.toString()));
        }

        // доменные ограничения на роли (чтобы модель не могла стать "неконсистентной")
        var actor = switch (action) {
            case BugReportAction.Fix(var a) -> a;
            case BugReportAction.Test(var a) -> a;
            case BugReportAction.Close(var a) -> a;
        };

        var role = members.get(actor);
        if (role == null) {
            return DomainResult.err(new DomainError.InvariantViolation(
                    "bug.actorIsMember",
                    "actor must be a project member"
            ));
        }

        if (action instanceof BugReportAction.Fix) {
            if (role != ProjectRole.DEVELOPER && role != ProjectRole.TEAM_LEAD) {
                return DomainResult.err(new DomainError.InvariantViolation(
                        "bug.fixRole",
                        "only DEVELOPER/TEAM_LEAD can fix bugs"
                ));
            }
        }
        if (action instanceof BugReportAction.Test) {
            if (role != ProjectRole.TESTER) {
                return DomainResult.err(new DomainError.InvariantViolation(
                        "bug.testRole",
                        "only TESTER can test bug fixes"
                ));
            }
        }
        if (action instanceof BugReportAction.Close) {
            if (role != ProjectRole.MANAGER && role != ProjectRole.TESTER) {
                return DomainResult.err(new DomainError.InvariantViolation(
                        "bug.closeRole",
                        "only MANAGER or TESTER can close bugs"
                ));
            }
        }

        return b.apply(action, now).map(updated -> {
            var next = new HashMap<>(bugReports);
            next.put(bugId, updated);
            return new Project(
                    id, key, name, description,
                    managerId, teamLeadId,
                    members,
                    milestones,
                    tickets,
                    next,
                    createdAt,
                    now
            );
        });
    }
}
