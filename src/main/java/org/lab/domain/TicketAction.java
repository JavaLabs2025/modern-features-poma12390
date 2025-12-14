package org.lab.domain;

public sealed interface TicketAction permits TicketAction.Accept, TicketAction.Start, TicketAction.Complete {

    record Accept(UserId actor) implements TicketAction { }

    record Start(UserId actor) implements TicketAction { }

    record Complete(UserId actor) implements TicketAction { }
}
