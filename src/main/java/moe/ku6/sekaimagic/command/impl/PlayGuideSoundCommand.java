package moe.ku6.sekaimagic.command.impl;

import lombok.extern.slf4j.Slf4j;
import moe.ku6.sekaimagic.adb.ADBManager;
import moe.ku6.sekaimagic.adb.TouchEventBuilder;
import moe.ku6.sekaimagic.command.ICommand;
import moe.ku6.sekaimagic.input.GuideSoundPlayer;
import moe.ku6.sekaimagic.util.Vec2;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.util.Arrays;

@Slf4j
public class PlayGuideSoundCommand implements ICommand<Object> {
    @Override
    public String[] GetNames() {
        return new String[] {"playguidesound", "clap" };
    }

    @Override
    public String GetManual() {
        return "Play the guide sound";
    }

    @Override
    public void HandleInternal(Object args) throws Exception {
//        ADBManager.getInstance().getConnectors().forEach(c -> c.ExecuteShellAsync("input", "swipe", "500", "500", "0", "500", "100"));
//        ADBManager.getInstance().getConnectors().forEach(c -> c.ExecuteShellAsync("input", "swipe", "500", "500", "500", "0", "100"));
//        ADBManager.getInstance().getConnectors().forEach(c -> c.ExecuteShellAsync("input tap 960 540"));

        try (var player = new GuideSoundPlayer()) {
            player.Play();

        } catch (Exception e) {
            throw new RuntimeException("Failed to play guide sound", e);
        }

        // flick
        var connector = ADBManager.getInstance().getConnectors().getFirst();

        var builder = new TouchEventBuilder(connector)
                .FingerDown(1)
                .Position(new Vec2(540, 960))
                .Sync()
                .FingerUp()
                .Sync();

        log.debug(String.join("\n", builder.Build()));

        var ret = connector.ExecuteShellBlocking(String.join(";", builder.Build()));
        log.debug(ret);
    }
}
