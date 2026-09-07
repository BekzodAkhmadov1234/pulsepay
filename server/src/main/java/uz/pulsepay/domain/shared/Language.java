package uz.pulsepay.domain.shared;

import java.util.Locale;

/**
 * Supported UI languages for PulsePay mobile clients.
 * Values: uz (Uzbek Latin), ru (Russian), en (English), zh (Chinese), uz_c (Uzbek Cyrillic).
 */
public enum Language {

    UZ("uz"),
    RU("ru"),
    EN("en"),
    ZH("zh"),
    UZ_C("uz_c");

    private final String code;

    Language(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /**
     * Resolve a raw header value (case-insensitive) to a Language.
     * Falls back to {@link #UZ} when the value is unknown or null.
     */
    public static Language fromCode(String raw) {
        if (raw == null) return UZ;
        for (Language l : values()) {
            if (l.code.equalsIgnoreCase(raw.trim())) return l;
        }
        return UZ;
    }

    /**
     * Convert to Java {@link Locale} for {@code MessageSource} lookups.
     * <ul>
     *   <li>{@code uz}   → {@code Locale("uz")}</li>
     *   <li>{@code uz_c} → {@code Locale("uz", "c")} — matches messages_uz_c.properties</li>
     *   <li>{@code ru}   → {@code Locale("ru")}</li>
     *   <li>{@code en}   → {@code Locale("en")}</li>
     *   <li>{@code zh}   → {@code Locale("zh")}</li>
     * </ul>
     */
    public Locale toLocale() {
        return switch (this) {
            case UZ   -> Locale.of("uz");
            case UZ_C -> Locale.of("uz", "c");
            case RU   -> Locale.of("ru");
            case EN   -> Locale.ENGLISH;
            case ZH   -> Locale.CHINESE;
        };
    }
}
