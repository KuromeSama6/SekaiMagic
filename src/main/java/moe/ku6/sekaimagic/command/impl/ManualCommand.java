package moe.ku6.sekaimagic.command.impl;

import com.beust.jcommander.Parameter;
import lombok.extern.slf4j.Slf4j;
import moe.ku6.sekaimagic.command.CommandManager;
import moe.ku6.sekaimagic.command.ICommand;
import org.jline.jansi.Ansi;

@Slf4j
public class ManualCommand implements ICommand<ManualCommand.Params> {
    @Override
    public String[] GetNames() {
        return new String[] {"man"};
    }

    @Override
    public String GetManual() {
        return "See help for a specific command.";
    }

    @Override
    public void HandleInternal(Params args) throws Exception {
        if (args.command == null) {
            log.info("What help would you like to see?\nUse `man <command>` to see help for a specific command.");

            var sb = new StringBuilder();
            sb.append("Available commands: ");
            sb.append(Ansi.ansi().fgBrightGreen().a(String.join(", ", CommandManager.getInstance().getCommands().keySet())).reset());

            log.info(sb.toString());
            return;
        }

        var command = CommandManager.getInstance().getCommands().get(args.command.toLowerCase());
        if (command == null) {
            log.warn("no manual for {}", args.command);
            return;
        }

        var usage = command.GetUsage();
        log.info("{} - {}", args.command, command.GetManual());
        log.info(usage);
    }

    public static class Params {
        @Parameter(description = "The command to show help for")
        public String command;
    }
}
