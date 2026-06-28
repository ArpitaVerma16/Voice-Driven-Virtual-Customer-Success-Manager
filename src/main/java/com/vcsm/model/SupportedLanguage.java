package com.vcsm.model;

public enum SupportedLanguage {
    ENGLISH("en", "English"),
    HINDI("hi", "Hindi"),
    SPANISH("es", "Spanish");

    private final String code;
    private final String displayName;

    SupportedLanguage(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() { return code; }
    public String getDisplayName() { return displayName; }

    public static SupportedLanguage fromCode(String code) {
        for (SupportedLanguage lang : values()) {
            if (lang.code.equalsIgnoreCase(code)) {
                return lang;
            }
        }
        return ENGLISH;
    }
}
