package org.lab.app;


import org.lab.domain.enums.ProjectRole;

import java.util.Objects;
import java.util.Optional;

public sealed interface ActorRole permits ActorRole.Manager, ActorRole.TeamLead, ActorRole.Developer, ActorRole.Tester, ActorRole.Outsider {

    String name();

    static ActorRole from(Optional<ProjectRole> roleOpt) {
        Objects.requireNonNull(roleOpt, "roleOpt");
        return roleOpt.map(ActorRole::from).orElseGet(Outsider::new);
    }

    static ActorRole from(ProjectRole role) {
        Objects.requireNonNull(role, "role");
        return switch (role) {
            case MANAGER -> new Manager();
            case TEAM_LEAD -> new TeamLead();
            case DEVELOPER -> new Developer();
            case TESTER -> new Tester();
        };
    }

    record Manager() implements ActorRole {
        @Override public String name() { return "MANAGER"; }
    }

    record TeamLead() implements ActorRole {
        @Override public String name() { return "TEAM_LEAD"; }
    }

    record Developer() implements ActorRole {
        @Override public String name() { return "DEVELOPER"; }
    }

    record Tester() implements ActorRole {
        @Override public String name() { return "TESTER"; }
    }

    record Outsider() implements ActorRole {
        @Override public String name() { return "OUTSIDER"; }
    }
}
