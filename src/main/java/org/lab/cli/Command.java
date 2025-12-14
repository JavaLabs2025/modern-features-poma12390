package org.lab.cli;

import java.time.LocalDate;
import java.util.Objects;

public sealed interface Command permits
        Command.Register,
        Command.CreateProject,
        Command.AddDev,
        Command.CreateMilestone,
        Command.ActivateMilestone,
        Command.CreateTicket,
        Command.AssignTicket,
        Command.StartTicket,
        Command.DoneTicket,
        Command.CreateBug,
        Command.FixBug,
        Command.TestBug,
        Command.CloseBug,
        Command.Dashboard {

    record Register(String login, String displayName) implements Command {
        public Register {
            Objects.requireNonNull(login, "login");
            Objects.requireNonNull(displayName, "displayName");
        }
    }

    record CreateProject(String actorLogin, String name, String description) implements Command {
        public CreateProject {
            Objects.requireNonNull(actorLogin, "actorLogin");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(description, "description");
        }
    }

    /**
     * Команда названа AddDev по ТЗ, но поддерживает добавление DEVELOPER и TESTER через поле role.
     * role: "DEVELOPER" | "TESTER"
     */
    record AddDev(String actorLogin, String projectRef, String memberLogin, String role) implements Command {
        public AddDev {
            Objects.requireNonNull(actorLogin, "actorLogin");
            Objects.requireNonNull(projectRef, "projectRef");
            Objects.requireNonNull(memberLogin, "memberLogin");
            Objects.requireNonNull(role, "role");
        }
    }

    record CreateMilestone(String actorLogin, String projectRef, String name, LocalDate start, LocalDate end) implements Command {
        public CreateMilestone {
            Objects.requireNonNull(actorLogin, "actorLogin");
            Objects.requireNonNull(projectRef, "projectRef");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(start, "start");
            Objects.requireNonNull(end, "end");
        }
    }

    record ActivateMilestone(String actorLogin, String projectRef, String milestoneRef) implements Command {
        public ActivateMilestone {
            Objects.requireNonNull(actorLogin, "actorLogin");
            Objects.requireNonNull(projectRef, "projectRef");
            Objects.requireNonNull(milestoneRef, "milestoneRef");
        }
    }

    record CreateTicket(String actorLogin, String projectRef, String milestoneRef, String title, String description) implements Command {
        public CreateTicket {
            Objects.requireNonNull(actorLogin, "actorLogin");
            Objects.requireNonNull(projectRef, "projectRef");
            Objects.requireNonNull(milestoneRef, "milestoneRef");
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(description, "description");
        }
    }

    record AssignTicket(String actorLogin, String projectRef, String ticketRef, String developerLogin) implements Command {
        public AssignTicket {
            Objects.requireNonNull(actorLogin, "actorLogin");
            Objects.requireNonNull(projectRef, "projectRef");
            Objects.requireNonNull(ticketRef, "ticketRef");
            Objects.requireNonNull(developerLogin, "developerLogin");
        }
    }

    record StartTicket(String actorLogin, String projectRef, String ticketRef) implements Command {
        public StartTicket {
            Objects.requireNonNull(actorLogin, "actorLogin");
            Objects.requireNonNull(projectRef, "projectRef");
            Objects.requireNonNull(ticketRef, "ticketRef");
        }
    }

    record DoneTicket(String actorLogin, String projectRef, String ticketRef) implements Command {
        public DoneTicket {
            Objects.requireNonNull(actorLogin, "actorLogin");
            Objects.requireNonNull(projectRef, "projectRef");
            Objects.requireNonNull(ticketRef, "ticketRef");
        }
    }

    record CreateBug(String actorLogin, String projectRef, String title, String description) implements Command {
        public CreateBug {
            Objects.requireNonNull(actorLogin, "actorLogin");
            Objects.requireNonNull(projectRef, "projectRef");
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(description, "description");
        }
    }

    record FixBug(String actorLogin, String projectRef, String bugRef) implements Command {
        public FixBug {
            Objects.requireNonNull(actorLogin, "actorLogin");
            Objects.requireNonNull(projectRef, "projectRef");
            Objects.requireNonNull(bugRef, "bugRef");
        }
    }

    record TestBug(String actorLogin, String projectRef, String bugRef) implements Command {
        public TestBug {
            Objects.requireNonNull(actorLogin, "actorLogin");
            Objects.requireNonNull(projectRef, "projectRef");
            Objects.requireNonNull(bugRef, "bugRef");
        }
    }

    record CloseBug(String actorLogin, String projectRef, String bugRef) implements Command {
        public CloseBug {
            Objects.requireNonNull(actorLogin, "actorLogin");
            Objects.requireNonNull(projectRef, "projectRef");
            Objects.requireNonNull(bugRef, "bugRef");
        }
    }

    record Dashboard(String actorLogin) implements Command {
        public Dashboard {
            Objects.requireNonNull(actorLogin, "actorLogin");
        }
    }
}
