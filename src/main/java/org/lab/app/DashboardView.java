package org.lab.app;

import org.lab.domain.UserId;

import java.util.List;
import java.util.Objects;

public record DashboardView(
        UserId userId,
        List<ProjectView> projects,
        List<TicketView> tickets,
        List<BugReportView> actionableBugs
) {
    public DashboardView {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(projects, "projects");
        Objects.requireNonNull(tickets, "tickets");
        Objects.requireNonNull(actionableBugs, "actionableBugs");
    }
}
