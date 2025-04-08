package moe.ku6.sekaimagic.command.impl;

import lombok.extern.slf4j.Slf4j;
import moe.ku6.sekaimagic.SekaiMagic;
import moe.ku6.sekaimagic.command.ICommand;

@Slf4j
public class ExitCommand implements ICommand<Object> {
    @Override
    public String[] GetNames() {
        return new String[] {"exit", "quit"};
    }

    @Override
    public String GetManual() {
        return "Exit the program.";
    }

    @Override
    public void HandleInternal(Object args) throws Exception {
        SekaiMagic.getInstance().Exit();
    }
}
