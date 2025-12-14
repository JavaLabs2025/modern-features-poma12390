package org.lab.app;

import org.lab.domain.UserId;

public record UserView(
        UserId id,
        String login,
        String displayName
) { }
