package moe.ku6.sekaimagic.exception.sus;

public class InvalidRequestAttributeException extends SUSParseException {
    public InvalidRequestAttributeException(String key, String value, String message) {
        super("Invalid value for request [%s]: %s: %s".formatted(key, value, message));
    }
}
