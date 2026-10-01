package io.github.transpass;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class KeyboardLayoutTest {
    @Test
    public void convertsEveryRussianLetterInBothCases() {
        assertEquals("`qwertyuiop[]asdfghjkl;'zxcvbnm,.",
                KeyboardLayout.convert("ёйцукенгшщзхъфывапролджэячсмитьбю"));
        assertEquals("~QWERTYUIOP{}ASDFGHJKL:\"ZXCVBNM<>",
                KeyboardLayout.convert("ЁЙЦУКЕНГШЩЗХЪФЫВАПРОЛДЖЭЯЧСМИТЬБЮ"));
    }

    @Test
    public void convertsAnOrdinaryWord() {
        assertEquals("Ghbdtn", KeyboardLayout.convert("Привет"));
        assertEquals("~;", KeyboardLayout.convert("Ёж"));
    }

    @Test
    public void preservesExistingLatinDigitsPunctuationAndWhitespace() {
        assertEquals("Hello 123 !?@#\n\tGhbdtn,",
                KeyboardLayout.convert("Hello 123 !?@#\n\tПривет,"));
    }

    @Test
    public void preservesUnmappedUnicodeAndSupplementaryCharacters() {
        assertEquals("😀 € Іі", KeyboardLayout.convert("😀 € Іі"));
        assertEquals("", KeyboardLayout.convert(""));
    }
}
