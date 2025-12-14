package org.lab.domain;

public record Title(String value) {

    private static final int MAX = 200;

    public Title {
        // constructor kept private via factories
    }

    public static DomainResult<Title> of(String raw) {
        return Validation.nonBlank("title", raw)
                .flatMap(v -> Validation.maxLen("title", v, MAX))
                .map(Title::new);
    }
}
