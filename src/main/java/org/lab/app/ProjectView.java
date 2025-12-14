package org.lab.app;

import org.lab.domain.ProjectId;
import org.lab.domain.UserId;

public record ProjectView(
        ProjectId id,
        String key,
        String name,
        UserId managerId,
        UserId teamLeadId,
        String myRole,
        int milestonesCount,
        int ticketsCount,
        int bugReportsCount
) { }
