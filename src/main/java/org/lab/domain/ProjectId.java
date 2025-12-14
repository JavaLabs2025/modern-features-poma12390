package org.lab.domain;

import java.util.Objects;
import java.util.UUID;

public record ProjectId(UUID value) {
    public ProjectId {
        Objects.requireNonNull(value, "value");
    }

    public static ProjectId newId() {
        return new ProjectId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
