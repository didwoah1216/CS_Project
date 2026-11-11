package com.chagok.client.util;

import javax.swing.UIManager;
import java.awt.Font;

public final class UiFonts {
    private static Font base;

    private UiFonts() {
    }

    public static void install() {
        base = pick(14);
        UIManager.put("defaultFont", base);
        for (String key : new String[]{
                "Label.font", "Button.font", "ToggleButton.font", "RadioButton.font", "CheckBox.font",
                "ComboBox.font", "TextField.font", "TextArea.font", "Table.font", "TableHeader.font",
                "TabbedPane.font", "OptionPane.messageFont", "OptionPane.buttonFont", "TitledBorder.font",
                "List.font", "Spinner.font"}) {
            UIManager.put(key, base);
        }
    }

    public static Font ui(int size) {
        Font source = base == null ? pick(size) : base;
        return source.deriveFont((float) size);
    }

    public static Font ui(int style, int size) {
        return ui(size).deriveFont(style, (float) size);
    }

    private static Font pick(int size) {
        String[] names = {
                "Malgun Gothic", "Apple SD Gothic Neo", "Noto Sans CJK KR", "Noto Sans KR",
                "WenQuanYi Micro Hei", Font.SANS_SERIF
        };
        for (String name : names) {
            Font font = new Font(name, Font.PLAIN, size);
            if (font.canDisplay('가')) {
                return font;
            }
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, size);
    }
}
