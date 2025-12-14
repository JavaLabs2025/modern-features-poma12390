package org.lab.cli;

import org.lab.app.ProjectManagementService;
import org.lab.app.Result;
import org.lab.infra.BugReportRepository;
import org.lab.infra.ProjectRepository;
import org.lab.infra.TicketRepository;
import org.lab.infra.UserRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class CliMain {

    /**
     * Modern Java:
     * - Расширенная стандартная библиотека: сборка приложения без фреймворков, wiring слоёв вручную.
     * - Демонстрирует in-memory инфраструктуру (ConcurrentHashMap в репозиториях) и запуск CLI без сервера/БД.
     */
    public static void main(String[] args) throws Exception {
        var users = new UserRepository();
        var projects = new ProjectRepository();
        var tickets = new TicketRepository();
        var bugs = new BugReportRepository();

        var service = new ProjectManagementService(users, projects, tickets, bugs);
        var state = new CliState();
        var runner = new CliRunner(service, users, projects, state);

        System.out.println("=== Project Management CLI (Java 26) ===");
        System.out.println("Type: help | demo | exit");
        System.out.println();

        boolean ranDemo = false;
        for (var a : args) {
            if ("--demo".equalsIgnoreCase(a)) {
                runDemo(runner);
                ranDemo = true;
                break;
            }
        }

        if (!ranDemo) {
            runDemo(runner);
        }

        runRepl(runner);
    }

    /**
     * Modern Java:
     * - Try-with-resources: корректное управление ресурсами ввода (BufferedReader).
     * - StandardCharsets.UTF_8: явная кодировка ввода из стандартной библиотеки (важно для Windows/консоли).
     */
    private static void runRepl(CliRunner runner) throws Exception {
        try (var br = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            while (true) {
                System.out.print("> ");
                var line = br.readLine();
                if (line == null) {
                    System.out.println();
                    return;
                }
                var trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                if ("exit".equalsIgnoreCase(trimmed) || "quit".equalsIgnoreCase(trimmed)) {
                    return;
                }
                if ("help".equalsIgnoreCase(trimmed)) {
                    printHelp();
                    continue;
                }
                if ("demo".equalsIgnoreCase(trimmed)) {
                    runDemo(runner);
                    continue;
                }

                executeLine(runner, trimmed);
            }
        }
    }

    /**
     * Modern Java:
     * - Pattern matching for switch: switch по sealed Parsed (Ok/Error) с record-pattern’ами
     *   (case Parsed.Ok(var cmd) -> ..., case Parsed.Error(var msg) -> ...).
     * - Sealed Result: печатает успех/ошибку, не используя исключения как бизнес-модель.
     */
    private static void executeLine(CliRunner runner, String line) {
        var parsed = CommandParser.parse(line);
        switch (parsed) {
            case CommandParser.Parsed.Ok(var cmd) -> {
                var result = runner.execute(cmd);

                if (result.isSuccess()) {
                    System.out.println(result.toOptional().orElse(""));
                } else {
                    System.out.println(String.valueOf(result.failureOrNull()));
                }
                System.out.println();
                System.out.flush();

            }
            case CommandParser.Parsed.Error(var msg) -> System.out.println("Parse error: " + msg);
        }
        System.out.println();
    }

    /**
     * Modern Java:
     * - Text Blocks ("""..."""): многострочный help без конкатенации и “\n” по всему коду.
     */
    private static void runDemo(CliRunner runner) {
        System.out.println("=== DEMO сценарий ===");

        executeLine(runner, "register manager \"Project Manager\"");
        executeLine(runner, "register dev \"Backend Developer\"");
        executeLine(runner, "register tester \"QA Tester\"");

        executeLine(runner, "create-project manager \"Demo Project\" \"Project created from CLI demo\"");
        executeLine(runner, "dashboard manager");

        executeLine(runner, "add-dev manager lastProject dev");
        executeLine(runner, "add-tester manager lastProject tester");

        executeLine(runner, "create-milestone manager lastProject \"Milestone 1\" 2025-12-14 2025-12-21");
        executeLine(runner, "activate-milestone manager lastProject lastMilestone");

        executeLine(runner, "create-ticket manager lastProject lastMilestone \"Implement feature\" \"Do the implementation\"");
        executeLine(runner, "assign-ticket manager lastProject lastTicket dev");

        executeLine(runner, "start-ticket dev lastProject lastTicket");
        executeLine(runner, "done-ticket dev lastProject lastTicket");

        executeLine(runner, "create-bug tester lastProject \"Bug #1\" \"Found defect during testing\"");
        executeLine(runner, "fix-bug dev lastProject lastBug");
        executeLine(runner, "test-bug tester lastProject lastBug");
        executeLine(runner, "close-bug tester lastProject lastBug");

        executeLine(runner, "dashboard manager");

        System.out.println("=== DEMO завершён ===");
        System.out.println();
    }

    private static void printHelp() {
        System.out.println("""
            Commands:
              register <login> "Display Name"

              create-project <actorLogin> "Project Name" "Description"
                projectRef can be: project KEY (e.g. PRJ-000001) | UUID | lastProject | last

              add-dev <actorLogin> <projectRef> <memberLogin>
              add-tester <actorLogin> <projectRef> <memberLogin>

              create-milestone <actorLogin> <projectRef> "Milestone Name" <start yyyy-mm-dd> <end yyyy-mm-dd>
              activate-milestone <actorLogin> <projectRef> <milestoneRef>
                milestoneRef: UUID | lastMilestone | last

              create-ticket <actorLogin> <projectRef> <milestoneRef> "Title" "Description"
              assign-ticket <actorLogin> <projectRef> <ticketRef> <developerLogin>
              start-ticket <actorLogin> <projectRef> <ticketRef>
              done-ticket <actorLogin> <projectRef> <ticketRef>
                ticketRef: UUID | lastTicket | last

              create-bug <actorLogin> <projectRef> "Title" "Description"
              fix-bug <actorLogin> <projectRef> <bugRef>
              test-bug <actorLogin> <projectRef> <bugRef>
              close-bug <actorLogin> <projectRef> <bugRef>
                bugRef: UUID | lastBug | last

              dashboard <actorLogin>

            Meta:
              demo | help | exit
            """);
    }
}
