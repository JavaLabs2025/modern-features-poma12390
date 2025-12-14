package org.lab.domain;

import java.util.Objects;
import java.util.UUID;

public record BugReportId(UUID value) {
    public BugReportId {
        Objects.requireNonNull(value, "value");
    }

    public static BugReportId newId() {
        return new BugReportId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
