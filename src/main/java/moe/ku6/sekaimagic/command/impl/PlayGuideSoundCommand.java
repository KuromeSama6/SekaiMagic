package moe.ku6.sekaimagic.command.impl;

import lombok.extern.slf4j.Slf4j;
import moe.ku6.sekaimagic.input.InputManager;
import moe.ku6.sekaimagic.input.TouchEventBuilder;
import moe.ku6.sekaimagic.command.ICommand;
import moe.ku6.sekaimagic.input.GuideSoundPlayer;
import moe.ku6.sekaimagic.util.Vec2;

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
        var connector = InputManager.getInstance().getDaemons().getFirst();


        var builder = new TouchEventBuilder(0)
                .FingerDown()
                .Position(new Vec2(540, 960))
                .Sync()
                .FingerUp()
                .Sync();

        connector.getWebsocketClient().SendEvents(builder.Build());
    }
}
