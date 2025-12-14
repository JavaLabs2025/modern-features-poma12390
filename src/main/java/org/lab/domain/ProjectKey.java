package org.lab.domain;

public record ProjectKey(String value) {

    private static final int MAX = 32;

    public ProjectKey { }

    public static DomainResult<ProjectKey> of(String raw) {
        return Validation.nonBlank("projectKey", raw)
                .flatMap(v -> Validation.maxLen("projectKey", v, MAX))
                .map(ProjectKey::new);
    }
}
