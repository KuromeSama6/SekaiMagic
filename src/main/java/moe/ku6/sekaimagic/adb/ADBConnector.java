package moe.ku6.sekaimagic.adb;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import moe.ku6.sekaimagic.SekaiMagic;
import moe.ku6.sekaimagic.exception.adb.ADBInitializationConnection;
import moe.ku6.sekaimagic.input.IInputDevice;
import moe.ku6.sekaimagic.util.Vec2;
import moe.ku6.sekaimagic.util.Util;
import se.vidstige.jadb.ConnectionToRemoteDeviceException;
import se.vidstige.jadb.JadbConnection;
import se.vidstige.jadb.JadbDevice;
import se.vidstige.jadb.JadbException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

@Slf4j
public class ADBConnector implements IInputDevice {
    private final JadbConnection connection;
    private final JadbDevice device;
    private final InetSocketAddress address;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    @Getter
    private final Vec2 screenResolution;
    @Getter
    private final String inputDeviceName;

    public ADBConnector(String host, int port) throws IOException, ConnectionToRemoteDeviceException, JadbException {
        var config = SekaiMagic.getInstance().getConfig();

        log.info("[ADB] Connecting to {}:{}", host, port);
        connection = new JadbConnection();
        address = new InetSocketAddress(host, port);
        connection.connectToTcpDevice(address);
        log.info("[ADB] Connected to {} devices", connection.getDevices().size());
        log.info("Fetching device data, please wait...");

        device = connection.getAnyDevice();

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
            // touchscreen device
            var devicesStr = ExecuteShellBlocking("getevent -p");
            var devices = Util.ParseEventDevices(devicesStr);
            devices.values().removeIf(c -> !c.contains(ABSKeyCode.ABS_MT_SLOT));

            if (devices.size() != 1) {
                // use specified device in config
                log.warn("[ADB] No suitable touchscreen device found, or found multiple. Using device name from config");
                var deviceName = config.GetString("adb.inputDeviceName", "");
                if (deviceName.isEmpty()) {
                    throw new ADBInitializationConnection("No touchscreen device found from the connected device, and no device name specified in config");
                }

                inputDeviceName = deviceName;

            } else {
                inputDeviceName = devices.keySet().stream().findFirst().orElseThrow();
            }

            log.info("[ADB] Input device name: {}", inputDeviceName);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executor.shutdown();
            try {
                connection.disconnectFromTcpDevice(address);
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
        return ExecuteShellBlocking(command, -1);
    }
    public String ExecuteShellBlocking(String command, int terminateAfter) {
        try (
                var is = device.executeShell(command);
                var reader = new BufferedReader(new InputStreamReader(is));
        ) {
            String line;
            var ret = new StringBuilder();

            var linesRead = 0;
            while ((line = reader.readLine()) != null && (terminateAfter == -1 || linesRead < terminateAfter)) {
                ret.append(line).append("\n");
                linesRead++;
            }

            return ret.toString();

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

    @Override
    public void Tap(double x) {
        var pos = ToScreenTapPosition(x);
//        ExecuteShellAsync("input tap %.0f %.0f".formatted(pos.getX(), pos.getY()));
    }

    @Override
    public void FlickUp(double x, int fingerId) {
        var config = SekaiMagic.getInstance().getConfig();
        var pos = ToScreenTapPosition(x);
        var duration = new Vec2(config.GetList("autoplay.randomization.swipeDuration", Double.class));

        ExecuteShellAsync("input swipe %.0f %.0f %.0f %.0f 100".formatted(pos.getX(), pos.getY(), pos.getX(), pos.getY() - 100));

//        log.debug(pos.toString());
//        var builder = new TouchEventBuilder(this)
//                .FingerDown(fingerId)
//                .Position(new Vec2(screenResolution.getY() - pos.getY(), pos.getX()))
//                .Sync()
//                .Sleep(duration.Random() / 1000.0)
//                .Position(new Vec2(screenResolution.getY() - pos.getY() + 100, pos.getX()))
//                .Sync()
//                .FingerUp()
//                .Sync();
////        res.thenAccept(log::debug);
//
//        ExecuteShellAsync(String.join(";", builder.Build()));
    }

    private Vec2 ToScreenTapPosition(double x) {
        var config = SekaiMagic.getInstance().getConfig();
        var range = new Vec2(config.GetList("autoplay.randomization.tapOffsetX", Double.class));
        var offset = range.Random();
        var laneWidth = screenResolution.getX() / 12.0;
        var yRange = new Vec2(config.GetList("autoplay.randomization.tapOffsetY", Double.class));
        var pos = (x + offset) * laneWidth + laneWidth / 2.0;

        return new Vec2(pos, screenResolution.getY() * 0.95 + yRange.Random());
    }
}
