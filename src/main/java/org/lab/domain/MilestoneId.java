package org.lab.domain;

import java.util.Objects;
import java.util.UUID;

public record MilestoneId(UUID value) {
    public MilestoneId {
        Objects.requireNonNull(value, "value");
    }

    public static MilestoneId newId() {
        return new MilestoneId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
