package org.lab.app;

public enum Operation {
    // Project users management
    ASSIGN_TEAM_LEAD,
    ADD_DEVELOPER,
    ADD_TESTER,

    // Milestones
    CREATE_MILESTONE,
    ACTIVATE_MILESTONE,
    CLOSE_MILESTONE,

    // Tickets management
    CREATE_TICKET,
    ASSIGN_TICKET_DEVELOPER,
    CHECK_TICKET_COMPLETION,

    // Tickets execution
    TICKET_ACCEPT,
    TICKET_START,
    TICKET_COMPLETE,

    // Bugs
    CREATE_BUG_REPORT,
    FIX_BUG_REPORT,
    TEST_BUG_REPORT,
    CLOSE_BUG_REPORT
}
