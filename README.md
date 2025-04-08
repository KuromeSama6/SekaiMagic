# SekaiMagic
### Black magic for Project Sekai.

![](https://img.shields.io/badge/Version-0.0.1-blue)
![](https://img.shields.io/badge/Stability-Not_Guaranteed-red)
![](https://img.shields.io/badge/DX_Rating-17000-gold)
![](https://img.shields.io/badge/初音-ミク-00ddc0)
![](https://img.shields.io/badge/宵崎-奏-edfdff)
![](https://img.shields.io/badge/朝比奈-まふゆ-7413a8)
![](https://img.shields.io/badge/東雲-絵名-ccaa88)
![](https://img.shields.io/badge/暁山-瑞希-ffccfc)

SekaiMagic is aims to be an all-in-one toolbox for Project Sekai, including features like song info lookup, leaderboard calculators, and most importantly, an autoplay feature to make your life easier in those midnight leaderboard grinding sessions.

**SekaiMagic is in very early development - issues and problems are common, especially with ADB and Android communication. Please report any issues you encounter on the GitHub page.**

SekaiMagic consists of two components - a Java executable that runs on your PC, and an input daemon that runs on your Android device to take care of touch events. The daemon simulates touch events by opening a virtual touchscreen device through `/dev/uinput`. We've decided to use this method as it was the most efficient and fast way to send touch events.

Note that because of this, you will need root access on your Android device to run the daemon. **We strongly recommend that you use an emulator (Bluestacks or LDPlayer), as emulators tend to be much more permissive with root access, and won't hurt your actual device if something goes wrong.** You'll also need to be able to run ADB in root mode, as it will try to change write permissions of `/dev/uinput` to allow the daemon to write to it.

## Running SekaiMagic

### Prerequisites
- Java 18 or higher
- Android device with root access (or an emulator)
- A Windows device
  - Right now, SekaiMagic uses the Windows Multimedia API (`winmm.dll`) for an accurate timer, which is only available on Windows. We are planning to find alternatives for it on Linux and other platforms.
- A copy of the Project Sekai game files (usually found under `/storage/emulated/0/Android/data/com.hermes.mk/files/` on your Android device).
  - You must extract and decrypt the game files yourself and find the music files. SekaiMagic does not provide any game files or decryption methods for hopefully obvious reasons. We do not condone piracy or any illegal activities, and SekaiMagic is not responsible for any actions you take with it.

### Installation

1. Obtain the JAR file as well as the input daemon APK from the releases page.
2. Place the JAR file in a directory of your choice on your Windows device. This will be the working directory for SekaiMagic and will be used to store configuration files.
3. Place the APK file in the same directory as the JAR file. SekaiMagic will automatically detect the APK file and install it on your Android device.
4. Run the JAR file with `java -jar <name-of-jar-file>.jar`. Follow the prompts to set up the configuration files and the input daemon. SekaiMagic will automatically install the input daemon on your Android device and connect to it.

### Building

Both SekaiMagic and the input daemon can be built right out of the box using Gradle. Build them just like any other Java or Android project.

Note that the input daemon uses JNI, and therefore requires the Android NDK and CMake to be installed. You'll also need to verify that `winmm.dll` exists on your Windows device in order for SekaiMagic to function properly.

### Testing

SekaiMagic has been tested with the following configuration:

- A PC running Windows 11.
- NetEase MuMu 12 emulator running Android 12 under 1920*1080 resolution.
- The latest release of Project Sekai Chinese version.
- ADB connection via IP address and port.

Device compatibility is not guaranteed, and information on other devices are desperately needed. Please report any issues you encounter on the GitHub page.

## Known Issues

SekaiMagic is still in its early stages, and there are quite a few issues that needs to be resolved. Keep in mind these issues are based on the aforementioned testing configuration - more or less issues may arise on other devices.

- [ ] ADB connection fails on the first attempt after the emulator starts up. Subsequent attempts work fine.
- [ ] Occasionally, ADB is unable to for forward a system port to the emulator.
- [ ] Large lag spikes when autoplaying with the "Show taps" and/or "Pointer location" options enabled in developer options.
- [ ] Touching the screen when autoplay is active causes the native touchscreen to be no longer responsive, even after autoplay has ended.
- [ ] Occasionally, autoplay will send touch events for all notes in the track immediately after the track starts, causing the touch screen to become unresponsive (probably due to too many touch events being sent at once).

**Autoplay related issues**

- [ ] Autoplay does not end automatically at the end of the track and must be stopped manually with `autoplay -s`.
- [ ] Slide notes with Bézier curves are not being followed properly, causing occasional misses on longer and thinner notes.
- [ ] Slide notes with zigzags or a lot of movement are being followed the "hard way" and does not resemble a human player. A human player should find a straight path through zigzagging slide notes.
- [ ] Occasionally, long and zigzagging slide notes are being released early (getting a GREAT at the end).
  - This has been specifically reproduced in the Master difficulty of *Tell Your World*, in the slide note to the right on measure 10. The reasons for this are still unknown.

There are definitely more issues that we haven't encountered yet, so please report any issues you encounter on the GitHub page. We will try to resolve them as soon as possible.

## Planned features

- [ ] Support for other platforms (Linux, macOS, etc.)
- [ ] Automatic start of autoplay by detecting the start of a song.
- [ ] Hands-off mode to do consecutive autoplay runs without user input. Useful for grinding leaderboards.
- [ ] Automatic energy recharges with batteries/crystals.
- [ ] Autoplay on multiple devices at once.
- [ ] Automatic game file decryption and extraction.

## Contact

Contact me via:
- Email: kuromesama6@gmail.com
- Discord: kuromesama6