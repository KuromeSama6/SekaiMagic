package moe.ku6.sekaimagic.util;

import lombok.experimental.UtilityClass;
import org.joda.time.DateTime;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@UtilityClass
public class Util {
    public static String SHA256(String input) {
        if (input == null) return null;

        try {
            // Get an instance of the SHA-256 MessageDigest
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Perform the hashing
            byte[] hashBytes = digest.digest(input.getBytes("UTF-8"));

            // Convert the byte array into a hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String ToUrlEncodedParams(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!sb.isEmpty()) sb.append('&');
            sb.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return sb.toString();
    }

    public static String FormatDatetimeALLNetUTC(DateTime time) {
        // 2025-01-27 22:54:46.0
        return time.toString("yyyy-MM-dd HH:mm:ss.S");
    }

    public static String ReadToStringAutoClose(InputStream is) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String ret = reader.lines().collect(Collectors.joining(System.lineSeparator()));
            is.close();
            return ret;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<File> MatchFiles(File dir, String glob) {
        List<File> matchedFiles = new ArrayList<>();
        if (dir == null || !dir.isDirectory()) {
            return matchedFiles; // Return empty list if directory is invalid
        }

        Path dirPath = dir.toPath();
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);

        try {
            Files.walkFileTree(dirPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (matcher.matches(file.getFileName())) {
                        matchedFiles.add(file.toFile());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace(); // Handle exception properly in a real application
        }

        return matchedFiles;
    }

    public static String ToFullWidth(String input) {
        StringBuilder fullWidth = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c >= 33 && c <= 126) { // Convert ASCII printable characters
                fullWidth.append((char)(c - 33 + 0xFF01));
            } else if (c == ' ') { // Convert space separately
                fullWidth.append('\u3000');
            } else {
                fullWidth.append(c); // Keep non-ASCII characters unchanged
            }
        }
        return fullWidth.toString();
    }

    public static <T> CompletableFuture<T> FromFuture(Future<T> future) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static Map<String, Set<Integer>> ParseEventDevices(String data) {
        Map<String, Set<Integer>> deviceEvents = new HashMap<>();

        String[] lines = data.split("\n");
        String currentDevice = null;
        Set<Integer> currentEvents = null;

        Pattern devicePattern = Pattern.compile("add device \\d+: (.+)");
        Pattern eventCodePattern = Pattern.compile("\\s+([0-9a-fA-F]{4})\\s*:.*");

        for (String line : lines) {
            Matcher deviceMatcher = devicePattern.matcher(line);
            if (deviceMatcher.find()) {
                currentDevice = deviceMatcher.group(1).trim();
                currentEvents = new HashSet<>();
                deviceEvents.put(currentDevice, currentEvents);
                continue;
            }

            if (currentDevice != null) {
                Matcher eventMatcher = eventCodePattern.matcher(line);
                if (eventMatcher.find()) {
                    int code = Integer.parseInt(eventMatcher.group(1), 16);
                    currentEvents.add(code);
                }
            }
        }

        return deviceEvents;
    }
}
