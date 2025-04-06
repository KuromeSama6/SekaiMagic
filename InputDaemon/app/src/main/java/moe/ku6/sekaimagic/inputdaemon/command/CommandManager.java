package moe.ku6.sekaimagic.inputdaemon.command;

import android.util.Log;
import dalvik.system.DexFile;
import lombok.Getter;
import moe.ku6.jsonwrapper.JsonWrapper;
import moe.ku6.sekaimagic.inputdaemon.InputDaemon;
import moe.ku6.sekaimagic.inputdaemon.exception.CommandHandleException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

public class CommandManager {
    private static final String TAG = "CommandManager";

    @Getter
    private static CommandManager instance;
    private final Map<String, ICommandHandler> handlers = new HashMap<>();

    public CommandManager() throws Exception {
        if (instance != null)
            throw new RuntimeException("CommandManager already initialized");
        instance = this;

        // register commands
        var ctx = InputDaemon.getInstance().getApplicationContext();
        var applicationInfo = ctx.getApplicationInfo();
        var classPath = applicationInfo.sourceDir;
        var dexFile = new DexFile(classPath);
        var classNames = Collections.list(dexFile.entries());

        for (var className : classNames) {
            try {
                Class<?> clazz = ctx.getClassLoader().loadClass(className);
                if (ICommandHandler.class.isAssignableFrom(clazz) && clazz != ICommandHandler.class) {
                    var handler = (ICommandHandler)clazz.getDeclaredConstructor().newInstance();
                    var cmd = handler.GetCommand();
                    if (handlers.containsKey(cmd)) {
                        Log.w(TAG, "Duplicate command handler found: " + cmd);
                        continue;
                    }

                    handlers.put(handler.GetCommand(), handler);
                }

            } catch (ClassNotFoundException e) {}
        }

        dexFile.close();
        Log.i(TAG, "CommandManager initialized with " + handlers.size() + " commands");
    }

    @NotNull
    public JsonWrapper HandleCommand(JsonWrapper data) {
        var ret = new JsonWrapper();
        var cmd = data.GetString("command");
        if (cmd == null) {
            Log.e(TAG, "Invalid message, no command present");
            ret.Set("code", 8);
            ret.Set("message", "invalid message, no command present");
            return ret;
        }

        var handler = handlers.get(cmd);
        if (handler == null) {
            Log.e(TAG, "command not found: " + cmd);
            ret.Set("code", 15);
            ret.Set("message", "command not found");
            return ret;
        }

        try {
            var body = data.GetObject("data");
            var response = handler.HandleInternal(body);
            ret.Set("code", 0);
            ret.Set("message", "ok");
            ret.Set("data", response == null ? new JsonWrapper() : response);

        } catch (CommandHandleException e) {
            ret.Set("code", e.getCode());
            ret.Set("message", e.getMessage());

        } catch (Exception e) {
            ret.Set("code", 2);
            ret.Set("message", e);
        }

        return ret;
    }
}
