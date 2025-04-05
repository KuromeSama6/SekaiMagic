package moe.ku6.sekaimagic.input;

/**
 * Represents a device that can receive tap events.
 */
public interface IInputDevice {
    /**
     * Taps at the given relative screen coordinates.
     * @param x The x coordinate of the tap, relative to the screen resolution. 0 is the left edge of the screen, 1 is the right edge.
     */
    void Tap(double x);

    /**
     * Performs a quick flick at the given relative screen coordinates.
     * @param x The x coordinate of the flick, relative to the screen resolution. 0 is the left edge of the screen, 1 is the right edge.
     * @param fingerId The ID of the finger to use for the flick. This is used to identify which finger to use when multiple fingers are on the screen.
     */
    void FlickUp(double x, int fingerId);
}
