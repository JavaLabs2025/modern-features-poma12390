package org.lab.app;

import org.lab.domain.ProjectId;
import org.lab.domain.UserId;

import java.util.Objects;

public record AccessDenied(
        UserId actorId,
        ProjectId projectId,
        String operation,
        String role
) implements FailureCause {

    public AccessDenied {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(role, "role");
    }

    @Override
    public String code() {
        return "ACCESS_DENIED";
    }

    @Override
    public String message() {
        return "Access denied: role=" + role + ", op=" + operation + ", actor=" + actorId + ", project=" + projectId;
    }
}
