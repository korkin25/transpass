package io.github.transpass;

import java.util.Objects;

/** Maps Russian letter keys to the characters produced by the English keyboard layout. */
public final class KeyboardLayout {
    private static final String RUSSIAN_LOWER = "ёйцукенгшщзхъфывапролджэячсмитьбю";
    private static final String ENGLISH_LOWER = "`qwertyuiop[]asdfghjkl;'zxcvbnm,.";
    private static final String RUSSIAN_UPPER = "ЁЙЦУКЕНГШЩЗХЪФЫВАПРОЛДЖЭЯЧСМИТЬБЮ";
    private static final String ENGLISH_UPPER = "~QWERTYUIOP{}ASDFGHJKL:\"ZXCVBNM<>";

    private KeyboardLayout() {
    }

    public static String convert(String text) {
        Objects.requireNonNull(text, "text");
        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            int lowerIndex = RUSSIAN_LOWER.indexOf(character);
            if (lowerIndex >= 0) {
                result.append(ENGLISH_LOWER.charAt(lowerIndex));
                continue;
            }
            int upperIndex = RUSSIAN_UPPER.indexOf(character);
            result.append(upperIndex >= 0 ? ENGLISH_UPPER.charAt(upperIndex) : character);
        }
        return result.toString();
    }
}
