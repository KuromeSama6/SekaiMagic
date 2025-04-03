package moe.ku6.sekaimagic.command.impl;

import moe.ku6.sekaimagic.command.ICommand;

public class HelpCommand implements ICommand<Object> {
    @Override
    public String[] GetNames() {
        return new String[] {"help"};
    }

    @Override
    public String GetManual() {
        return "See help.";
    }

    @Override
    public void HandleInternal(Object args) throws Exception {

    }
}
