package org.lab.cli;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CommandParser {

    private CommandParser() {
    }

    public sealed interface Parsed permits Parsed.Ok, Parsed.Error {
        record Ok(Command command) implements Parsed { }
        record Error(String message) implements Parsed { }
    }

    /**
     * Modern Java:
     * - Sealed interface + records: Parsed = Ok(Command) | Error(message) как типизированный результат парсинга.
     * - Switch expression: диспетчеризация по имени команды через switch -> (без цепочек if/else).
     */
    public static Parsed parse(String line) {
        if (line == null) {
            return new Parsed.Error("Empty input");
        }
        var trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return new Parsed.Error("Empty input");
        }

        List<String> tokens;
        try {
            tokens = tokenize(trimmed);
        } catch (IllegalArgumentException e) {
            return new Parsed.Error(e.getMessage());
        }

        if (tokens.isEmpty()) {
            return new Parsed.Error("Empty input");
        }

        var cmd = tokens.get(0).toLowerCase();

        try {
            return switch (cmd) {
                case "register" -> parseRegister(tokens);
                case "create-project" -> parseCreateProject(tokens);

                case "add-dev" -> parseAddMember(tokens, "DEVELOPER");
                case "add-tester" -> parseAddMember(tokens, "TESTER");

                case "create-milestone" -> parseCreateMilestone(tokens);
                case "activate-milestone" -> parseActivateMilestone(tokens);

                case "create-ticket" -> parseCreateTicket(tokens);
                case "assign-ticket" -> parseAssignTicket(tokens);

                case "start-ticket" -> parseStartTicket(tokens);
                case "done-ticket" -> parseDoneTicket(tokens);

                case "create-bug" -> parseCreateBug(tokens);
                case "fix-bug" -> parseFixBug(tokens);
                case "test-bug" -> parseTestBug(tokens);
                case "close-bug" -> parseCloseBug(tokens);

                case "dashboard" -> parseDashboard(tokens);

                default -> new Parsed.Error("Unknown command: " + tokens.get(0));
            };
        } catch (IllegalArgumentException e) {
            return new Parsed.Error(e.getMessage());
        }
    }

    private static Parsed parseRegister(List<String> t) {
        requireSize(t, 3, "register <login> \"Display Name\"");
        return new Parsed.Ok(new Command.Register(t.get(1), t.get(2)));
    }

    private static Parsed parseCreateProject(List<String> t) {
        requireSize(t, 4, "create-project <actorLogin> \"Project Name\" \"Description\"");
        return new Parsed.Ok(new Command.CreateProject(t.get(1), t.get(2), t.get(3)));
    }

    private static Parsed parseAddMember(List<String> t, String role) {
        requireSize(t, 4, "add-dev/add-tester <actorLogin> <projectRef> <memberLogin>");
        return new Parsed.Ok(new Command.AddDev(t.get(1), t.get(2), t.get(3), role));
    }

    private static Parsed parseCreateMilestone(List<String> t) {
        requireSize(t, 6, "create-milestone <actorLogin> <projectRef> \"Milestone Name\" <start yyyy-mm-dd> <end yyyy-mm-dd>");
        var start = parseDate(t.get(4), "start");
        var end = parseDate(t.get(5), "end");
        return new Parsed.Ok(new Command.CreateMilestone(t.get(1), t.get(2), t.get(3), start, end));
    }

    private static Parsed parseActivateMilestone(List<String> t) {
        requireSize(t, 4, "activate-milestone <actorLogin> <projectRef> <milestoneRef>");
        return new Parsed.Ok(new Command.ActivateMilestone(t.get(1), t.get(2), t.get(3)));
    }

    private static Parsed parseCreateTicket(List<String> t) {
        requireSize(t, 6, "create-ticket <actorLogin> <projectRef> <milestoneRef> \"Title\" \"Description\"");
        return new Parsed.Ok(new Command.CreateTicket(t.get(1), t.get(2), t.get(3), t.get(4), t.get(5)));
    }

    private static Parsed parseAssignTicket(List<String> t) {
        requireSize(t, 5, "assign-ticket <actorLogin> <projectRef> <ticketRef> <developerLogin>");
        return new Parsed.Ok(new Command.AssignTicket(t.get(1), t.get(2), t.get(3), t.get(4)));
    }

    private static Parsed parseStartTicket(List<String> t) {
        requireSize(t, 4, "start-ticket <actorLogin> <projectRef> <ticketRef>");
        return new Parsed.Ok(new Command.StartTicket(t.get(1), t.get(2), t.get(3)));
    }

    private static Parsed parseDoneTicket(List<String> t) {
        requireSize(t, 4, "done-ticket <actorLogin> <projectRef> <ticketRef>");
        return new Parsed.Ok(new Command.DoneTicket(t.get(1), t.get(2), t.get(3)));
    }

    private static Parsed parseCreateBug(List<String> t) {
        requireSize(t, 5, "create-bug <actorLogin> <projectRef> \"Title\" \"Description\"");
        return new Parsed.Ok(new Command.CreateBug(t.get(1), t.get(2), t.get(3), t.get(4)));
    }

    private static Parsed parseFixBug(List<String> t) {
        requireSize(t, 4, "fix-bug <actorLogin> <projectRef> <bugRef>");
        return new Parsed.Ok(new Command.FixBug(t.get(1), t.get(2), t.get(3)));
    }

    private static Parsed parseTestBug(List<String> t) {
        requireSize(t, 4, "test-bug <actorLogin> <projectRef> <bugRef>");
        return new Parsed.Ok(new Command.TestBug(t.get(1), t.get(2), t.get(3)));
    }

    private static Parsed parseCloseBug(List<String> t) {
        requireSize(t, 4, "close-bug <actorLogin> <projectRef> <bugRef>");
        return new Parsed.Ok(new Command.CloseBug(t.get(1), t.get(2), t.get(3)));
    }

    private static Parsed parseDashboard(List<String> t) {
        requireSize(t, 2, "dashboard <actorLogin>");
        return new Parsed.Ok(new Command.Dashboard(t.get(1)));
    }

    private static LocalDate parseDate(String raw, String field) {
        Objects.requireNonNull(raw, field);
        try {
            return LocalDate.parse(raw);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date for " + field + ": " + raw + " (expected yyyy-mm-dd)");
        }
    }

    private static void requireSize(List<String> t, int expected, String usage) {
        if (t.size() != expected) {
            throw new IllegalArgumentException("Invalid arguments. Usage: " + usage);
        }
    }

    /**
     * Modern Java:
     * - Расширенная стандартная библиотека: StringBuilder + посимвольный разбор + Character.isWhitespace().
     * - Поддерживает quoted-аргументы "..." и escaping внутри кавычек без внешних зависимостей.
     */

    private static List<String> tokenize(String input) {
        Objects.requireNonNull(input, "input");
        var out = new ArrayList<String>();

        var sb = new StringBuilder();
        boolean inQuotes = false;
        boolean escaping = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (escaping) {
                sb.append(c);
                escaping = false;
                continue;
            }

            if (inQuotes && c == '\\') {
                escaping = true;
                continue;
            }

            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }

            if (!inQuotes && Character.isWhitespace(c)) {
                if (!sb.isEmpty()) {
                    out.add(sb.toString());
                    sb.setLength(0);
                }
                continue;
            }

            sb.append(c);
        }

        if (escaping) {
            throw new IllegalArgumentException("Invalid escaping in input");
        }
        if (inQuotes) {
            throw new IllegalArgumentException("Unclosed quotes in input");
        }
        if (!sb.isEmpty()) {
            out.add(sb.toString());
        }
        return out;
    }
}
