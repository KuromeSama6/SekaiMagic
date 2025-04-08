package moe.ku6.sekaimagic.input;

import dadb.Dadb;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import moe.ku6.sekaimagic.SekaiMagic;
import moe.ku6.sekaimagic.exception.adb.ADBInitializationConnection;
import moe.ku6.sekaimagic.util.Vec2;
import moe.ku6.sekaimagic.util.Util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

@Slf4j
public class InputDaemon implements Closeable {
    private final Dadb adb;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    @Getter
    private final Vec2 screenResolution;
    @Getter
    private final DaemonWebsocketClient websocketClient;
    public InputDaemon(String host, int port) throws IOException, InterruptedException, URISyntaxException {
        var config = SekaiMagic.getInstance().getConfig();

        log.info("[ADB] Connecting to {}:{}", host, port);
        adb = Dadb.create(host, port);

        log.info("[ADB] Fetching device data, please wait...");

        {
            // device resolution
            var resolutionStr = ExecuteShellBlocking("wm size");
            Pattern pattern = Pattern.compile("(\\d+)x(\\d+)");
            var matcher = pattern.matcher(resolutionStr);

            if (matcher.find()) {
                screenResolution = new Vec2(Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(1)));
                log.info("[ADB] Screen resolution: {}", screenResolution);

            } else {
                throw new ADBInitializationConnection("Failed to get screen resolution");
            }
        }

        {
            // adb permissions
            log.info("[ADB] Checking ADB permissions...");
            String adbUser = ExecuteShellBlocking("whoami");
            if (!adbUser.contains("root")) {
                log.info("[ADB] ADB is not running as root. Attempting to run as root...");
                adb.root();
                adbUser = ExecuteShellBlocking("whoami");
                if (!adbUser.contains("root")) {
                    log.warn("[ADB] Could not root ADB. Some features may not work.");
                }
            }

            if (adbUser.contains("root")) {
                log.info("[ADB] Running as root. Modifying permissions of /dev/uinput to 602 (writeable by all users)");
                ExecuteShellBlocking("chmod 602 /dev/uinput");
            }
        }

        {
            // install
            log.info("[ADB] Installing SekaiMagic Input Daemon...");
            ExecuteShellBlocking("am force-stop moe.ku6.sekaimagic.inputdaemon");

            var overridePath = config.GetString("inputd.daemonApkPath", "");
            File file;

            if (!overridePath.isBlank()) {
                file = new File(overridePath);
                if (!file.exists()) {
                    log.error("Daemon APK not found at {}. Check the path specified in .sekairc.json.", overridePath);
                    throw new IOException("Daemon APK not found at " + overridePath);
                }

            } else {
                var cwd = SekaiMagic.getInstance().getCwd();
                var files = Util.MatchFiles(cwd, "*.apk");
                var apkFile = files.size() == 1 ? files.getFirst() : null;
                if (apkFile == null) {
                    log.error("No input daemon APK found (or found multiple). Please place the Daemon APK in the current working directory or specify the path in .sekairc.json.");
                    throw new IOException("Daemon APK not found");
                }

                file = apkFile;
            }

            log.info("[ADB] Daemon APK found at {}. Installing", file.getAbsolutePath());
            adb.install(file, "-r");
            log.info("[ADB] Daemon APK installed.");
//            Thread.sleep(3000);
            var res = ExecuteShellBlocking("am start -n moe.ku6.sekaimagic.inputdaemon/.InputDaemon");
            if (res.contains("Error")) {
                log.error("[ADB] Failed to start daemon process. Please check the logs for more details.");
                throw new IOException("Failed to start daemon");
            }
        }

        int upstreamPort;
        {
            // port mapping
            upstreamPort = config.GetInt("inputd.forwardPort", 10800);
            log.info("[ADB] Forwarding upstream port {} to device port 7700", upstreamPort);
            try {
                adb.tcpForward(upstreamPort, 7700);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                log.warn("[ADB] Failed to forward port {}. Please check the logs for more details.", upstreamPort);
            }
        }

        {
            // connect to websocket
            Thread.sleep(500);
            log.info("[ADB] Connecting to websocket server...");
            websocketClient = new DaemonWebsocketClient(upstreamPort);
        }

        var autoConnectList = config.GetList("inputd.autoConnect", String.class);
        var addr = "%s:%d".formatted(host, port);
        if (!autoConnectList.contains(addr)) {
            autoConnectList.add(addr);
            config.Set("inputd.autoConnect", autoConnectList);
            SekaiMagic.getInstance().SaveConfig();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executor.shutdown();
            try {
                websocketClient.close();
                adb.close();
            } catch (Exception e) {
                log.error("[ADB] Error closing ADB connection: {}", e.getMessage());
            }
        }));
    }

    public CompletableFuture<String> ExecuteShellAsync(String cmd) {
        return Util.FromFuture(executor.submit(() -> ExecuteShellBlocking(cmd)));
    }

    public CompletableFuture<String[]> ExecuteShellAsync(String... cmd) {
        return Util.FromFuture(executor.submit(() -> ExecuteShellBlocking(cmd)));
    }

    public String ExecuteShellBlocking(String command) {
        try  {
            return adb.shell(command).getAllOutput();

        } catch (Exception e) {
            log.error("[ADB] Error executing shell command: {}", e.getMessage());
            return null;
        }
    }

    public String[] ExecuteShellBlocking(String... commands) {
        var ret = new String[commands.length];
        for (int i = 0; i < commands.length; i++) {
            ret[i] = ExecuteShellBlocking(commands[i]);
        }
        return ret;
    }

    private Vec2 ToScreenTapPosition(double x) {
        var config = SekaiMagic.getInstance().getConfig();
        var range = new Vec2(config.GetList("autoplay.randomization.tapOffsetX", Double.class));
        var offset = range.Random();
        var laneWidth = screenResolution.getX() / 12.0;
        var yRange = new Vec2(config.GetList("autoplay.randomization.tapOffsetY", Double.class));
        var pos = (x + offset) * laneWidth + laneWidth / 2.0;

        return new Vec2(pos + Math.round(Math.random()), screenResolution.getY() * 0.15 + yRange.Random());
    }

    @Override
    public void close() throws IOException {
        if (websocketClient != null) {
            websocketClient.close();
        }

        try {
            if (adb != null) adb.close();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
