package moe.ku6.sekaimagic.inputdaemon.command.impl;

import android.util.Log;
import moe.ku6.jsonwrapper.JsonWrapper;
import moe.ku6.sekaimagic.inputdaemon.InputDaemon;
import moe.ku6.sekaimagic.inputdaemon.InputDaemonService;
import moe.ku6.sekaimagic.inputdaemon.command.ICommandHandler;
import moe.ku6.sekaimagic.inputdaemon.exception.CommandHandleException;

public class SendEventCommand implements ICommandHandler {
    @Override
    public String GetCommand() {
        return "sendevent";
    }

    @Override
    public JsonWrapper HandleInternal(JsonWrapper data) throws CommandHandleException {
        var events = data.GetList("events", Integer.class);
        if (events.size() % 3 != 0) {
            throw new CommandHandleException("event size is not multiple of 3");
        }

        for (int i = 0; i < events.size(); i += 3) {
            int type = events.get(i);
            int code = events.get(i + 1);
            int value = events.get(i + 2);

            Log.d("SendEventCommand", "event type: " + type + " code: " + code + " value: " + value);

            InputDaemon.getInstance().runOnUiThread(() -> {
                InputDaemonService.getInstance().getUInput().Emit(type, code, value);
            });
        }

        return null;
    }
}
