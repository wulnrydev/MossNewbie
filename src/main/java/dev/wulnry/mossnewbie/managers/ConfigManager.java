package dev.wulnry.mossnewbie.managers;

import dev.wulnry.mossnewbie.MossNewbie;
import dev.wulnry.mossnewbie.utils.ColorUtils;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Config.yml verilerine kolay erişim sağlayan sarmalayıcı sınıf.
 * Reload desteği ile birlikte gelir.
 */
public class ConfigManager {

    private final MossNewbie plugin;
    private FileConfiguration config;

    public ConfigManager(MossNewbie plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * Config'i diskten yeniden yükler.
     */
    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    // ----------------------------------------------------------------
    // GENEL AYARLAR
    // ----------------------------------------------------------------

    /** Koruma süresi dakika cinsinden. */
    public long getProtectionDurationMinutes() {
        return config.getLong("settings.protection-duration-minutes", 120L);
    }

    /** Koruma süresi milisaniye cinsinden. */
    public long getProtectionDurationMillis() {
        return getProtectionDurationMinutes() * 60_000L;
    }

    /** PvE hasarı engellensin mi? */
    public boolean isBlockPveDamage() {
        return config.getBoolean("settings.block-pve-damage", true);
    }

    /** ActionBar zamanlayıcısı aktif mi? */
    public boolean isShowActionbarTimer() {
        return config.getBoolean("settings.show-actionbar-timer", true);
    }

    /** ActionBar güncelleme periyodu (tick). */
    public long getActionbarUpdateInterval() {
        return config.getLong("settings.actionbar-update-interval-ticks", 40L);
    }

    // ----------------------------------------------------------------
    // WORLDGUARD AYARLARI
    // ----------------------------------------------------------------

    public boolean isWorldGuardEnabled() {
        return config.getBoolean("worldguard.enabled", true);
    }

    public java.util.List<String> getBlockedRegions() {
        return config.getStringList("worldguard.blocked-regions");
    }

    public double getPushBackStrength() {
        return config.getDouble("worldguard.push-back-strength", 0.8);
    }

    public String getDenyTitle() {
        return config.getString("worldguard.deny-title", "&#FF4444&l⚠ BÖLGEYE GİRİŞ ENGELLENDİ");
    }

    public String getDenySubtitle() {
        return config.getString("worldguard.deny-subtitle", "&#ffd500Bu alana erişim izniniz bulunmuyor!");
    }

    // ----------------------------------------------------------------
    // SES AYARLARI
    // ----------------------------------------------------------------

    public String getSoundName(String key) {
        return config.getString("sounds." + key + ".sound", "BLOCK_NOTE_BLOCK_PLING");
    }

    public float getSoundVolume(String key) {
        return (float) config.getDouble("sounds." + key + ".volume", 1.0);
    }

    public float getSoundPitch(String key) {
        return (float) config.getDouble("sounds." + key + ".pitch", 1.0);
    }

    // ----------------------------------------------------------------
    // MESAJLAR
    // ----------------------------------------------------------------

    /**
     * Config'deki verilen path'ten mesajı okur (renk çevirisiz ham hali).
     */
    public String getMessage(String path) {
        String raw = config.getString(path, "&cMesaj bulunamadı: " + path);
        return raw;
    }

    /**
     * Config'deki mesajı okur ve renk kodlarını çevirir.
     */
    public String getColoredMessage(String path) {
        return ColorUtils.translate(getMessage(path));
    }
}
