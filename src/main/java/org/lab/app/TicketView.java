package org.lab.app;


import org.lab.domain.MilestoneId;
import org.lab.domain.ProjectId;
import org.lab.domain.TicketId;
import org.lab.domain.UserId;
import org.lab.domain.enums.TicketStatus;

import java.util.Set;

public record TicketView(
        TicketId id,
        ProjectId projectId,
        MilestoneId milestoneId,
        String title,
        TicketStatus status,
        Set<UserId> assignees
) { }
