package org.lab.app;

import org.lab.domain.ProjectId;
import org.lab.domain.UserId;
import org.lab.domain.enums.BugStatus;
import org.lab.domain.enums.ProjectRole;
import org.lab.domain.enums.TicketStatus;

import java.util.Objects;

public final class Presenter {

    private Presenter() {
    }

    /**
     * Modern Java:
     * - Text Blocks ("""..."""): многострочное человекочитаемое сообщение без конкатенации строк.
     * - String::formatted: безопасная подстановка значений (альтернатива “строковым шаблонам”, которых нет в JDK 26).
     */
    public static String projectCreated(ProjectView project) {
        Objects.requireNonNull(project, "project");
        return """
            Project created:
              key=%s
              id=%s
              name=%s
              manager=%s
              teamLead=%s
              myRole=%s
            """.formatted(
                project.key(),
                project.id(),
                project.name(),
                project.managerId(),
                project.teamLeadId(),
                project.myRole()
        );
    }

    /**
     * Modern Java:
     * - Text Blocks ("""...""") + formatted(): удобный вывод структурированных данных для CLI/логов.
     */
    public static String milestoneCreated(MilestoneView milestone) {
        Objects.requireNonNull(milestone, "milestone");
        return """
            Milestone created:
              id=%s
              project=%s
              name=%s
              range=%s..%s
              status=%s
            """.formatted(
                milestone.id(),
                milestone.projectId(),
                milestone.name(),
                milestone.start(),
                milestone.end(),
                milestone.status()
        );
    }

    /**
     * Modern Java:
     * - Text Blocks ("""...""") + formatted(): читаемое сообщение о назначении роли без ручной склейки строк.
     */
    public static String roleAssigned(ProjectId projectId, UserId userId, ProjectRole role) {
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(role, "role");
        return """
            Role assigned:
              project=%s
              user=%s
              role=%s
            """.formatted(projectId, userId, role);
    }

    public static String ticketCreated(TicketView ticket) {
        Objects.requireNonNull(ticket, "ticket");
        return """
            Ticket created:
              id=%s
              project=%s
              milestone=%s
              title="%s"
              status=%s
              assignees=%s
            """.formatted(
                ticket.id(),
                ticket.projectId(),
                ticket.milestoneId(),
                ticket.title(),
                ticket.status(),
                ticket.assignees()
        );
    }

    /**
     * Modern Java:
     * - Text Blocks ("""...""") + formatted(): структурированное сообщение о bug report.
     */
    public static String bugReportCreated(BugReportView bug) {
        Objects.requireNonNull(bug, "bug");
        return """
            Bug report created:
              id=%s
              project=%s
              title="%s"
              status=%s
              assignedTo=%s
            """.formatted(
                bug.id(),
                bug.projectId(),
                bug.title(),
                bug.status(),
                bug.assignedTo()
        );
    }

    /**
     * Modern Java:
     * - Text Blocks ("""...""") + formatted(): фиксирует переход статуса в формате “FROM -> TO”.
     */
    public static String bugReportStatusChanged(BugReportView bug, BugStatus from) {
        Objects.requireNonNull(bug, "bug");
        Objects.requireNonNull(from, "from");
        return """
            Bug report status changed:
              id=%s
              %s -> %s
              assignedTo=%s
            """.formatted(
                bug.id(),
                from,
                bug.status(),
                bug.assignedTo()
        );
    }

    /**
     * Modern Java:
     * - Text Blocks ("""...""") + formatted(): фиксирует переход статуса тикета “FROM -> TO”.
     */
    public static String ticketStatusChanged(TicketView ticket, TicketStatus from) {
        Objects.requireNonNull(ticket, "ticket");
        Objects.requireNonNull(from, "from");
        return """
            Ticket status changed:
              id=%s
              %s -> %s
              assignees=%s
            """.formatted(
                ticket.id(),
                from,
                ticket.status(),
                ticket.assignees()
        );
    }
    /**
     * Modern Java:
     * - Расширенная стандартная библиотека: StringBuilder для эффективной сборки большого вывода без лишних аллокаций.
     * - Использование records/view-объектов: читает данные через accessor-методы record’ов (d.projects(), d.tickets()...).
     */
    public static String dashboard(String actorLogin, DashboardView d) {
        Objects.requireNonNull(actorLogin, "actorLogin");
        Objects.requireNonNull(d, "d");

        var sb = new StringBuilder();
        sb.append("DASHBOARD for ").append(actorLogin).append(" (").append(d.userId()).append(")\n\n");

        sb.append("Projects: ").append(d.projects().size()).append("\n");
        for (var pr : d.projects()) {
            sb.append("  - ").append(pr.key()).append(" | ").append(pr.name())
                    .append(" | myRole=").append(pr.myRole())
                    .append(" | milestones=").append(pr.milestonesCount())
                    .append(" | tickets=").append(pr.ticketsCount())
                    .append(" | bugs=").append(pr.bugReportsCount())
                    .append("\n");
        }

        sb.append("\nMy tickets: ").append(d.tickets().size()).append("\n");
        for (var tv : d.tickets()) {
            sb.append("  - ").append(tv.id()).append(" | ").append(tv.title())
                    .append(" | ").append(tv.status())
                    .append(" | project=").append(tv.projectId())
                    .append("\n");
        }

        sb.append("\nActionable bugs (fix/check): ").append(d.actionableBugs().size()).append("\n");
        for (var bv : d.actionableBugs()) {
            sb.append("  - ").append(bv.id()).append(" | ").append(bv.title())
                    .append(" | ").append(bv.status())
                    .append(" | assignedTo=").append(bv.assignedTo())
                    .append(" | project=").append(bv.projectId())
                    .append("\n");
        }

        return sb.toString();
    }

    /**
     * Modern Java:
     * - Sealed Result + “алгебраический” API: использует result.match(ok -> ..., failure -> ...)
     *   вместо try/catch и проверки типов исключений.
     */
    public static String result(String operationName, Result<?> result) {
        Objects.requireNonNull(operationName, "operationName");
        Objects.requireNonNull(result, "result");
        return result.match(
                ok -> "OK: " + operationName,
                failure -> failureMessage(operationName, failure)
        );
    }

    public static String failureMessage(String operationName, FailureCause failure) {
        Objects.requireNonNull(operationName, "operationName");
        Objects.requireNonNull(failure, "failure");

        return switch (failure) {
            case FailureCause.Domain(var error) -> """
                FAIL: %s
                  type=DOMAIN
                  code=%s
                  message=%s
                """.formatted(operationName, error.code(), error.userMessage());

            case AccessDenied denied -> """
                FAIL: %s
                  type=ACCESS_DENIED
                  actor=%s
                  project=%s
                  role=%s
                  operation=%s
                """.formatted(
                    operationName,
                    denied.actorId(),
                    denied.projectId(),
                    denied.role(),
                    denied.operation()
            );
        };
    }
}
