package org.lab.app;


import org.lab.domain.BugReportId;
import org.lab.domain.ProjectId;
import org.lab.domain.UserId;
import org.lab.domain.enums.BugStatus;

public record BugReportView(
        BugReportId id,
        ProjectId projectId,
        String title,
        BugStatus status,
        UserId assignedTo
) { }
