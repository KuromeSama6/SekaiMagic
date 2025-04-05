package moe.ku6.sekaimagic.adb;

import lombok.AllArgsConstructor;
import moe.ku6.sekaimagic.util.Vec2;

import java.util.ArrayList;
import java.util.List;

public class TouchEventBuilder {
    private final ADBConnector adbConnector;

    private final List<String> commands = new ArrayList<>();
    private int slotId = -1;

    public TouchEventBuilder(ADBConnector adbConnector) {
        this.adbConnector = adbConnector;
    }

    public TouchEventBuilder FingerDown(int slotId) {
        if (this.slotId != -1)
            throw new IllegalStateException("Finger is already down (id %d)".formatted(this.slotId));

        this.slotId = slotId;
        commands.add("sendevent %s 3 %d %d".formatted(adbConnector.getInputDeviceName(), ABSKeyCode.ABS_MT_SLOT, slotId));
        commands.add("sendevent %s 3 %d %d".formatted(adbConnector.getInputDeviceName(), ABSKeyCode.ABS_MT_TRACKING_ID, slotId));
        return this;
    }

    public TouchEventBuilder FingerUp() {
        if (this.slotId == -1)
            throw new IllegalStateException("Finger is not down");
        commands.add("sendevent %s 3 %d %d".formatted(adbConnector.getInputDeviceName(), ABSKeyCode.ABS_MT_SLOT, slotId));
        commands.add("sendevent %s 3 %d -1".formatted(adbConnector.getInputDeviceName(), ABSKeyCode.ABS_MT_TRACKING_ID));
        this.slotId = -1;
        return this;
    }

    public TouchEventBuilder Position(Vec2 position) {
        commands.add("sendevent %s 3 %d %d".formatted(adbConnector.getInputDeviceName(), ABSKeyCode.ABS_MT_SLOT, slotId));
        commands.add("sendevent %s 3 %d %d".formatted(adbConnector.getInputDeviceName(), ABSKeyCode.ABS_MT_POSITION_X, (int)position.getX()));
        commands.add("sendevent %s 3 %d %d".formatted(adbConnector.getInputDeviceName(), ABSKeyCode.ABS_MT_POSITION_Y, (int)position.getY()));
        return this;
    }

    public TouchEventBuilder Sync() {
        commands.add("sendevent %s 3 %d %d".formatted(adbConnector.getInputDeviceName(), ABSKeyCode.ABS_MT_SLOT, slotId));
        commands.add("sendevent %s 0 0 0".formatted(adbConnector.getInputDeviceName()));
        return this;
    }

    public TouchEventBuilder Sleep(double seconds) {
        commands.add("sleep %f".formatted(seconds));
        return this;
    }

    public String[] Build() {
        return commands.toArray(new String[0]);
    }

}
