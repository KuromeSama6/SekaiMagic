#include <jni.h>
#include <fcntl.h>
#include <unistd.h>
#include <linux/uinput.h>
#include <string.h>
#include <stdio.h>
#include <sys/ioctl.h>

JNIEXPORT jint JNICALL
Java_moe_ku6_sekaimagic_inputdaemon_UInputJNI_Open(JNIEnv *env, jobject thiz) {
    int fd = open("/dev/uinput", O_WRONLY | O_NONBLOCK);
    if (fd < 0) return -1;

    // Enable touchscreen capabilities
    ioctl(fd, UI_SET_EVBIT, EV_ABS);
    ioctl(fd, UI_SET_EVBIT, EV_KEY);
    ioctl(fd, UI_SET_EVBIT, EV_SYN);

    ioctl(fd, UI_SET_ABSBIT, ABS_MT_POSITION_X);
    ioctl(fd, UI_SET_ABSBIT, ABS_MT_POSITION_Y);
    ioctl(fd, UI_SET_ABSBIT, ABS_MT_TRACKING_ID);
    ioctl(fd, UI_SET_ABSBIT, ABS_MT_SLOT);

    ioctl(fd, UI_SET_ABSBIT, ABS_MT_PRESSURE);
    ioctl(fd, UI_SET_ABSBIT, ABS_MT_TOUCH_MAJOR);
    ioctl(fd, UI_SET_PROPBIT, INPUT_PROP_DIRECT);

    ioctl(fd, UI_SET_KEYBIT, BTN_TOUCH);

    struct uinput_user_dev uidev;
    memset(&uidev, 0, sizeof(uidev));
    snprintf(uidev.name, UINPUT_MAX_NAME_SIZE, "SekaiInputDaemonT");
    uidev.id.bustype = BUS_VIRTUAL;
    uidev.id.vendor  = 0x1234;
    uidev.id.product = 0x5678;
    uidev.id.version = 1;

    // Set ABS ranges
    uidev.absmin[ABS_MT_POSITION_X] = 0;
    uidev.absmax[ABS_MT_POSITION_X] = 1080;
    uidev.absmin[ABS_MT_POSITION_Y] = 0;
    uidev.absmax[ABS_MT_POSITION_Y] = 1920;
    uidev.absmin[ABS_MT_SLOT] = 0;
    uidev.absmax[ABS_MT_SLOT] = 63;
    uidev.absmin[ABS_MT_TRACKING_ID] = 0;
    uidev.absmax[ABS_MT_TRACKING_ID] = 65535;

    if (write(fd, &uidev, sizeof(uidev)) < 0) {
        close(fd);
        return -2;
    }

    if (ioctl(fd, UI_DEV_CREATE) < 0) {
        close(fd);
        return -3;
    }

    return fd;
}

JNIEXPORT void JNICALL
Java_moe_ku6_sekaimagic_inputdaemon_UInputJNI_Emit(JNIEnv *env, jobject thiz, jint fd, jint type, jint code, jint value) {
    struct input_event ev;
    struct timeval tv;
    gettimeofday(&tv, NULL);

    memset(&ev, 0, sizeof(ev));
    ev.time = tv;
    ev.type = (uint16_t)type;
    ev.code = (uint16_t)code;
    ev.value = value;

    write(fd, &ev, sizeof(ev));
}

JNIEXPORT void JNICALL
Java_moe_ku6_sekaimagic_inputdaemon_UInputJNI_Close(JNIEnv *env, jobject thiz, jint fd) {
    if (fd >= 0) {
        ioctl(fd, UI_DEV_DESTROY);
        close(fd);
    }
}
