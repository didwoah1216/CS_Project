package com.chagok.client.util;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class DigitsOnlyFilter extends DocumentFilter {
    private final int maxDigits;

    public DigitsOnlyFilter(int maxDigits) {
        this.maxDigits = maxDigits;
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
        replace(fb, offset, 0, text, attr);
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        String current = fb.getDocument().getText(0, fb.getDocument().getLength());
        String next = current.substring(0, offset) + (text == null ? "" : text) + current.substring(offset + length);
        if (next.matches("\\d{0," + maxDigits + "}")) {
            super.replace(fb, offset, length, text, attrs);
        }
    }
}
