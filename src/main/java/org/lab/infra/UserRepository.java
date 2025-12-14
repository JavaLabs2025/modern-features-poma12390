package org.lab.infra;

import org.lab.domain.DomainError;
import org.lab.domain.DomainResult;
import org.lab.domain.User;
import org.lab.domain.UserId;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public final class UserRepository {

    private final ConcurrentHashMap<UserId, User> byId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UserId> idByLogin = new ConcurrentHashMap<>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public UserId nextId() {
        return UserId.newId();
    }

    /**
     * Modern Java:
     * - java.util.concurrent: ConcurrentHashMap + ReadWriteLock для потокобезопасного in-memory репозитория.
     * - Функциональный стиль: update принимает Function<User, DomainResult<User>> как “транзакционную” функцию обновления.
     * - Возвращает DomainResult (типизированная ошибка), вместо исключений для доменных конфликтов/инвариантов.
     */


    public DomainResult<User> insert(User user) {
        Objects.requireNonNull(user, "user");

        lock.writeLock().lock();
        try {
            var existingId = idByLogin.get(user.login());
            if (existingId != null && !existingId.equals(user.id())) {
                return DomainResult.err(new DomainError.Conflict("Login already exists: " + user.login()));
            }
            if (byId.containsKey(user.id())) {
                return DomainResult.err(new DomainError.Conflict("User already exists: " + user.id()));
            }

            idByLogin.put(user.login(), user.id());
            byId.put(user.id(), user);
            return DomainResult.ok(user);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Optional<User> findById(UserId id) {
        Objects.requireNonNull(id, "id");
        return Optional.ofNullable(byId.get(id));
    }

    public Optional<User> findByLogin(String login) {
        Objects.requireNonNull(login, "login");
        lock.readLock().lock();
        try {
            var id = idByLogin.get(login);
            if (id == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(byId.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

}
