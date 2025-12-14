package org.lab.domain;

import java.util.Objects;
import java.util.UUID;

public record TicketId(UUID value) {
    public TicketId {
        Objects.requireNonNull(value, "value");
    }

    public static TicketId newId() {
        return new TicketId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
