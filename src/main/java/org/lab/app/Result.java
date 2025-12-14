package org.lab.app;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public sealed interface Result<T> permits Result.Success, Result.Failure {

    boolean isSuccess();

    default boolean isFailure() {
        return !isSuccess();
    }

    Optional<T> toOptional();

    FailureCause failureOrNull();

    static <T> Result<T> ok(T value) {
        return new Success<>(Objects.requireNonNull(value, "value"));
    }

    static <T> Result<T> fail(FailureCause cause) {
        return new Failure<>(Objects.requireNonNull(cause, "cause"));
    }

    @SuppressWarnings("unchecked")
    default <U> U match(Function<? super T, ? extends U> onSuccess,
                        Function<? super FailureCause, ? extends U> onFailure) {
        Objects.requireNonNull(onSuccess, "onSuccess");
        Objects.requireNonNull(onFailure, "onFailure");

        // pattern matching switch №1 (record patterns over sealed Result)
        return switch (this) {
            case Success(var value) -> onSuccess.apply((T) value);
            case Failure(var cause) -> onFailure.apply(cause);
        };
    }

    default <U> Result<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return match(
                v -> Result.ok(Objects.requireNonNull(mapper.apply(v), "mapper result")),
                Result::fail
        );
    }

    default <U> Result<U> flatMap(Function<? super T, ? extends Result<U>> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return match(
                v -> Objects.requireNonNull(mapper.apply(v), "mapper result"),
                Result::fail
        );
    }

    record Success<T>(T value) implements Result<T> {
        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public Optional<T> toOptional() {
            return Optional.of(value);
        }

        @Override
        public FailureCause failureOrNull() {
            return null;
        }
    }

    record Failure<T>(FailureCause cause) implements Result<T> {
        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public Optional<T> toOptional() {
            return Optional.empty();
        }

        @Override
        public FailureCause failureOrNull() {
            return cause;
        }
    }
}
