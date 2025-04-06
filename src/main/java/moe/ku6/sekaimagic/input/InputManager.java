package moe.ku6.sekaimagic.input;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import moe.ku6.sekaimagic.SekaiMagic;
import org.jline.jansi.Ansi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.reflections.Reflections.log;

@Slf4j
public class InputManager {
    @Getter
    private static InputManager instance;
    @Getter
    private final List<InputDaemon> daemons = new ArrayList<>();

    public InputManager() {
        if (instance != null)
            throw new IllegalStateException("InputManager already initialized");
        instance = this;
        var config = SekaiMagic.getInstance().getConfig();
        var autoConnect = config.GetList("inputd.autoConnect", String.class);

        if (!config.GetBool("inputd.skipSetupWizard") && autoConnect.isEmpty()) {
            log.warn("No ADB/Daemon connection addresses found. Enter the <IP>:<Port> of an ADB server to set one up now. (You can skip this by setting `inputd.skipSetupWizard` to true in the config)");
            RunSetupWizard();
        }

        // auto adb connect
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
            var daemon = new InputDaemon(ip, port);
            daemons.add(daemon);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void RunSetupWizard() {
        var config = SekaiMagic.getInstance().getConfig();
        log.info(Ansi.ansi().bg(Ansi.Color.RED).fg(Ansi.Color.WHITE).a("\n\nWarning - We strongly recommend you to use an emulator device instead of your real one. The setup process involves rooting ADB and/or your device, which bring forth unexpected damage. If you do not wish to continue, hit enter without entering an address.\n").reset().toString());

        String ip;
        int port;
        while (true) {
            var address = SekaiMagic.getInstance().getConsole().ReadBlocking("Enter the address of the ADB server (IP:Port): ");
            if (!address.isEmpty()) {
                var args = address.split(":");
                if (args.length != 2) {
                    log.error("Invalid address format. Expected <IP>:<Port>. To quit the setup wizard, hit enter.");
                    continue;
                }

                ip = args[0];
                port = Integer.parseInt(args[1]);

                if (port < 0 || port > 65535) {
                    log.error("Invalid port number. Expected 0-65535.");
                    continue;
                }

                break;

            } else {
                log.warn("No address entered. Skipping setup.");
                return;
            }
        }

        try {
            var daemon = new InputDaemon(ip, port);
        } catch (Exception e) {
            log.error("Failed to connect to ADB server: {}", e.getMessage());
            e.printStackTrace();
        }
    }
}
