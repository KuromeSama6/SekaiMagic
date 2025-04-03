package moe.ku6.sekaimagic;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import moe.ku6.sekaimagic.command.CommandManager;
import moe.ku6.sekaimagic.config.CommandLineConfig;
import moe.ku6.sekaimagic.console.InteractiveConsole;
import moe.ku6.sekaimagic.data.SekaiDataManager;
import moe.ku6.sekaimagic.util.JsonUtil;
import moe.ku6.sekaimagic.util.Util;
import moe.ku6.sekaimagic.util.json.JsonWrapper;
import okhttp3.OkHttpClient;
import org.jline.jansi.Ansi;
import org.jline.jansi.AnsiConsole;

import java.io.File;
import java.net.http.HttpClient;

@Slf4j
public class SekaiMagic {
    @Getter
    private static SekaiMagic instance;
    @Getter
    private CommandLineConfig cliConfig;
    @Getter
    private InteractiveConsole console;
    @Getter
    private JsonWrapper config;
    @Getter
    private File cwd;
    @Getter
    private SekaiDataManager sekaiDataManager;
    @Getter
    private OkHttpClient httpClient;

    private SekaiMagic() throws Exception {
        if (instance != null)
            throw new IllegalStateException("Instance already exists!");
        instance = this;

        log.info(Ansi.ansi().bold().fgBrightMagenta().bgBright(Ansi.Color.WHITE).a("Sekai Magic %s - Black Magic For Project Sekai".formatted(getClass().getPackage().getImplementationVersion())).reset().toString());
        log.info("Working directory: {}", System.getProperty("user.dir"));
        cwd = new File(System.getProperty("user.dir"));

        try (var is = getClass().getResourceAsStream("/static/legal_notice.txt")) {
            if (is == null) {
                throw new RuntimeException("Could not find legal notice file");
            }

            var legalNotice = new String(is.readAllBytes());
            System.out.println(Ansi.ansi().fg(Ansi.Color.RED).bold().a(legalNotice).reset());

        }

        httpClient = new OkHttpClient();

        console = new InteractiveConsole();
        LoadConfig();
        sekaiDataManager = new SekaiDataManager();

        new CommandManager();

        log.info(Ansi.ansi().fgBrightCyan().a("Ready. Type 'help' for general guidance, 'man' for command usage, hit Ctrl+D to exit.").reset().toString());
        log.info(Ansi.ansi().bg(Ansi.Color.WHITE).fg(Ansi.Color.GREEN).a("Hint: Do `search [keyword]` to see info about a music.").reset().toString());

        // block
        console.Start();
    }

    private void LoadConfig() {
        log.info("Loading config...");
        var file = new File(cwd + "/.sekairc.json");
        JsonUtil.UpdateAndWrite(file, getClass().getResourceAsStream("/config/.sekairc.json"));
        if (!file.exists()) {
            log.warn("No config file is found. A file has been created in the current working directory. Please modify it according to your needs and press enter to continue.");

            console.ReadBlocking("");
        }

        config = JsonUtil.Read(file);

        log.info("Config loaded.");

    }

    public void SaveConfig() {
        config.Save(new File(cwd + "/.sekairc.json"));
    }

    private void LoadSekaiData() {

    }

    public static void main(String[] args) throws Exception {
        if (instance != null)
            throw new IllegalStateException("Instance already exists!");

        instance = new SekaiMagic();
    }
}
