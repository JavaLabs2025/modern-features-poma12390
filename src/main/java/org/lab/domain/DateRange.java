package org.lab.domain;

import java.time.LocalDate;
import java.util.Objects;

public record DateRange(LocalDate start, LocalDate end) {

    public DateRange {
        // constructor kept private via factories
    }

    public static DomainResult<DateRange> of(LocalDate start, LocalDate end) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (start.isAfter(end)) {
            return DomainResult.err(new DomainError.InvalidValue(
                    "dateRange",
                    "start must be <= end"
            ));
        }
        return DomainResult.ok(new DateRange(start, end));
    }
}
