package org.lab.domain;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public sealed interface DomainResult<T> permits DomainResult.Success, DomainResult.Failure {

    boolean isSuccess();

    default boolean isFailure() {
        return !isSuccess();
    }

    Optional<T> toOptional();

    T orElseThrow();

    DomainError errorOrNull();

    <U> DomainResult<U> map(Function<? super T, ? extends U> mapper);

    <U> DomainResult<U> flatMap(Function<? super T, ? extends DomainResult<U>> mapper);

    static <T> DomainResult<T> ok(T value) {
        return new Success<>(value);
    }

    static <T> DomainResult<T> err(DomainError error) {
        return new Failure<>(Objects.requireNonNull(error, "error"));
    }

    record Success<T>(T value) implements DomainResult<T> {
        public Success {
            Objects.requireNonNull(value, "value");
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public Optional<T> toOptional() {
            return Optional.of(value);
        }

        @Override
        public T orElseThrow() {
            return value;
        }

        @Override
        public DomainError errorOrNull() {
            return null;
        }

        @Override
        public <U> DomainResult<U> map(Function<? super T, ? extends U> mapper) {
            Objects.requireNonNull(mapper, "mapper");
            return DomainResult.ok(mapper.apply(value));
        }

        @Override
        public <U> DomainResult<U> flatMap(Function<? super T, ? extends DomainResult<U>> mapper) {
            Objects.requireNonNull(mapper, "mapper");
            return Objects.requireNonNull(mapper.apply(value), "mapper result");
        }
    }

    record Failure<T>(DomainError error) implements DomainResult<T> {
        public Failure {
            Objects.requireNonNull(error, "error");
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public Optional<T> toOptional() {
            return Optional.empty();
        }

        @Override
        public T orElseThrow() {
            throw new IllegalStateException("DomainResult is failure: " + error.userMessage());
        }

        @Override
        public DomainError errorOrNull() {
            return error;
        }

        @Override
        public <U> DomainResult<U> map(Function<? super T, ? extends U> mapper) {
            Objects.requireNonNull(mapper, "mapper");
            return DomainResult.err(error);
        }

        @Override
        public <U> DomainResult<U> flatMap(Function<? super T, ? extends DomainResult<U>> mapper) {
            Objects.requireNonNull(mapper, "mapper");
            return DomainResult.err(error);
        }
    }
}
