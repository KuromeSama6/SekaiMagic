package moe.ku6.sekaimagic.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import moe.ku6.jsonwrapper.JsonWrapper;
import moe.ku6.sekaimagic.SekaiMagic;
import moe.ku6.sekaimagic.exception.track.TrackException;
import moe.ku6.sekaimagic.music.MusicPackage;
import moe.ku6.sekaimagic.music.Track;
import moe.ku6.sekaimagic.music.TrackDifficulty;
import moe.ku6.jsonwrapper.JsonUtil;
import moe.ku6.sekaimagic.util.Util;
import okhttp3.Request;
import org.jline.jansi.Ansi;
import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class SekaiDatabase {
    @Getter
    private static SekaiDatabase instance;
    @Getter
    private final Map<Integer, MusicPackage> packages = new HashMap<>();

    public SekaiDatabase() {
        if (instance != null)
            throw new IllegalStateException("SekaiDataManager is already initialized");

        instance = this;

        ReloadSekaiDataBlocking();
    }

    public void ReloadSekaiDataBlocking() {
        log.info("Loading Sekai data...");
        var dataDir = new File(SekaiMagic.getInstance().getCwd() + "/data");
        dataDir.mkdirs();
        JsonWrapper data = new JsonWrapper();

        var sheetCacheFile = new File(dataDir + "/sheet_cache.json");
        if (!sheetCacheFile.exists()) {
            log.warn("No sheet cache data found. Downloading from pjsekai.moe...");

            // sheet data
            {
                var url = "https://database.pjsekai.moe/musics.json?t=%d".formatted(DateTime.now().getMillis());
                var req = new Request.Builder()
                        .url(url)
                        .get()
                        .build();

                try (var response = SekaiMagic.getInstance().getHttpClient().newCall(req).execute()) {
                    if (!response.isSuccessful()) {
                        log.error("Failed to downoad music data. This program may not continue.");
                        System.exit(1);
                    }

                    var body = response.body().string();
                    var arr = new Gson().fromJson(body, JsonArray.class);
                    data.Set("packages", arr);

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            // difficulties data
            {

                var url = "https://database.pjsekai.moe/musicDifficulties.json?t=%d".formatted(DateTime.now().getMillis());
                var req = new Request.Builder()
                        .url(url)
                        .get()
                        .build();

                try (var response = SekaiMagic.getInstance().getHttpClient().newCall(req).execute()) {
                    if (!response.isSuccessful()) {
                        log.error("Failed to downoad difficulty data. This program may not continue.");
                        System.exit(1);
                    }

                    var body = response.body().string();
                    var arr = new Gson().fromJson(body, JsonArray.class);
                    data.Set("difficulties", arr);

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            // write to cache
            data.Save(sheetCacheFile);
            log.info("Sheet cache data downloaded and saved to {}", sheetCacheFile.getAbsolutePath());

        } else {
            data = JsonUtil.Read(sheetCacheFile);
            log.info("Sheet cache data found. Loading...");
        }

        LoadSheetData(data);

        if (!EnsureGameFiles()) {
            log.warn("No game file path set. Some features may not work without a valid path. To learn more about extracting game files, please visit our repository.");
            return;
        }

        LoadTracks();
    }

    public boolean EnsureGameFiles() {
        var path = SekaiMagic.getInstance().getConfig().GetString("gameFilesPath", "");
        var dir = new File(path);

        if (!path.isEmpty() && !dir.exists()) {
            log.error("Invalid game files path: {}", path);
        }

        if (path.isEmpty()) {
            log.warn(Ansi.ansi().a("No game file path set. Enter the path to your game files directory now or hit enter to skip. Some features may not work without a valid path.").reset().toString());
            path = SekaiMagic.getInstance().getConsole().ReadBlocking("Path: ");

            if (path.isEmpty()) return false;
            dir = new File(path);
            if (!dir.exists()) {
                log.error("Invalid path: {}. Skipping.", path);
                return false;
            }

            SekaiMagic.getInstance().getConfig().Set("gameFilesPath", path);
            SekaiMagic.getInstance().SaveConfig();

            log.info("Game files path set to {}", path);
        }

        return true;
    }

    private void LoadSheetData(JsonWrapper data) {
        {
            // parse musics
            for (var obj : data.GetObjectList("packages")) {
                var music = new MusicPackage(obj);
                packages.put(music.getId(), music);
            }
        }

        {
            // parse difficulties
            for (var obj : data.GetObjectList("difficulties")) {
                var id = obj.GetInt("musicId");
                if (!packages.containsKey(id)) continue;
                var music = packages.get(id);
                music.getPlayLevels().put(TrackDifficulty.FromString(obj.GetString("musicDifficulty")), obj.GetInt("playLevel"));
            }
        }

        log.info("Loaded {} packages.", packages.size());
    }

    private void LoadTracks() {
        log.info("Loading tracks...");
        var gameFilesDir = new File(SekaiMagic.getInstance().getConfig().GetString("gameFilesPath", ""));
        if (!gameFilesDir.exists()) return;

        // load track files
        var files = Util.MatchFiles(gameFilesDir, "*.bytes").stream()
                .filter(c -> c.getParentFile().getParentFile().getName().equals("music_score"))
                .toList();

        List<Integer> missingMusic = new ArrayList<>();
        for (var file : files) {
            try {
                var track = new Track(file);
                var music = packages.get(track.getId());
                if (music == null) {
                    missingMusic.add(track.getId());
                    continue;
                }

                music.getTracks().put(track.getDifficulty(), track);

            } catch (TrackException e) {
                log.error("Failed to load track at {}: {}", file.getAbsolutePath(), e.getMessage());
            }
        }

        if (!missingMusic.isEmpty()) {
            log.warn("The following tracks have no corresponding music definitions and are ignored: {}", missingMusic.toArray());
        }

        packages.values().forEach(MusicPackage::SortTracks);

        log.info("Loaded {} tracks.", files.size());
    }
}
