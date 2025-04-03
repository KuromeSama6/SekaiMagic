package moe.ku6.sekaimagic.command;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import moe.ku6.sekaimagic.exception.command.CommandExecutionException;
import org.reflections.Reflections;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class CommandManager {
    @Getter
    private static CommandManager instance;
    @Getter
    private final Map<String, ICommand<?>> commands = new HashMap<>();

    public CommandManager() throws Exception {
        if (instance != null) {
            throw new IllegalStateException("CommandManager is already initialized");
        }

        instance = this;
        ReloadCommands();

        log.info("Loaded {} commands", commands.size());
    }

    public void ReloadCommands() throws Exception {
        new Reflections("moe.ku6.sekaimagic.command.impl").getSubTypesOf(ICommand.class).forEach(c -> {
            try {
                var command = c.getConstructor().newInstance();
                RegisterCommand(command);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void RegisterCommand(ICommand<?> command) {
        for (var name : command.GetNames()) {
            if (commands.containsKey(name.toLowerCase())) {
                log.warn("Command {} is already registered to another command (trying to register {})", name.toLowerCase(), command.getClass().getName());
                continue;
            }

            commands.put(name.toLowerCase(), command);
        }
    }

    public void ProcessCommand(String[] args) {
        if (args.length == 0) return;
        var cmd = args[0];

        if (!commands.containsKey(cmd.toLowerCase())) {
            log.warn("{}: command not found", cmd);
            return;
        }
        var command = commands.get(cmd.toLowerCase());
        var argsObject = command.CreateParameterObject();

        try {
            JCommander.newBuilder()
                    .addObject(argsObject)
                    .expandAtSign(false)
                    .build()
                    .parse(Arrays.copyOfRange(args, 1, args.length));

            command.Handle(argsObject);

        } catch (CommandExecutionException e) {
            log.error("The command was not executed correctly: {}", e.getMessage());

        } catch (ParameterException e) {
            log.error("The command was not executed correctly: {}", e.getMessage());
            log.info(command.GetUsage());

        } catch (Exception e) {
            log.error("There was an error while executing the command: {}", e.getMessage());
            e.printStackTrace();
        }

    }
}
