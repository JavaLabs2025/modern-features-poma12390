package org.lab.app;

import java.util.EnumSet;
import java.util.Set;

public final class AccessControl {

    private static final Set<Operation> MANAGER = EnumSet.of(
            Operation.ASSIGN_TEAM_LEAD,
            Operation.ADD_DEVELOPER,
            Operation.ADD_TESTER,
            Operation.CREATE_MILESTONE,
            Operation.ACTIVATE_MILESTONE,
            Operation.CLOSE_MILESTONE,
            Operation.CREATE_TICKET,
            Operation.ASSIGN_TICKET_DEVELOPER,
            Operation.CHECK_TICKET_COMPLETION,
            Operation.CLOSE_BUG_REPORT
    );

    private static final Set<Operation> TEAM_LEAD = EnumSet.of(
            Operation.CREATE_TICKET,
            Operation.ASSIGN_TICKET_DEVELOPER,
            Operation.CHECK_TICKET_COMPLETION,
            Operation.TICKET_ACCEPT,
            Operation.TICKET_START,
            Operation.TICKET_COMPLETE
    );

    private static final Set<Operation> DEVELOPER = EnumSet.of(
            Operation.TICKET_ACCEPT,
            Operation.TICKET_START,
            Operation.TICKET_COMPLETE,
            Operation.CREATE_BUG_REPORT,
            Operation.FIX_BUG_REPORT
    );

    private static final Set<Operation> TESTER = EnumSet.of(
            Operation.CREATE_BUG_REPORT,
            Operation.TEST_BUG_REPORT,
            Operation.CLOSE_BUG_REPORT
    );

    private AccessControl() { }

    public static boolean isAllowed(ActorRole role, Operation operation) {
        return switch (role) {
            case ActorRole.Manager ignored -> MANAGER.contains(operation);
            case ActorRole.TeamLead ignored -> TEAM_LEAD.contains(operation);
            case ActorRole.Developer ignored -> DEVELOPER.contains(operation);
            case ActorRole.Tester ignored -> TESTER.contains(operation);
            case ActorRole.Outsider ignored -> false;
        };
    }
}
