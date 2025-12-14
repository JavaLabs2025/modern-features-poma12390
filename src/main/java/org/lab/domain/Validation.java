package org.lab.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

final class Validation {

    private Validation() { }

    static DomainResult<String> nonBlank(String field, String value) {
        if (value == null) {
            return DomainResult.err(new DomainError.InvalidValue(field, "must not be null"));
        }
        var v = value.trim();
        if (v.isEmpty()) {
            return DomainResult.err(new DomainError.InvalidValue(field, "must not be blank"));
        }
        return DomainResult.ok(v);
    }

    static DomainResult<String> maxLen(String field, String value, int max) {
        Objects.requireNonNull(value, field);
        if (value.length() > max) {
            return DomainResult.err(new DomainError.InvalidValue(field, "length must be <= " + max));
        }
        return DomainResult.ok(value);
    }

    static DomainResult<Instant> nonNullInstant(String field, Instant value) {
        if (value == null) {
            return DomainResult.err(new DomainError.InvalidValue(field, "must not be null"));
        }
        return DomainResult.ok(value);
    }

    static DomainResult<LocalDate> nonNullDate(String field, LocalDate value) {
        if (value == null) {
            return DomainResult.err(new DomainError.InvalidValue(field, "must not be null"));
        }
        return DomainResult.ok(value);
    }
}
