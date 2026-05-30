package dev.wulnry.mossnewbie.managers;

import dev.wulnry.mossnewbie.MossNewbie;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Yeni oyuncu koruma verilerini yöneten sınıf.
 * <p>
 * Koruma bitiş zamanları UUID → Long (epoch ms) şeklinde bellekte tutulur.
 * Oyuncu sunucudan ayrıldığında veri asenkron olarak data.yml'e yazılır.
 * Sunucu kapanırken senkron kayıt yapılır.
 */
public class ProtectionManager {

    private final MossNewbie plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;

    /**
     * UUID → Koruma bitiş zamanı (epoch milisaniye)
     */
    private final Map<UUID, Long> protectionMap = new HashMap<>();

    public ProtectionManager(MossNewbie plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        loadData();
    }

    // ----------------------------------------------------------------
    // TEMEL KORUMA İŞLEMLERİ
    // ----------------------------------------------------------------

    /**
     * Verilen oyuncunun korumasının aktif olup olmadığını kontrol eder.
     */
    public boolean isProtected(UUID uuid) {
        Long expiresAt = protectionMap.get(uuid);
        if (expiresAt == null) return false;

        if (System.currentTimeMillis() < expiresAt) {
            return true;
        }

        // Süresi dolmuş – temizle
        protectionMap.remove(uuid);
        return false;
    }

    /**
     * Oyuncuya config'de tanımlı süre kadar koruma verir.
     */
    public void giveProtection(UUID uuid) {
        long duration = plugin.getConfigManager().getProtectionDurationMillis();
        protectionMap.put(uuid, System.currentTimeMillis() + duration);
    }

    /**
     * Oyuncuya belirli bir süre (dakika) kadar koruma verir.
     */
    public void giveProtection(UUID uuid, long minutes) {
        protectionMap.put(uuid, System.currentTimeMillis() + (minutes * 60_000L));
    }

    /**
     * Oyuncunun korumasını anında kaldırır.
     */
    public void removeProtection(UUID uuid) {
        protectionMap.remove(uuid);
    }

    /**
     * Oyuncunun kalan koruma süresini milisaniye cinsinden döndürür.
     * Koruması yoksa veya süresi dolmuşsa 0 döner.
     */
    public long getRemainingMillis(UUID uuid) {
        Long expiresAt = protectionMap.get(uuid);
        if (expiresAt == null) return 0L;
        long remaining = expiresAt - System.currentTimeMillis();
        return Math.max(0L, remaining);
    }

    /**
     * Kalan süreyi "SSss" / "DDss" / "ss sn" formatında döndürür.
     */
    public String getRemainingFormatted(UUID uuid) {
        long millis = getRemainingMillis(uuid);
        long totalSeconds = millis / 1000;

        long hours   = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%ds %02d dk %02d sn", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%d dk %02d sn", minutes, seconds);
        } else {
            return String.format("%d sn", seconds);
        }
    }

    // ----------------------------------------------------------------
    // VERİ KAYIT / YÜKLEME
    // ----------------------------------------------------------------

    /**
     * data.yml'den mevcut koruma verilerini belleğe yükler.
     * Süresi geçmiş kayıtlar otomatik atlanır.
     */
    public void loadData() {
        if (!dataFile.exists()) {
            dataConfig = new YamlConfiguration();
            return;
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        long now = System.currentTimeMillis();

        if (dataConfig.isConfigurationSection("data")) {
            for (String key : dataConfig.getConfigurationSection("data").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    long expiresAt = dataConfig.getLong("data." + key);
                    if (expiresAt > now) {
                        protectionMap.put(uuid, expiresAt);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Geçersiz UUID veri satırı atlandı: " + key);
                }
            }
        }

        plugin.getLogger().info(protectionMap.size() + " aktif koruma kaydı yüklendi.");
    }

    /**
     * Mevcut koruma verilerini asenkron olarak data.yml'e kaydeder.
     */
    public void saveDataAsync() {
        // Snapshot al (thread safety için)
        final Map<UUID, Long> snapshot = new HashMap<>(protectionMap);

        new BukkitRunnable() {
            @Override
            public void run() {
                writeToFile(snapshot);
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Mevcut koruma verilerini senkron olarak data.yml'e kaydeder.
     * onDisable sırasında kullanılır.
     */
    public void saveDataSync() {
        writeToFile(new HashMap<>(protectionMap));
    }

    private synchronized void writeToFile(Map<UUID, Long> data) {
        FileConfiguration yml = new YamlConfiguration();
        long now = System.currentTimeMillis();

        for (Map.Entry<UUID, Long> entry : data.entrySet()) {
            // Süresi geçmiş kayıtları kaydetme
            if (entry.getValue() > now) {
                yml.set("data." + entry.getKey().toString(), entry.getValue());
            }
        }

        try {
            yml.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "data.yml kaydedilemedi!", e);
        }
    }

    // ----------------------------------------------------------------
    // YARDIMCI
    // ----------------------------------------------------------------

    /** Tüm koruma haritasına erişim (admin amaçlı). */
    public Map<UUID, Long> getProtectionMap() {
        return protectionMap;
    }
}
