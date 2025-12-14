package org.lab.app;

import org.lab.domain.MilestoneId;
import org.lab.domain.ProjectId;
import org.lab.domain.enums.MilestoneStatus;

import java.time.LocalDate;

public record MilestoneView(
        MilestoneId id,
        ProjectId projectId,
        String name,
        LocalDate start,
        LocalDate end,
        MilestoneStatus status
) { }
