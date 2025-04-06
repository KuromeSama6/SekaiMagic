package moe.ku6.sekaimagic.inputdaemon.command;

import moe.ku6.jsonwrapper.JsonWrapper;
import moe.ku6.sekaimagic.inputdaemon.exception.CommandHandleException;

public interface ICommandHandler {
    String GetCommand();
    JsonWrapper HandleInternal(JsonWrapper data) throws CommandHandleException;
}
