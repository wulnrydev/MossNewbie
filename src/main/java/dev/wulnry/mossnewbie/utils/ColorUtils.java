package dev.wulnry.mossnewbie.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renk dönüştürücü yardımcı sınıf.
 * &#RRGGBB formatındaki hex renk kodlarını ve standart &X kodlarını destekler.
 * BungeeCord / Bukkit ChatColor kullanmaz — tamamen Adventure API uyumludur.
 */
public final class ColorUtils {

    /** &#RRGGBB hex renk kodu deseni */
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    /** Standart &X renk / format kodu deseni (0-9, a-f, k-o, r) */
    private static final Pattern LEGACY_PATTERN =
            Pattern.compile("&([0-9A-Fa-fk-orK-OR])");

    private ColorUtils() {}

    /**
     * Verilen metni renk kodlarıyla birlikte çevirir.
     * &#RRGGBB → §x§R§R§G§G§B§B (Paper hex format)
     * &X       → §X (legacy Minecraft format)
     * BungeeCord / Bukkit ChatColor kullanılmaz.
     *
     * @param text Çevrilecek metin
     * @return § tabanlı renk kodları uygulanmış metin
     */
    public static String translate(String text) {
        if (text == null) return "";

        // 1. &#RRGGBB → §x§R§R§G§G§B§B
        Matcher hexMatcher = HEX_PATTERN.matcher(text);
        StringBuilder hexBuffer = new StringBuilder();
        while (hexMatcher.find()) {
            String hex = hexMatcher.group(1);
            StringBuilder replacement = new StringBuilder("\u00a7x"); // §x
            for (char c : hex.toCharArray()) {
                replacement.append('\u00a7').append(c);              // §R §R §G §G §B §B
            }
            hexMatcher.appendReplacement(hexBuffer,
                    Matcher.quoteReplacement(replacement.toString()));
        }
        hexMatcher.appendTail(hexBuffer);

        // 2. &X → §X (yalnızca geçerli renk/format karakterleri)
        Matcher legacyMatcher = LEGACY_PATTERN.matcher(hexBuffer.toString());
        return legacyMatcher.replaceAll(m -> "\u00a7" + m.group(1));
    }

    /**
     * Birden fazla yer tutucu değişkeni metne uygular.
     * Kullanım: replacePlaceholders(text, "key1", "val1", "key2", "val2")
     *
     * @param text         Temel metin
     * @param replacements Çift sayıda argüman: anahtar, değer, anahtar, değer...
     * @return Yer tutucular uygulanmış metin
     */
    public static String replacePlaceholders(String text, String... replacements) {
        if (text == null) return "";
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            text = text.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return text;
    }
}
