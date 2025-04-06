package moe.ku6.sekaimagic.inputdaemon.command.impl;

import moe.ku6.jsonwrapper.JsonWrapper;
import moe.ku6.sekaimagic.inputdaemon.command.ICommandHandler;
import moe.ku6.sekaimagic.inputdaemon.exception.CommandHandleException;

public class PingCommand implements ICommandHandler {
    @Override
    public String GetCommand() {
        return "ping";
    }

    @Override
    public JsonWrapper HandleInternal(JsonWrapper data) throws CommandHandleException {
        return null;
    }
}
