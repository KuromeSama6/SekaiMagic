package moe.ku6.sekaimagic.inputdaemon;

import android.util.Log;
import com.sun.jna.*;

import java.util.Arrays;
import java.util.List;

public class UInput {
    public static final String TAG = "UInput";

    public interface CLib extends Library {
        CLib INSTANCE = Native.load("c", CLib.class);

        int open(String path, int flags);

        int ioctl(int fd, long cmd, Object... args);

        int write(int fd, byte[] buffer, int count);

        int close(int fd);

        int memset(Pointer ptr, int value, int num);
    }

    // Constants from <linux/input.h> and <linux/uinput.h>
    public static final int O_WRONLY = 0x0001;
    public static final int O_NONBLOCK = 0x800;
    public static final int EV_KEY = 0x01;
    public static final int EV_ABS = 0x03;
    public static final int EV_SYN = 0x00;
    public static final int SYN_REPORT = 0;
    public static final int UI_SET_EVBIT = 0x40045564;
    public static final int UI_SET_KEYBIT = 0x40045565;
    public static final int UI_DEV_CREATE = 0x5501;
    public static final int UI_DEV_DESTROY = 0x5502;

    public static class InputEvent extends Structure {
        public NativeLong tv_sec;
        public NativeLong tv_usec;
        public short type;
        public short code;
        public int value;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("tv_sec", "tv_usec", "type", "code", "value");
        }
    }

    public static class UInputUserDev extends Structure {
        public static final int UINPUT_MAX_NAME_SIZE = 80;

        public byte[] name = new byte[UINPUT_MAX_NAME_SIZE];
        public int id_bustype = 0x03;
        public short id_vendor = 0x1234;
        public short id_product = (short)0xfedc;
        public short id_version = 1;
        public int ff_effects_max = 0;
        public int[] absmax = new int[64];
        public int[] absmin = new int[64];
        public int[] absfuzz = new int[64];
        public int[] absflat = new int[64];

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("name", "id_bustype", "id_vendor", "id_product", "id_version",
                    "ff_effects_max", "absmax", "absmin", "absfuzz", "absflat");
        }
    }

    private int fd;

    public boolean Init() {
        fd = CLib.INSTANCE.open("/dev/uinput", O_WRONLY | O_NONBLOCK);
        if (fd < 0) {
            Log.e("UINPUT", "Failed to open /dev/uinput. Check permissions.");
            return false;
        }

        CLib.INSTANCE.ioctl(fd, UI_SET_EVBIT, EV_KEY);
        CLib.INSTANCE.ioctl(fd, UI_SET_EVBIT, EV_ABS);
        CLib.INSTANCE.ioctl(fd, UI_SET_EVBIT, EV_SYN);

        CLib.INSTANCE.ioctl(fd, UI_SET_EVBIT, ABSKeyCode.ABS_MT_SLOT);
        CLib.INSTANCE.ioctl(fd, UI_SET_EVBIT, ABSKeyCode.ABS_MT_POSITION_X);
        CLib.INSTANCE.ioctl(fd, UI_SET_EVBIT, ABSKeyCode.ABS_MT_POSITION_Y);
        CLib.INSTANCE.ioctl(fd, UI_SET_EVBIT, ABSKeyCode.ABS_MT_TRACKING_ID);

        UInputUserDev uidev = new UInputUserDev();
        var deviceName = "SekaiMagicInputDaemon";
        System.arraycopy(deviceName.getBytes(), 0, uidev.name, 0, deviceName.length());

        try {
            byte[] devBytes = uidev.getPointer().getByteArray(0, uidev.size());
            CLib.INSTANCE.write(fd, devBytes, devBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        CLib.INSTANCE.ioctl(fd, UI_DEV_CREATE);
        try {
            Thread.sleep(100); // Give time for device creation
        } catch (InterruptedException ignored) {
        }
        return true;
    }

    public void SendKey(int keyCode, boolean pressed) {
        SendEvent(EV_KEY, keyCode, pressed ? 1 : 0);
        SendEvent(EV_SYN, SYN_REPORT, 0);
    }

    private void SendEvent(int type, int code, int value) {
        InputEvent event = new InputEvent();
        event.tv_sec = new NativeLong(System.currentTimeMillis() / 1000);
        event.tv_usec = new NativeLong((System.nanoTime() / 1000) % 1000000);
        event.type = (short)type;
        event.code = (short)code;
        event.value = value;
        event.write();

        CLib.INSTANCE.write(fd, event.getPointer().getByteArray(0, event.size()), event.size());
    }

    public void close() {
        CLib.INSTANCE.ioctl(fd, UI_DEV_DESTROY);
        CLib.INSTANCE.close(fd);
    }
}

