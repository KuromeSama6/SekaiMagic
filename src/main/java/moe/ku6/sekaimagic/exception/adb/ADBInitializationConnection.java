package moe.ku6.sekaimagic.exception.adb;

import java.io.IOException;

public class ADBInitializationConnection extends IOException {
    public ADBInitializationConnection(String message) {
        super(message);
    }
}
