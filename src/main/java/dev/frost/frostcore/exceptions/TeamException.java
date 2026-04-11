package dev.frost.frostcore.exceptions;

import dev.frost.frostcore.teams.TeamError;
import lombok.Getter;

public class TeamException extends Exception {

    @Getter
    private final TeamError error;

    public TeamException(TeamError error, String message) {
        super(message);
        this.error = error;
    }
}
