package org.lab.app;

import org.lab.domain.*;
import org.lab.domain.enums.BugStatus;
import org.lab.infra.BugReportRepository;
import org.lab.infra.ProjectRepository;
import org.lab.infra.TicketRepository;
import org.lab.infra.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.concurrent.StructuredTaskScope;

public final class ProjectManagementService {

    private final UserRepository users;
    private final ProjectRepository projects;
    private final TicketRepository tickets;
    private final BugReportRepository bugs;
    private final Clock clock;

    public ProjectManagementService(UserRepository users,
                                    ProjectRepository projects,
                                    TicketRepository tickets,
                                    BugReportRepository bugs) {
        this(users, projects, tickets, bugs, Clock.systemUTC());
    }

    public ProjectManagementService(UserRepository users,
                                    ProjectRepository projects,
                                    TicketRepository tickets,
                                    BugReportRepository bugs,
                                    Clock clock) {
        this.users = Objects.requireNonNull(users, "users");
        this.projects = Objects.requireNonNull(projects, "projects");
        this.tickets = Objects.requireNonNull(tickets, "tickets");
        this.bugs = Objects.requireNonNull(bugs, "bugs");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    private Instant now() {
        return clock.instant();
    }

    // ---------------- Common for all users ----------------

    public Result<UserView> register(String login, String displayName) {
        var id = users.nextId();
        var ts = now();

        return fromDomain(User.register(id, login, displayName, ts))
                .flatMap(u -> fromDomain(users.insert(u)))
                .map(u -> new UserView(u.id(), u.login(), u.displayName()));
    }

    public Result<ProjectView> createProject(UserId creatorId, String name, String description) {
        Objects.requireNonNull(creatorId, "creatorId");

        var userCheck = ensureUserExists(creatorId);
        if (userCheck.isFailure()) {
            return Result.fail(userCheck.failureOrNull());
        }

        var id = projects.nextId();
        var key = projects.nextProjectKey();
        var ts = now();

        return fromDomain(Project.create(id, key, name, description, creatorId, ts))
                .flatMap(p -> fromDomain(projects.insert(p)))
                .map(p -> toProjectView(p, creatorId));
    }

    public Result<List<ProjectView>> listMyProjects(UserId userId) {
        Objects.requireNonNull(userId, "userId");

        var userCheck = ensureUserExists(userId);
        if (userCheck.isFailure()) {
            return Result.fail(userCheck.failureOrNull());
        }

        var list = projects.findByMember(userId).stream()
                .map(p -> toProjectView(p, userId))
                .collect(Collectors.toUnmodifiableList());

        return Result.ok(list);
    }

    public Result<List<TicketView>> listMyTickets(UserId userId) {
        Objects.requireNonNull(userId, "userId");

        var userCheck = ensureUserExists(userId);
        if (userCheck.isFailure()) {
            return Result.fail(userCheck.failureOrNull());
        }

        var list = tickets.findByAssignee(userId).stream()
                .map(this::toTicketView)
                .collect(Collectors.toUnmodifiableList());

        return Result.ok(list);
    }

    public Result<List<BugReportView>> listBugsToFix(UserId userId) {
        Objects.requireNonNull(userId, "userId");

        var userCheck = ensureUserExists(userId);
        if (userCheck.isFailure()) {
            return Result.fail(userCheck.failureOrNull());
        }

        var list = bugs.findToFix(userId).stream()
                .map(this::toBugView)
                .collect(Collectors.toUnmodifiableList());

        return Result.ok(list);
    }

    /**
     * Modern Java:
     * - Structured Concurrency (preview): использует StructuredTaskScope.open() для параллельного fork/join трёх задач
     *   (проекты, тикеты, actionable-bugs) как единого блока работ с корректным join и обработкой InterruptedException.
     * - Pattern matching for switch: в обработке FailedException разбирает причину через switch с type pattern
     *   (case TaskFailure tf -> ...), без ручных instanceof/кастов.
     * - Sealed-результат: возвращает Result<DashboardView> (Success/Failure), т.е. типизированная модель успеха/ошибки
     *   вместо исключений как механизма бизнес-ошибок.
     */

    public Result<DashboardView> buildDashboard(UserId userId) {
        Objects.requireNonNull(userId, "userId");

        var userCheck = ensureUserExists(userId);
        if (userCheck.isFailure()) {
            return Result.fail(userCheck.failureOrNull());
        }

        try (var scope = StructuredTaskScope.open()) {
            var projectsTask = scope.fork(() -> unwrap(listMyProjects(userId)));
            var ticketsTask  = scope.fork(() -> unwrap(listMyTickets(userId)));
            var bugsTask     = scope.fork(() -> unwrap(listActionableBugs(userId)));

            scope.join(); // propagates failures (throws FailedException)

            var view = new DashboardView(
                    userId,
                    projectsTask.get(),
                    ticketsTask.get(),
                    bugsTask.get()
            );
            return Result.ok(view);

        } catch (StructuredTaskScope.FailedException e) {
            var cause = e.getCause();

            return switch (cause) {
                case TaskFailure tf -> Result.fail(tf.cause());
                default -> Result.fail(new FailureCause.Domain(
                        new DomainError.InvariantViolation("dashboard.failed", "Unexpected failure: " + cause)
                ));
            };

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.fail(new FailureCause.Domain(
                    new DomainError.InvariantViolation("dashboard.interrupted", "Thread interrupted while building dashboard")
            ));
        }
    }

    /**
     * Modern Java:
     * - Функциональный стиль (Stream API): собирает список через stream/flatMap/map/Collectors.toUnmodifiableList().
     * - Расширения стандартной библиотеки: использует Collectors.toUnmodifiableList() для неизменяемого результата.
     * - Sealed-результат: возвращает Result<List<...>>, сохраняя ошибки домена/доступа в типе результата.
     */

    private Result<List<BugReportView>> listActionableBugs(UserId userId) {
        Objects.requireNonNull(userId, "userId");

        var userCheck = ensureUserExists(userId);
        if (userCheck.isFailure()) {
            return Result.fail(userCheck.failureOrNull());
        }

        var memberProjects = projects.findByMember(userId);

        var list = memberProjects.stream()
                .flatMap(p -> bugsForRole(p, userId))
                .map(this::toBugView)
                .collect(Collectors.toUnmodifiableList());

        return Result.ok(list);
    }

    /**
     * Modern Java:
     * - Switch expression: возвращает значение (Stream<BugReport>) непосредственно из switch по роли.
     * - В сочетании с enum-статусами и Stream API показывает “выражаемую” бизнес-логику без if/else-цепочек.
     */
    private Stream<BugReport> bugsForRole(Project p, UserId userId) {
        var roleOpt = p.roleOf(userId);
        if (roleOpt.isEmpty()) {
            return Stream.empty();
        }

        var role = roleOpt.get();
        return switch (role) {
            case DEVELOPER -> p.bugReports().values().stream()
                    .filter(b -> b.status() == BugStatus.NEW)
                    .filter(b -> userId.equals(b.assignedTo()));

            case TESTER -> p.bugReports().values().stream()
                    .filter(b -> b.status() == BugStatus.FIXED);

            case MANAGER, TEAM_LEAD -> Stream.empty();
        };
    }

    /**
     * Modern Java:
     * - Sealed Result + функциональная модель ошибок: превращает Result<T> в значение либо кидает доменный TaskFailure.
     * - Используется совместно со structured concurrency, чтобы пробросить Failure из подзадачи через исключение
     *   (технический мост между Result-моделью и механизмом FailedException у StructuredTaskScope).
     */
    private static <T> T unwrap(Result<T> r) {
        if (r.isSuccess()) {
            return r.toOptional().orElseThrow();
        }
        throw new TaskFailure(Objects.requireNonNull(r.failureOrNull(), "failure"));
    }

    private static final class TaskFailure extends RuntimeException {
        private final FailureCause cause;

        private TaskFailure(FailureCause cause) {
            super(cause.code() + ": " + cause.message());
            this.cause = cause;
        }

        public FailureCause cause() {
            return cause;
        }
    }

    public Result<ProjectView> addDeveloper(UserId actorId, ProjectId projectId, UserId developerId) {
        return withProjectAndPermission(actorId, projectId, Operation.ADD_DEVELOPER)
                .flatMap(ctx -> ensureUserExists(developerId)
                        .flatMap(ignored ->
                                fromDomain(projects.update(projectId, p -> p.addDeveloper(developerId, now())))
                                        .map(updated -> toProjectView(updated, actorId))
                        ));
    }

    public Result<ProjectView> addTester(UserId actorId, ProjectId projectId, UserId testerId) {
        return withProjectAndPermission(actorId, projectId, Operation.ADD_TESTER)
                .flatMap(ctx -> ensureUserExists(testerId)
                        .flatMap(ignored ->
                                fromDomain(projects.update(projectId, p -> p.addTester(testerId, now())))
                                        .map(updated -> toProjectView(updated, actorId))
                        ));
    }

    public Result<MilestoneView> createMilestone(UserId actorId,
                                                 ProjectId projectId,
                                                 String milestoneName,
                                                 LocalDate start,
                                                 LocalDate end) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");

        return withProjectAndPermission(actorId, projectId, Operation.CREATE_MILESTONE)
                .flatMap(ctx -> fromDomain(DateRange.of(start, end))
                        .flatMap(range -> {
                            var mid = MilestoneId.newId();
                            return fromDomain(projects.update(projectId, p -> p.createMilestone(mid, milestoneName, range, now())))
                                    .flatMap(updated -> {
                                        var ms = updated.milestones().get(mid);
                                        if (ms == null) {
                                            return Result.fail(new FailureCause.Domain(
                                                    new DomainError.InvariantViolation("milestone.created", "milestone not found after creation")
                                            ));
                                        }
                                        return Result.ok(toMilestoneView(ms));
                                    });
                        }));
    }

    public Result<MilestoneView> activateMilestone(UserId actorId, ProjectId projectId, MilestoneId milestoneId) {
        return withProjectAndPermission(actorId, projectId, Operation.ACTIVATE_MILESTONE)
                .flatMap(ctx -> fromDomain(projects.update(projectId, p -> p.activateMilestone(milestoneId, now()))))
                .flatMap(updated -> {
                    var ms = updated.milestones().get(milestoneId);
                    if (ms == null) {
                        return Result.fail(new FailureCause.Domain(new DomainError.NotFound("Milestone", milestoneId.toString())));
                    }
                    return Result.ok(toMilestoneView(ms));
                });
    }

    public Result<MilestoneView> closeMilestone(UserId actorId, ProjectId projectId, MilestoneId milestoneId) {
        return withProjectAndPermission(actorId, projectId, Operation.CLOSE_MILESTONE)
                .flatMap(ctx -> fromDomain(projects.update(projectId, p -> p.closeMilestone(milestoneId, now()))))
                .flatMap(updated -> {
                    var ms = updated.milestones().get(milestoneId);
                    if (ms == null) {
                        return Result.fail(new FailureCause.Domain(new DomainError.NotFound("Milestone", milestoneId.toString())));
                    }
                    return Result.ok(toMilestoneView(ms));
                });
    }

    public Result<TicketView> createTicket(UserId actorId,
                                           ProjectId projectId,
                                           MilestoneId milestoneId,
                                           String title,
                                           String description) {
        return withProjectAndPermission(actorId, projectId, Operation.CREATE_TICKET)
                .flatMap(ctx -> fromDomain(Title.of(title))
                        .flatMap(t -> fromDomain(Description.of(description))
                                .flatMap(d -> {
                                    var tid = tickets.nextId();
                                    return fromDomain(projects.update(projectId, p -> p.createTicket(tid, milestoneId, t, d, actorId, now())))
                                            .flatMap(updated -> {
                                                var ticket = updated.tickets().get(tid);
                                                if (ticket == null) {
                                                    return Result.fail(new FailureCause.Domain(
                                                            new DomainError.InvariantViolation("ticket.created", "ticket not found after creation")
                                                    ));
                                                }
                                                var up = fromDomain(tickets.upsert(ticket));
                                                if (up.isFailure()) {
                                                    return Result.fail(up.failureOrNull());
                                                }
                                                return Result.ok(toTicketView(ticket));
                                            });
                                })));
    }

    public Result<TicketView> assignDeveloperToTicket(UserId actorId,
                                                      ProjectId projectId,
                                                      TicketId ticketId,
                                                      UserId developerId) {
        return withProjectAndPermission(actorId, projectId, Operation.ASSIGN_TICKET_DEVELOPER)
                .flatMap(ctx -> ensureUserExists(developerId)
                        .flatMap(ignored ->
                                fromDomain(projects.update(projectId, p -> p.assignDeveloperToTicket(ticketId, developerId, now())))
                                        .flatMap(updated -> {
                                            var ticket = updated.tickets().get(ticketId);
                                            if (ticket == null) {
                                                return Result.fail(new FailureCause.Domain(new DomainError.NotFound("Ticket", ticketId.toString())));
                                            }
                                            var up = fromDomain(tickets.upsert(ticket));
                                            if (up.isFailure()) {
                                                return Result.fail(up.failureOrNull());
                                            }
                                            return Result.ok(toTicketView(ticket));
                                        })
                        ));
    }

    public Result<TicketCompletionView> checkTicketCompletion(UserId actorId, ProjectId projectId, TicketId ticketId) {
        return withProjectAndPermission(actorId, projectId, Operation.CHECK_TICKET_COMPLETION)
                .flatMap(ctx -> getProject(projectId))
                .flatMap(p -> {
                    var ticket = p.tickets().get(ticketId);
                    if (ticket == null) {
                        return Result.fail(new FailureCause.Domain(new DomainError.NotFound("Ticket", ticketId.toString())));
                    }
                    return Result.ok(new TicketCompletionView(ticket.id(), ticket.status(), ticket.isDone()));
                });
    }

    public Result<TicketView> acceptTicket(UserId actorId, ProjectId projectId, TicketId ticketId) {
        return withProjectAndPermission(actorId, projectId, Operation.TICKET_ACCEPT)
                .flatMap(ctx -> fromDomain(projects.update(projectId, p -> p.applyTicketAction(ticketId, new TicketAction.Accept(actorId), now()))))
                .flatMap(updated -> {
                    var ticket = updated.tickets().get(ticketId);
                    if (ticket == null) {
                        return Result.fail(new FailureCause.Domain(new DomainError.NotFound("Ticket", ticketId.toString())));
                    }
                    var up = fromDomain(tickets.upsert(ticket));
                    if (up.isFailure()) {
                        return Result.fail(up.failureOrNull());
                    }
                    return Result.ok(toTicketView(ticket));
                });
    }

    public Result<TicketView> startTicket(UserId actorId, ProjectId projectId, TicketId ticketId) {
        return withProjectAndPermission(actorId, projectId, Operation.TICKET_START)
                .flatMap(ctx -> fromDomain(projects.update(projectId, p -> p.applyTicketAction(ticketId, new TicketAction.Start(actorId), now()))))
                .flatMap(updated -> {
                    var ticket = updated.tickets().get(ticketId);
                    if (ticket == null) {
                        return Result.fail(new FailureCause.Domain(new DomainError.NotFound("Ticket", ticketId.toString())));
                    }
                    var up = fromDomain(tickets.upsert(ticket));
                    if (up.isFailure()) {
                        return Result.fail(up.failureOrNull());
                    }
                    return Result.ok(toTicketView(ticket));
                });
    }

    public Result<TicketView> completeTicket(UserId actorId, ProjectId projectId, TicketId ticketId) {
        return withProjectAndPermission(actorId, projectId, Operation.TICKET_COMPLETE)
                .flatMap(ctx -> fromDomain(projects.update(projectId, p -> p.applyTicketAction(ticketId, new TicketAction.Complete(actorId), now()))))
                .flatMap(updated -> {
                    var ticket = updated.tickets().get(ticketId);
                    if (ticket == null) {
                        return Result.fail(new FailureCause.Domain(new DomainError.NotFound("Ticket", ticketId.toString())));
                    }
                    var up = fromDomain(tickets.upsert(ticket));
                    if (up.isFailure()) {
                        return Result.fail(up.failureOrNull());
                    }
                    return Result.ok(toTicketView(ticket));
                });
    }


    public Result<BugReportView> createBugReport(UserId actorId,
                                                 ProjectId projectId,
                                                 String title,
                                                 String description) {
        return withProjectAndPermission(actorId, projectId, Operation.CREATE_BUG_REPORT)
                .flatMap(ctx -> fromDomain(Title.of(title))
                        .flatMap(t -> fromDomain(Description.of(description))
                                .flatMap(d -> {
                                    var bid = bugs.nextId();
                                    return fromDomain(projects.update(projectId, p -> p.createBugReport(bid, t, d, actorId, now())))
                                            .flatMap(updated -> {
                                                var bug = updated.bugReports().get(bid);
                                                if (bug == null) {
                                                    return Result.fail(new FailureCause.Domain(
                                                            new DomainError.InvariantViolation("bug.created", "bug report not found after creation")
                                                    ));
                                                }
                                                var up = fromDomain(bugs.upsert(bug));
                                                if (up.isFailure()) {
                                                    return Result.fail(up.failureOrNull());
                                                }
                                                return Result.ok(toBugView(bug));
                                            });
                                })));
    }

    public Result<BugReportView> fixBugReport(UserId actorId, ProjectId projectId, BugReportId bugId) {
        return withProjectAndPermission(actorId, projectId, Operation.FIX_BUG_REPORT)
                .flatMap(ctx -> fromDomain(projects.update(projectId, p -> p.applyBugReportAction(bugId, new BugReportAction.Fix(actorId), now()))))
                .flatMap(updated -> {
                    var bug = updated.bugReports().get(bugId);
                    if (bug == null) {
                        return Result.fail(new FailureCause.Domain(new DomainError.NotFound("BugReport", bugId.toString())));
                    }
                    var up = fromDomain(bugs.upsert(bug));
                    if (up.isFailure()) {
                        return Result.fail(up.failureOrNull());
                    }
                    return Result.ok(toBugView(bug));
                });
    }

    public Result<BugReportView> testBugReport(UserId actorId, ProjectId projectId, BugReportId bugId) {
        return withProjectAndPermission(actorId, projectId, Operation.TEST_BUG_REPORT)
                .flatMap(ctx -> fromDomain(projects.update(projectId, p -> p.applyBugReportAction(bugId, new BugReportAction.Test(actorId), now()))))
                .flatMap(updated -> {
                    var bug = updated.bugReports().get(bugId);
                    if (bug == null) {
                        return Result.fail(new FailureCause.Domain(new DomainError.NotFound("BugReport", bugId.toString())));
                    }
                    var up = fromDomain(bugs.upsert(bug));
                    if (up.isFailure()) {
                        return Result.fail(up.failureOrNull());
                    }
                    return Result.ok(toBugView(bug));
                });
    }

    public Result<BugReportView> closeBugReport(UserId actorId, ProjectId projectId, BugReportId bugId) {
        return withProjectAndPermission(actorId, projectId, Operation.CLOSE_BUG_REPORT)
                .flatMap(ctx -> fromDomain(projects.update(projectId, p -> p.applyBugReportAction(bugId, new BugReportAction.Close(actorId), now()))))
                .flatMap(updated -> {
                    var bug = updated.bugReports().get(bugId);
                    if (bug == null) {
                        return Result.fail(new FailureCause.Domain(new DomainError.NotFound("BugReport", bugId.toString())));
                    }
                    var up = fromDomain(bugs.upsert(bug));
                    if (up.isFailure()) {
                        return Result.fail(up.failureOrNull());
                    }
                    return Result.ok(toBugView(bug));
                });
    }

    // ---------------- Internal helpers ----------------

    private Result<Unit> ensureUserExists(UserId userId) {
        Objects.requireNonNull(userId, "userId");
        return users.findById(userId)
                .<Result<Unit>>map(u -> Result.ok(Unit.INSTANCE))
                .orElseGet(() -> Result.fail(new FailureCause.Domain(new DomainError.NotFound("User", userId.toString()))));
    }

    private Result<Project> getProject(ProjectId projectId) {
        Objects.requireNonNull(projectId, "projectId");
        return projects.findById(projectId)
                .<Result<Project>>map(Result::ok)
                .orElseGet(() -> Result.fail(new FailureCause.Domain(new DomainError.NotFound("Project", projectId.toString()))));
    }

    private record ProjectContext(Project project, ActorRole role) { }

    private Result<ProjectContext> getContext(UserId actorId, ProjectId projectId) {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(projectId, "projectId");

        return getProject(projectId).map(p -> {
            var roleOpt = p.roleOf(actorId);
            var ar = ActorRole.from(roleOpt);
            return new ProjectContext(p, ar);
        });
    }

    private Result<ProjectContext> withProjectAndPermission(UserId actorId, ProjectId projectId, Operation op) {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(op, "op");

        var userCheck = ensureUserExists(actorId);
        if (userCheck.isFailure()) {
            return Result.fail(userCheck.failureOrNull());
        }

        return getContext(actorId, projectId).flatMap(ctx -> {
            if (!AccessControl.isAllowed(ctx.role(), op)) {
                return Result.fail(new AccessDenied(actorId, projectId, op.name(), ctx.role().name()));
            }
            if (ctx.role() instanceof ActorRole.Outsider) {
                return Result.fail(new AccessDenied(actorId, projectId, op.name(), ctx.role().name()));
            }
            return Result.ok(ctx);
        });
    }

    private static <T> Result<T> fromDomain(DomainResult<T> domain) {
        Objects.requireNonNull(domain, "domain");
        if (domain.isSuccess()) {
            return Result.ok(domain.orElseThrow());
        }
        var err = domain.errorOrNull();
        return Result.fail(new FailureCause.Domain(Objects.requireNonNull(err, "domain error")));
    }

    private ProjectView toProjectView(Project p, UserId viewer) {
        var role = p.roleOf(viewer).map(Enum::name).orElse("OUTSIDER");
        return new ProjectView(
                p.id(),
                p.key().value(),
                p.name(),
                p.managerId(),
                p.teamLeadId(),
                role,
                p.milestones().size(),
                p.tickets().size(),
                p.bugReports().size()
        );
    }

    private MilestoneView toMilestoneView(Milestone m) {
        return new MilestoneView(
                m.id(),
                m.projectId(),
                m.name(),
                m.range().start(),
                m.range().end(),
                m.status()
        );
    }

    private TicketView toTicketView(Ticket t) {
        return new TicketView(
                t.id(),
                t.projectId(),
                t.milestoneId(),
                t.title().value(),
                t.status(),
                t.assignees()
        );
    }

    private BugReportView toBugView(BugReport b) {
        return new BugReportView(
                b.id(),
                b.projectId(),
                b.title().value(),
                b.status(),
                b.assignedTo()
        );
    }
}
