package moe.ku6.sekaimagic.adb;

import lombok.Getter;
import moe.ku6.sekaimagic.SekaiMagic;
import moe.ku6.sekaimagic.input.GuideSoundPlayer;
import se.vidstige.jadb.ConnectionToRemoteDeviceException;
import se.vidstige.jadb.JadbException;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import static org.reflections.Reflections.log;

public class ADBManager {
    @Getter
    private static ADBManager instance;
    @Getter
    private final List<ADBConnector> connectors = new ArrayList<>();

    public ADBManager() {
        if (instance != null)
            throw new IllegalStateException("ADBManager already initialized");
        instance = this;

        // auto adb connect
        var autoConnect = SekaiMagic.getInstance().getConfig().GetList("adb.autoConnect", String.class);
        if (autoConnect.isEmpty()) {
            log.warn("No ADB connect addresses found in config. Autoplay will not work without connecting to ADB. Use `adbc -c <ip>:<port>` to add and connect one.");
        }

        for (var addr : autoConnect) {
            var ip = addr.split(":")[0];
            var port = Integer.parseInt(addr.split(":")[1]);
            Connect(ip, port);
        }
    }

    public void Connect(String ip, int port) {
        try {
            var connector = new ADBConnector(ip, port);
            connectors.add(connector);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
