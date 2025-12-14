package org.lab.app;

import org.lab.domain.DomainError;

import java.util.Objects;
import java.util.Optional;

public sealed interface FailureCause permits FailureCause.Domain, AccessDenied {

    String code();

    String message();

    default Optional<DomainError> asDomainError() {
        return switch (this) {
            case Domain(var err) -> Optional.of(err);
            case AccessDenied ignored -> Optional.empty();
        };
    }

    record Domain(DomainError error) implements FailureCause {
        public Domain {
            Objects.requireNonNull(error, "error");
        }

        @Override
        public String code() {
            return error.code();
        }

        @Override
        public String message() {
            return error.userMessage();
        }
    }
}
