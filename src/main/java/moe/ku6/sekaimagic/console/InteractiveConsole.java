package moe.ku6.sekaimagic.console;

import lombok.extern.slf4j.Slf4j;
import moe.ku6.sekaimagic.SekaiMagic;
import moe.ku6.sekaimagic.command.CommandManager;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.tools.ant.types.Commandline;
import org.jline.reader.*;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

@Slf4j
public class InteractiveConsole {
    private final Terminal terminal;
    private final LineReader lineReader;

    public InteractiveConsole() throws IOException {
        terminal = TerminalBuilder.builder()
                .jansi(true)
                .system(true)
                .build();

        var historyFile = new File(SekaiMagic.getInstance().getCwd() + "/.sekai_history");
        if (!historyFile.exists()) {
            historyFile.createNewFile();
        }

        lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .variable(LineReader.HISTORY_FILE, historyFile)
                .build();
    }

    public void Start() throws IOException {
        ConsoleLoop();
        terminal.close();
    }

    public String ReadBlocking(String prompt) {
        try {
            return lineReader.readLine(prompt);
        } catch (UserInterruptException | EndOfFileException e) {
            return null;
        }
    }

    private void ConsoleLoop() {
        while (true) {
            try {
                var line = lineReader.readLine(">> ");
                if (line.isEmpty()) continue;

                var args = Commandline.translateCommandline(line);
                if (args.length == 0) continue;

                CommandManager.getInstance().ProcessCommand(args);

            } catch (UserInterruptException | EndOfFileException e) {
                log.info("User exit.");
                break;
            }

        }
    }
}
