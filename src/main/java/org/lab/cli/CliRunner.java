package org.lab.cli;

import org.lab.app.Presenter;
import org.lab.app.ProjectManagementService;
import org.lab.app.Result;
import org.lab.domain.BugReportId;
import org.lab.domain.DomainError;
import org.lab.domain.MilestoneId;
import org.lab.domain.ProjectId;
import org.lab.domain.TicketId;
import org.lab.domain.UserId;
import org.lab.domain.enums.BugStatus;
import org.lab.domain.enums.ProjectRole;
import org.lab.domain.enums.TicketStatus;
import org.lab.infra.ProjectRepository;
import org.lab.infra.UserRepository;

import java.util.Locale;
import java.util.Objects;

public final class CliRunner {

    private final ProjectManagementService service;
    private final UserRepository users;
    private final ProjectRepository projects;
    private final CliState state;

    public CliRunner(ProjectManagementService service, UserRepository users, ProjectRepository projects, CliState state) {
        this.service = Objects.requireNonNull(service, "service");
        this.users = Objects.requireNonNull(users, "users");
        this.projects = Objects.requireNonNull(projects, "projects");
        this.state = Objects.requireNonNull(state, "state");
    }

    /**
     * Modern Java:
     * - Pattern matching for switch: switch по sealed Command с record-pattern’ами
     *   (case Command.Register(var login, var displayName) -> ...).
     * - Sealed Result: единый тип результата для всех команд (успех/ошибка) вместо исключений.
     */
    public Result<String> execute(Command command) {
        Objects.requireNonNull(command, "command");

        return switch (command) {
            case Command.Register(var login, var displayName) -> execRegister(login, displayName);
            case Command.CreateProject(var actorLogin, var name, var description) -> execCreateProject(actorLogin, name, description);
            case Command.AddDev(var actorLogin, var projectRef, var memberLogin, var role) -> execAddMember(actorLogin, projectRef, memberLogin, role);

            case Command.CreateMilestone(var actorLogin, var projectRef, var name, var start, var end) ->
                    execCreateMilestone(actorLogin, projectRef, name, start, end);

            case Command.ActivateMilestone(var actorLogin, var projectRef, var milestoneRef) ->
                    execActivateMilestone(actorLogin, projectRef, milestoneRef);

            case Command.CreateTicket(var actorLogin, var projectRef, var milestoneRef, var title, var description) ->
                    execCreateTicket(actorLogin, projectRef, milestoneRef, title, description);

            case Command.AssignTicket(var actorLogin, var projectRef, var ticketRef, var developerLogin) ->
                    execAssignTicket(actorLogin, projectRef, ticketRef, developerLogin);

            case Command.StartTicket(var actorLogin, var projectRef, var ticketRef) ->
                    execStartTicket(actorLogin, projectRef, ticketRef);

            case Command.DoneTicket(var actorLogin, var projectRef, var ticketRef) ->
                    execDoneTicket(actorLogin, projectRef, ticketRef);

            case Command.CreateBug(var actorLogin, var projectRef, var title, var description) ->
                    execCreateBug(actorLogin, projectRef, title, description);

            case Command.FixBug(var actorLogin, var projectRef, var bugRef) ->
                    execFixBug(actorLogin, projectRef, bugRef);

            case Command.TestBug(var actorLogin, var projectRef, var bugRef) ->
                    execTestBug(actorLogin, projectRef, bugRef);

            case Command.CloseBug(var actorLogin, var projectRef, var bugRef) ->
                    execCloseBug(actorLogin, projectRef, bugRef);

            case Command.Dashboard(var actorLogin) ->
                    execDashboard(actorLogin);
        };
    }

    private Result<String> execRegister(String login, String displayName) {
        var res = service.register(login, displayName);
        return res.map(u -> {
            state.rememberUser(u.login(), u.id());
            return "Registered: login=" + u.login() + ", id=" + u.id();
        });
    }

    private Result<String> execCreateProject(String actorLogin, String name, String description) {
        return resolveUser(actorLogin)
                .flatMap(actorId -> service.createProject(actorId, name, description))
                .map(pv -> {
                    state.rememberProject(pv.id(), pv.key());
                    return Presenter.projectCreated(pv);
                });
    }

    /**
     * Modern Java:
     * - Switch expression: выбирает ветку выполнения по нормализованной роли и возвращает Result прямо из switch.
     * - Функциональный стиль: композиция операций через flatMap/map (Result-монадоподобный API).
     */
    private Result<String> execAddMember(String actorLogin, String projectRef, String memberLogin, String roleRaw) {
        var role = normalizeRole(roleRaw);
        return resolveUser(actorLogin).flatMap(actorId ->
                resolveProjectId(projectRef).flatMap(projectId ->
                        resolveUser(memberLogin).flatMap(memberId -> {
                            return switch (role) {
                                case DEVELOPER -> service.addDeveloper(actorId, projectId, memberId)
                                        .map(updated -> Presenter.roleAssigned(projectId, memberId, ProjectRole.DEVELOPER));
                                case TESTER -> service.addTester(actorId, projectId, memberId)
                                        .map(updated -> Presenter.roleAssigned(projectId, memberId, ProjectRole.TESTER));
                            };
                        })
                )
        );
    }

    private Result<String> execCreateMilestone(String actorLogin, String projectRef, String name, java.time.LocalDate start, java.time.LocalDate end) {
        return resolveUser(actorLogin).flatMap(actorId ->
                resolveProjectId(projectRef).flatMap(projectId ->
                        service.createMilestone(actorId, projectId, name, start, end)
                                .map(ms -> {
                                    state.rememberMilestone(projectId, ms.id());
                                    return Presenter.milestoneCreated(ms);
                                })
                )
        );
    }

    private Result<String> execActivateMilestone(String actorLogin, String projectRef, String milestoneRef) {
        return resolveUser(actorLogin).flatMap(actorId ->
                resolveProjectId(projectRef).flatMap(projectId ->
                        resolveMilestoneId(projectId, milestoneRef).flatMap(mid ->
                                service.activateMilestone(actorId, projectId, mid)
                                        .map(Presenter::milestoneCreated)
                        )
                )
        );
    }

    private Result<String> execCreateTicket(String actorLogin, String projectRef, String milestoneRef, String title, String description) {
        return resolveUser(actorLogin).flatMap(actorId ->
                resolveProjectId(projectRef).flatMap(projectId ->
                        resolveMilestoneId(projectId, milestoneRef).flatMap(mid ->
                                service.createTicket(actorId, projectId, mid, title, description)
                                        .map(tv -> {
                                            state.rememberTicket(projectId, tv.id());
                                            return Presenter.ticketCreated(tv);
                                        })
                        )
                )
        );
    }

    private Result<String> execAssignTicket(String actorLogin, String projectRef, String ticketRef, String developerLogin) {
        return resolveUser(actorLogin).flatMap(actorId ->
                resolveProjectId(projectRef).flatMap(projectId ->
                        resolveTicketId(projectId, ticketRef).flatMap(tid ->
                                resolveUser(developerLogin).flatMap(devId ->
                                        service.assignDeveloperToTicket(actorId, projectId, tid, devId)
                                                .map(Presenter::ticketCreated)
                                )
                        )
                )
        );
    }

    /**
     * Modern Java:
     * - Switch expression по TicketStatus: реализует переходы статуса (NEW/ACCEPTED/IN_PROGRESS/DONE)
     *   как выражение, возвращающее Result, без вложенных if/else.
     * - Функциональный стиль: цепочки flatMap/map для последовательных бизнес-операций.
     */
    private Result<String> execStartTicket(String actorLogin, String projectRef, String ticketRef) {
        return resolveUser(actorLogin).flatMap(actorId ->
                resolveProjectId(projectRef).flatMap(projectId ->
                        resolveTicketId(projectId, ticketRef).flatMap(tid -> {
                            var current = loadTicketStatus(projectId, tid);
                            if (current == null) {
                                return Result.fail(new org.lab.app.FailureCause.Domain(new DomainError.NotFound("Ticket", tid.toString())));
                            }

                            return switch (current) {
                                case NEW -> service.acceptTicket(actorId, projectId, tid)
                                        .flatMap(afterAccept -> service.startTicket(actorId, projectId, tid)
                                                .map(afterStart -> Presenter.ticketStatusChanged(afterStart, TicketStatus.NEW)));
                                case ACCEPTED -> service.startTicket(actorId, projectId, tid)
                                        .map(afterStart -> Presenter.ticketStatusChanged(afterStart, TicketStatus.ACCEPTED));
                                case IN_PROGRESS -> Result.ok("Ticket already IN_PROGRESS: " + tid);
                                case DONE -> Result.ok("Ticket already DONE: " + tid);
                            };
                        })
                )
        );
    }

    /**
     * Modern Java:
     * - Switch expression по TicketStatus: “доводит” тикет до DONE корректной цепочкой шагов.
     * - Функциональный стиль: композиция accept/start/complete через flatMap/map (без try/catch).
     */
    private Result<String> execDoneTicket(String actorLogin, String projectRef, String ticketRef) {
        return resolveUser(actorLogin).flatMap(actorId ->
                resolveProjectId(projectRef).flatMap(projectId ->
                        resolveTicketId(projectId, ticketRef).flatMap(tid -> {
                            var current = loadTicketStatus(projectId, tid);
                            if (current == null) {
                                return Result.fail(new org.lab.app.FailureCause.Domain(new DomainError.NotFound("Ticket", tid.toString())));
                            }

                            return switch (current) {
                                case NEW -> service.acceptTicket(actorId, projectId, tid)
                                        .flatMap(a -> service.startTicket(actorId, projectId, tid))
                                        .flatMap(s -> service.completeTicket(actorId, projectId, tid))
                                        .map(done -> Presenter.ticketStatusChanged(done, TicketStatus.NEW));
                                case ACCEPTED -> service.startTicket(actorId, projectId, tid)
                                        .flatMap(s -> service.completeTicket(actorId, projectId, tid))
                                        .map(done -> Presenter.ticketStatusChanged(done, TicketStatus.ACCEPTED));
                                case IN_PROGRESS -> service.completeTicket(actorId, projectId, tid)
                                        .map(done -> Presenter.ticketStatusChanged(done, TicketStatus.IN_PROGRESS));
                                case DONE -> Result.ok("Ticket already DONE: " + tid);
                            };
                        })
                )
        );
    }

    private Result<String> execCreateBug(String actorLogin, String projectRef, String title, String description) {
        return resolveUser(actorLogin).flatMap(actorId ->
                resolveProjectId(projectRef).flatMap(projectId ->
                        service.createBugReport(actorId, projectId, title, description)
                                .map(bv -> {
                                    state.rememberBug(projectId, bv.id());
                                    return Presenter.bugReportCreated(bv);
                                })
                )
        );
    }

    private Result<String> execFixBug(String actorLogin, String projectRef, String bugRef) {
        return resolveUser(actorLogin).flatMap(actorId ->
                resolveProjectId(projectRef).flatMap(projectId ->
                        resolveBugId(projectId, bugRef).flatMap(bid -> {
                            var from = loadBugStatus(projectId, bid);
                            if (from == null) {
                                return Result.fail(new org.lab.app.FailureCause.Domain(new DomainError.NotFound("BugReport", bid.toString())));
                            }
                            return service.fixBugReport(actorId, projectId, bid)
                                    .map(bv -> Presenter.bugReportStatusChanged(bv, from));
                        })
                )
        );
    }

    private Result<String> execTestBug(String actorLogin, String projectRef, String bugRef) {
        return resolveUser(actorLogin).flatMap(actorId ->
                resolveProjectId(projectRef).flatMap(projectId ->
                        resolveBugId(projectId, bugRef).flatMap(bid -> {
                            var from = loadBugStatus(projectId, bid);
                            if (from == null) {
                                return Result.fail(new org.lab.app.FailureCause.Domain(new DomainError.NotFound("BugReport", bid.toString())));
                            }
                            return service.testBugReport(actorId, projectId, bid)
                                    .map(bv -> Presenter.bugReportStatusChanged(bv, from));
                        })
                )
        );
    }

    private Result<String> execCloseBug(String actorLogin, String projectRef, String bugRef) {
        return resolveUser(actorLogin).flatMap(actorId ->
                resolveProjectId(projectRef).flatMap(projectId ->
                        resolveBugId(projectId, bugRef).flatMap(bid -> {
                            var from = loadBugStatus(projectId, bid);
                            if (from == null) {
                                return Result.fail(new org.lab.app.FailureCause.Domain(new DomainError.NotFound("BugReport", bid.toString())));
                            }
                            return service.closeBugReport(actorId, projectId, bid)
                                    .map(bv -> Presenter.bugReportStatusChanged(bv, from));
                        })
                )
        );
    }

    /**
     * Modern Java:
     * - Sealed Result + функциональная композиция: resolveUser(...).flatMap(service::buildDashboard).map(Presenter::dashboard).
     * - Косвенно демонстрирует structured concurrency, т.к. buildDashboard внутри сервиса выполняется параллельно.
     */
    private Result<String> execDashboard(String actorLogin) {
        return resolveUser(actorLogin)
                .flatMap(actorId -> service.buildDashboard(actorId))
                .map(d -> Presenter.dashboard(actorLogin, d));
    }

    private Result<UserId> resolveUser(String login) {
        Objects.requireNonNull(login, "login");

        var cached = state.userId(login);
        if (cached.isPresent()) {
            return Result.ok(cached.get());
        }

        return users.findByLogin(login)
                .map(u -> {
                    state.rememberUser(login, u.id());
                    return Result.ok(u.id());
                })
                .orElseGet(() -> Result.fail(new org.lab.app.FailureCause.Domain(new DomainError.NotFound("User(login)", login))));
    }

    private Result<ProjectId> resolveProjectId(String projectRef) {
        Objects.requireNonNull(projectRef, "projectRef");
        var ref = projectRef.trim();

        if (ref.equalsIgnoreCase("lastProject") || ref.equalsIgnoreCase("last")) {
            return state.lastProjectId()
                    .map(Result::ok)
                    .orElseGet(() -> Result.fail(new org.lab.app.FailureCause.Domain(new DomainError.NotFound("Project(last)", "no lastProject in state"))));
        }

        if (CliState.looksLikeUuid(ref)) {
            return Result.ok(new ProjectId(CliState.parseUuidStrict(ref, "projectId")));
        }

        return state.projectIdByKey(ref)
                .map(Result::ok)
                .orElseGet(() -> Result.fail(new org.lab.app.FailureCause.Domain(new DomainError.NotFound("Project(key)", ref))));
    }

    private Result<MilestoneId> resolveMilestoneId(ProjectId projectId, String milestoneRef) {
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(milestoneRef, "milestoneRef");
        var ref = milestoneRef.trim();

        if (ref.equalsIgnoreCase("lastMilestone") || ref.equalsIgnoreCase("last")) {
            return state.lastMilestone(projectId)
                    .map(Result::ok)
                    .orElseGet(() -> Result.fail(new org.lab.app.FailureCause.Domain(new DomainError.NotFound("Milestone(last)", "no lastMilestone in state"))));
        }

        if (!CliState.looksLikeUuid(ref)) {
            return Result.fail(new org.lab.app.FailureCause.Domain(new DomainError.InvalidValue("milestoneRef", "expected UUID or lastMilestone/last")));
        }

        return Result.ok(new MilestoneId(CliState.parseUuidStrict(ref, "milestoneId")));
    }

    private Result<TicketId> resolveTicketId(ProjectId projectId, String ticketRef) {
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(ticketRef, "ticketRef");
        var ref = ticketRef.trim();

        if (ref.equalsIgnoreCase("lastTicket") || ref.equalsIgnoreCase("last")) {
            return state.lastTicket(projectId)
                    .map(Result::ok)
                    .orElseGet(() -> Result.fail(new org.lab.app.FailureCause.Domain(new DomainError.NotFound("Ticket(last)", "no lastTicket in state"))));
        }

        if (!CliState.looksLikeUuid(ref)) {
            return Result.fail(new org.lab.app.FailureCause.Domain(new DomainError.InvalidValue("ticketRef", "expected UUID or lastTicket/last")));
        }

        return Result.ok(new TicketId(CliState.parseUuidStrict(ref, "ticketId")));
    }

    private Result<BugReportId> resolveBugId(ProjectId projectId, String bugRef) {
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(bugRef, "bugRef");
        var ref = bugRef.trim();

        if (ref.equalsIgnoreCase("lastBug") || ref.equalsIgnoreCase("last")) {
            return state.lastBug(projectId)
                    .map(Result::ok)
                    .orElseGet(() -> Result.fail(new org.lab.app.FailureCause.Domain(new DomainError.NotFound("BugReport(last)", "no lastBug in state"))));
        }

        if (!CliState.looksLikeUuid(ref)) {
            return Result.fail(new org.lab.app.FailureCause.Domain(new DomainError.InvalidValue("bugRef", "expected UUID or lastBug/last")));
        }

        return Result.ok(new BugReportId(CliState.parseUuidStrict(ref, "bugReportId")));
    }

    private TicketStatus loadTicketStatus(ProjectId projectId, TicketId ticketId) {
        var p = projects.findById(projectId).orElse(null);
        if (p == null) return null;
        var t = p.tickets().get(ticketId);
        if (t == null) return null;
        return t.status();
    }

    private BugStatus loadBugStatus(ProjectId projectId, BugReportId bugId) {
        var p = projects.findById(projectId).orElse(null);
        if (p == null) return null;
        var b = p.bugReports().get(bugId);
        if (b == null) return null;
        return b.status();
    }

    private enum MemberRole { DEVELOPER, TESTER }

    /**
     * Modern Java:
     * - Switch expression по строке: компактная нормализация входа (DEV/DEVELOPER, TEST/TESTER) через switch ->.
     * - Locale.ROOT: корректная нормализация регистра из стандартной библиотеки (без региональных сюрпризов).
     */
    private MemberRole normalizeRole(String roleRaw) {
        Objects.requireNonNull(roleRaw, "roleRaw");
        var v = roleRaw.trim().toUpperCase(Locale.ROOT);
        return switch (v) {
            case "DEV", "DEVELOPER" -> MemberRole.DEVELOPER;
            case "TEST", "TESTER" -> MemberRole.TESTER;
            default -> throw new IllegalArgumentException("Unknown role for AddDev: " + roleRaw + " (expected DEVELOPER/TESTER)");
        };
    }
}
