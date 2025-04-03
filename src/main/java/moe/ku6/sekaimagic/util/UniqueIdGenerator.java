package moe.ku6.sekaimagic.util;

import java.util.concurrent.atomic.AtomicLong;

public class UniqueIdGenerator {
    private static final long MAX_SEQUENCE = CUID.sizeLimit;
    private static final AtomicLong lastTimestamp = new AtomicLong(-1L);
    private static final AtomicLong sequence = new AtomicLong(0L);

    public static long GenerateUniqueId() {
        long timestamp = System.currentTimeMillis();
        long currentSequence;

        // Synchronize to ensure thread safety
        synchronized (UniqueIdGenerator.class) {
            if (timestamp == lastTimestamp.get()) {
                currentSequence = sequence.incrementAndGet();
                if (currentSequence >= MAX_SEQUENCE) {
                    // If we reach the maximum sequence, wait for next millisecond
                    while (timestamp <= lastTimestamp.get()) {
                        timestamp = System.currentTimeMillis();
                    }
                    // Reset sequence for the new millisecond
                    sequence.set(0);
                }
            } else {
                // Reset sequence for the new millisecond
                sequence.set(0);
                currentSequence = sequence.incrementAndGet();
            }
            lastTimestamp.set(timestamp);
        }

        return (timestamp << 20) + currentSequence; // Adjust bit shifting as needed
    }
}
