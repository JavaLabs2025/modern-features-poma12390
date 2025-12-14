package org.lab.domain;

import java.util.Objects;

public sealed interface DomainError
        permits DomainError.InvalidValue,
        DomainError.NotFound,
        DomainError.Conflict,
        DomainError.InvariantViolation,
        DomainError.InvalidTransition {

    String code();

    String userMessage();

    record InvalidValue(String field, String message) implements DomainError {
        public InvalidValue {
            Objects.requireNonNull(field, "field");
            Objects.requireNonNull(message, "message");
        }

        @Override
        public String code() {
            return "INVALID_VALUE";
        }

        @Override
        public String userMessage() {
            return "Invalid value for '" + field + "': " + message;
        }
    }

    record NotFound(String entity, String id) implements DomainError {
        public NotFound {
            Objects.requireNonNull(entity, "entity");
            Objects.requireNonNull(id, "id");
        }

        @Override
        public String code() {
            return "NOT_FOUND";
        }

        @Override
        public String userMessage() {
            return entity + " not found: " + id;
        }
    }

    record Conflict(String message) implements DomainError {
        public Conflict {
            Objects.requireNonNull(message, "message");
        }

        @Override
        public String code() {
            return "CONFLICT";
        }

        @Override
        public String userMessage() {
            return message;
        }
    }

    record InvariantViolation(String invariant, String message) implements DomainError {
        public InvariantViolation {
            Objects.requireNonNull(invariant, "invariant");
            Objects.requireNonNull(message, "message");
        }

        @Override
        public String code() {
            return "INVARIANT_VIOLATION";
        }

        @Override
        public String userMessage() {
            return invariant + ": " + message;
        }
    }

    record InvalidTransition(String entity, String from, String to, String message) implements DomainError {
        public InvalidTransition {
            Objects.requireNonNull(entity, "entity");
            Objects.requireNonNull(from, "from");
            Objects.requireNonNull(to, "to");
            Objects.requireNonNull(message, "message");
        }

        @Override
        public String code() {
            return "INVALID_TRANSITION";
        }

        @Override
        public String userMessage() {
            return entity + " transition " + from + " -> " + to + " is invalid: " + message;
        }
    }
}
