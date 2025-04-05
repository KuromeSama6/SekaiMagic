package moe.ku6.sekaimagic.command.impl;

import com.beust.jcommander.Parameter;
import lombok.extern.slf4j.Slf4j;
import moe.ku6.sekaimagic.SekaiMagic;
import moe.ku6.sekaimagic.adb.ADBManager;
import moe.ku6.sekaimagic.command.ICommand;
import moe.ku6.sekaimagic.exception.command.CommandExecutionException;
import se.vidstige.jadb.JadbException;

import java.io.IOException;

@Slf4j
public class ADBClientCommand implements ICommand<ADBClientCommand.Params> {
    @Override
    public String[] GetNames() {
        return new String[] {"adbc"};
    }

    @Override
    public String GetManual() {
        return "Manage ADB client connections.";
    }

    @Override
    public void HandleInternal(Params args) throws Exception {
        if (args.connect != null) {
            var addrArgs = args.connect.split(":");
            if (addrArgs.length != 2) {
                throw new CommandExecutionException("Invalid address format. Use <ip>:<port>");
            }

            var ip = addrArgs[0];
            var port = Integer.parseInt(addrArgs[1]);

            try {
                ADBManager.getInstance().Connect(ip, port);
            } catch (Exception e) {
                log.error("Connection failed: {}", e.getMessage());
                return;
            }

            if (!args.noSave) {
                var list = SekaiMagic.getInstance().getConfig().GetList("adb.autoConnect", String.class);
                if (!list.contains(args.connect)) {
                    list.add(args.connect);
                    SekaiMagic.getInstance().getConfig().Set("adb.autoConnect", list);
                    SekaiMagic.getInstance().SaveConfig();
                }
            }

            return;
        }

        log.info(GetUsage());
    }

    public static class Params {
        @Parameter(
                names = {"-c", "--connect"},
                description = "Connect to ADB client."
        )
        public String connect;

        @Parameter(
                names = {"--no-save"},
                description = "Do not save the ADB client connection to the list of autoconnect addresses."
        )
        public boolean noSave = false;
    }
}
