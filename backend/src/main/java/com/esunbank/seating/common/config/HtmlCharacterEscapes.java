package com.esunbank.seating.common.config;

import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SerializedString;

/**
 * 共用層：XSS 防護。
 * 將 JSON 字串值中的 HTML 危險字元 (&lt; &gt; &amp; ') 轉為 Unicode 跳脫序列，
 * 避免回傳資料被當成 HTML/Script 執行 (縱深防禦，前端 Vue 亦會自動跳脫)。
 */
public class HtmlCharacterEscapes extends CharacterEscapes {

    private final int[] asciiEscapes;

    public HtmlCharacterEscapes() {
        int[] esc = CharacterEscapes.standardAsciiEscapesForJSON();
        esc['<'] = CharacterEscapes.ESCAPE_CUSTOM;
        esc['>'] = CharacterEscapes.ESCAPE_CUSTOM;
        esc['&'] = CharacterEscapes.ESCAPE_CUSTOM;
        esc['\''] = CharacterEscapes.ESCAPE_CUSTOM;
        this.asciiEscapes = esc;
    }

    @Override
    public int[] getEscapeCodesForAscii() {
        return asciiEscapes;
    }

    @Override
    public SerializableString getEscapeSequence(int ch) {
        switch (ch) {
            case '<':
                return new SerializedString("\\u003C");
            case '>':
                return new SerializedString("\\u003E");
            case '&':
                return new SerializedString("\\u0026");
            case '\'':
                return new SerializedString("\\u0027");
            default:
                return null;
        }
    }
}
