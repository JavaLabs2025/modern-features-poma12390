package org.lab.app;

import org.lab.domain.TicketId;
import org.lab.domain.enums.TicketStatus;

public record TicketCompletionView(
        TicketId ticketId,
        TicketStatus status,
        boolean done
) { }
