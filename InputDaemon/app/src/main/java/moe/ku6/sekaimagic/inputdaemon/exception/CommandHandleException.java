package moe.ku6.sekaimagic.inputdaemon.exception;

import lombok.Getter;

public class CommandHandleException extends RuntimeException {
    @Getter
    private final int code;

    public CommandHandleException(int code, String message) {
        super(message);
        this.code = code;
    }

    public CommandHandleException(String message) {
        this(17, message);
    }
}
