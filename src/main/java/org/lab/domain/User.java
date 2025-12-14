package org.lab.domain;

import java.time.Instant;
import java.util.Objects;

public record User(
        UserId id,
        String login,
        String displayName,
        Instant registeredAt
) {

    public User { }

    public static DomainResult<User> register(UserId id, String login, String displayName, Instant now) {
        Objects.requireNonNull(id, "id");
        return Validation.nonBlank("login", login)
                .flatMap(l -> Validation.maxLen("login", l, 64))
                .flatMap(l ->
                        Validation.nonBlank("displayName", displayName)
                                .flatMap(n -> Validation.maxLen("displayName", n, 128))
                                .flatMap(nm ->
                                        Validation.nonNullInstant("registeredAt", now)
                                                .map(ts -> new User(id, l, nm, ts))
                                )
                );
    }
}
