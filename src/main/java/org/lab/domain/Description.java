package org.lab.domain;

public record Description(String value) {

    private static final int MAX = 4000;

    public Description {
        // constructor kept private via factories
    }

    public static DomainResult<Description> of(String raw) {
        if (raw == null) {
            return DomainResult.ok(new Description(""));
        }
        var v = raw.trim();
        return Validation.maxLen("description", v, MAX).map(Description::new);
    }
}
