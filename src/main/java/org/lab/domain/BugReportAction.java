package org.lab.domain;

public sealed interface BugReportAction permits BugReportAction.Fix, BugReportAction.Test, BugReportAction.Close {

    record Fix(UserId actor) implements BugReportAction { }

    record Test(UserId actor) implements BugReportAction { }

    record Close(UserId actor) implements BugReportAction { }
}
