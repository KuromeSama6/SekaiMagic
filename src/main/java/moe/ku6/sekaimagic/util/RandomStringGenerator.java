package moe.ku6.sekaimagic.util;

import java.security.SecureRandom;

public class RandomStringGenerator {
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom random = new SecureRandom();

    public static String GenerateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(ALPHANUMERIC.length());
            char randomChar = ALPHANUMERIC.charAt(randomIndex);
            sb.append(randomChar);
        }
        return sb.toString();
    }

    public static String GenerateRandomSnowflake() {
        return GenerateRandomString(32)
                + RandomizeCapitalization(CUID.FromDenary(System.currentTimeMillis()).toString())
                + GenerateRandomString(16 - 9)
                + RandomizeCapitalization(CUID.FromDenary(UniqueIdGenerator.GenerateUniqueId()).toString())
                + GenerateRandomString(16 - 9);
    }

    private static String RandomizeCapitalization(String input) {
        StringBuilder result = new StringBuilder();

        for (char c : input.toCharArray()) {
            // Randomly decide whether to capitalize or keep as lowercase
            if (Character.isLetter(c)) {
                if (random.nextBoolean()) {
                    result.append(Character.toUpperCase(c));
                } else {
                    result.append(Character.toLowerCase(c));
                }
            } else {
                // If it's not a letter, append the character unchanged
                result.append(c);
            }
        }

        return result.toString();
    }

}
