package moe.ku6.sekaimagic.input;

import moe.ku6.sekaimagic.util.Vec2;

import java.util.ArrayList;
import java.util.List;

public class TouchEventBuilder {

    private final List<Integer> commands = new ArrayList<>();
    private int slotId = -1;

    public TouchEventBuilder(int slotId) {
        this.slotId = slotId;
        commands.add(3);
        commands.add(ABSKeyCode.ABS_MT_SLOT);
        commands.add(slotId);
    }

    public TouchEventBuilder FingerDown() {


        commands.add(3);
        commands.add(ABSKeyCode.ABS_MT_TRACKING_ID);
        commands.add(slotId);
        return this;
    }

    public TouchEventBuilder FingerUp() {
        commands.add(3);
        commands.add(ABSKeyCode.ABS_MT_TRACKING_ID);
        commands.add(-1);
        return this;
    }

    public TouchEventBuilder Position(Vec2 position) {
//        commands.add("sendevent %s 3 %d %d".formatted(inputDaemon.getInputDeviceName(), ABSKeyCode.ABS_MT_SLOT, slotId));
//        commands.add("sendevent %s 3 %d %d".formatted(inputDaemon.getInputDeviceName(), ABSKeyCode.ABS_MT_POSITION_X, (int)position.getX()));
//        commands.add("sendevent %s 3 %d %d".formatted(inputDaemon.getInputDeviceName(), ABSKeyCode.ABS_MT_POSITION_Y, (int)position.getY()));

        commands.add(3);
        commands.add(ABSKeyCode.ABS_MT_POSITION_X);
        commands.add((int)(Math.random() * 100));

        commands.add(3);
        commands.add(ABSKeyCode.ABS_MT_POSITION_X);
        commands.add((int) position.getX());

        commands.add(3);
        commands.add(ABSKeyCode.ABS_MT_POSITION_Y);
        commands.add((int)(Math.random() * 100));

        commands.add(3);
        commands.add(ABSKeyCode.ABS_MT_POSITION_Y);
        commands.add((int) position.getY());

        return this;
    }

    public TouchEventBuilder TouchButton(boolean down) {
        commands.add(1);
        commands.add(330);
        commands.add(down ? 1 : 0);
        return this;
    }

    public TouchEventBuilder Sync() {
//        commands.add("sendevent %s 3 %d %d".formatted(inputDaemon.getInputDeviceName(), ABSKeyCode.ABS_MT_SLOT, slotId));
//        commands.add("sendevent %s 0 0 0".formatted(inputDaemon.getInputDeviceName()));

        commands.add(0);
        commands.add(0);
        commands.add(0);

        return this;
    }

    public List<Integer> Build() {
        return new ArrayList<>(commands);
    }

}
