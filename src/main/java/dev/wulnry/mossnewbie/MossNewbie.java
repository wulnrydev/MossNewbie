package dev.wulnry.mossnewbie;

import dev.wulnry.mossnewbie.commands.KorumaCommand;
import dev.wulnry.mossnewbie.hooks.WorldGuardHook;
import dev.wulnry.mossnewbie.listeners.DamageListener;
import dev.wulnry.mossnewbie.listeners.JoinListener;
import dev.wulnry.mossnewbie.listeners.WorldGuardListener;
import dev.wulnry.mossnewbie.managers.ConfigManager;
import dev.wulnry.mossnewbie.managers.ProtectionManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * MossNewbie – BoxPvP Yeni Oyuncu Koruma Eklentisi
 *
 * <p>Ana plugin sınıfı. Tüm manager, hook, listener ve command nesnelerini
 * burada başlatır ve singleton erişimi sağlar.</p>
 *
 * @author wulnrydev
 * @version 1.0.0
 */
public final class MossNewbie extends JavaPlugin {

    // ----------------------------------------------------------------
    // Singleton
    // ----------------------------------------------------------------
    private static MossNewbie instance;

    // ----------------------------------------------------------------
    // Bileşenler
    // ----------------------------------------------------------------
    private ConfigManager configManager;
    private ProtectionManager protectionManager;
    private WorldGuardHook worldGuardHook;
    private KorumaCommand korumaCommand;

    // ================================================================
    // YAŞAM DÖNGÜSÜ
    // ================================================================

    @Override
    public void onEnable() {
        instance = this;

        // 1) Config yöneticisi
        configManager = new ConfigManager(this);

        // 2) Koruma veri yöneticisi (data.yml yükleme dahil)
        protectionManager = new ProtectionManager(this);

        // 3) WorldGuard hook
        worldGuardHook = new WorldGuardHook();
        if (worldGuardHook.isAvailable()) {
            getLogger().info("WorldGuard hook başarıyla yüklendi.");
        } else {
            getLogger().warning("WorldGuard bulunamadı. Bölge engelleme özelliği devre dışı.");
        }

        // 4) Komut işleyicisi
        korumaCommand = new KorumaCommand(this);
        PluginCommand cmd = getCommand("koruma");
        if (cmd != null) {
            cmd.setExecutor(korumaCommand);
            cmd.setTabCompleter(korumaCommand);
        } else {
            getLogger().severe("'koruma' komutu plugin.yml'de tanımlı değil! Komutlar çalışmayacak.");
        }

        // 5) Event listener'lar
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(new DamageListener(this), this);

        // WorldGuard yalnızca hook aktifse ve config'de açıksa kayıt edilsin
        if (worldGuardHook.isAvailable() && configManager.isWorldGuardEnabled()) {
            getServer().getPluginManager().registerEvents(new WorldGuardListener(this), this);
        }

        getLogger().info("MossNewbie v" + getPluginMeta().getVersion() + " başarıyla etkinleştirildi!");
    }

    @Override
    public void onDisable() {
        // Tüm veriyi senkron olarak kaydet (async scheduler kapandıktan sonra çalışmaz)
        if (protectionManager != null) {
            protectionManager.saveDataSync();
            getLogger().info("Koruma verileri kaydedildi.");
        }

        getLogger().info("MossNewbie devre dışı bırakıldı.");
        instance = null;
    }

    // ================================================================
    // ERİŞİMCİLER
    // ================================================================

    /** Plugin singleton'ı döndürür. */
    public static MossNewbie getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ProtectionManager getProtectionManager() {
        return protectionManager;
    }

    public WorldGuardHook getWorldGuardHook() {
        return worldGuardHook;
    }

    public KorumaCommand getKorumaCommand() {
        return korumaCommand;
    }
}
