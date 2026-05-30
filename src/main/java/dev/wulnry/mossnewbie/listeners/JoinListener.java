package dev.wulnry.mossnewbie.listeners;

import dev.wulnry.mossnewbie.MossNewbie;
import dev.wulnry.mossnewbie.managers.ConfigManager;
import dev.wulnry.mossnewbie.managers.ProtectionManager;
import dev.wulnry.mossnewbie.utils.ColorUtils;
import dev.wulnry.mossnewbie.utils.MessageUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Oyuncu giriş/çıkış olaylarını dinler.
 * - İlk giriş: Otomatik koruma ver, Title + ses.
 * - Çıkış: Verinin asenkron kaydını tetikle, ActionBar görevini iptal et.
 */
public class JoinListener implements Listener {

    private final MossNewbie plugin;
    private final ProtectionManager protectionManager;
    private final ConfigManager configManager;

    public JoinListener(MossNewbie plugin) {
        this.plugin = plugin;
        this.protectionManager = plugin.getProtectionManager();
        this.configManager = plugin.getConfigManager();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // ---- İlk kez mi giriyor? ----
        if (!player.hasPlayedBefore()) {
            protectionManager.giveProtection(player.getUniqueId());
            showProtectionActivatedFeedback(player);
        } else if (protectionManager.isProtected(player.getUniqueId())) {
            // Daha önce giriş yaptı ama koruması hâlâ aktif
            showProtectionReminderFeedback(player);
        }

        // ActionBar zamanlayıcısını başlat (koruma varsa)
        if (configManager.isShowActionbarTimer() && protectionManager.isProtected(player.getUniqueId())) {
            startActionBarTimer(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Veriyi asenkron kaydet
        protectionManager.saveDataAsync();
        // Çıkan oyuncunun bekleyen confirm görevini iptal et
        plugin.getKorumaCommand().cancelConfirm(player.getUniqueId());
    }

    // ----------------------------------------------------------------
    // YARDIMCI METODLAR
    // ----------------------------------------------------------------

    /** İlk giriş için Title + ses. */
    private void showProtectionActivatedFeedback(Player player) {
        long durationMinutes = configManager.getProtectionDurationMinutes();
        String durationStr = formatDuration(durationMinutes);

        // Chat mesajı
        String chatRaw = ColorUtils.replacePlaceholders(
                configManager.getMessage("messages.join-chat"),
                "player", player.getName(),
                "duration", durationStr
        );
        String prefix = ColorUtils.translate(configManager.getMessage("messages.prefix"));
        player.sendMessage(prefix + ColorUtils.translate(chatRaw));

        // Title
        MessageUtils.sendTitle(player, "messages.join-title", "messages.join-subtitle");

        // Ses
        playSound(player, "protection-start");
    }

    /** Tekrar giriş için hafif hatırlatma (sadece ActionBar + chat). */
    private void showProtectionReminderFeedback(Player player) {
        String remaining = protectionManager.getRemainingFormatted(player.getUniqueId());
        String actionbarRaw = ColorUtils.replacePlaceholders(
                configManager.getMessage("messages.actionbar-format"),
                "remaining", remaining
        );
        MessageUtils.sendActionBar(player, actionbarRaw);
    }

    /** ActionBar zamanlayıcısını periyodik olarak çalıştırır. */
    private void startActionBarTimer(Player player) {
        long interval = configManager.getActionbarUpdateInterval();

        new BukkitRunnable() {
            @Override
            public void run() {
                // Oyuncu çevrimdışıysa görevi iptal et
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                // Koruma bitti mi?
                if (!protectionManager.isProtected(player.getUniqueId())) {
                    // Son bildirim
                    String expiredMsg = configManager.getMessage("messages.actionbar-expired");
                    MessageUtils.sendActionBar(player, expiredMsg);
                    cancel();
                    return;
                }

                // Kalan süreyi göster
                String remaining = protectionManager.getRemainingFormatted(player.getUniqueId());
                String raw = ColorUtils.replacePlaceholders(
                        configManager.getMessage("messages.actionbar-format"),
                        "remaining", remaining
                );
                MessageUtils.sendActionBar(player, raw);
            }
        }.runTaskTimer(plugin, interval, interval);
    }

    private String formatDuration(long minutes) {
        if (minutes >= 60) {
            return (minutes / 60) + " saat " + (minutes % 60 != 0 ? (minutes % 60) + " dakika" : "");
        }
        return minutes + " dakika";
    }

    private void playSound(Player player, String soundKey) {
        try {
            String soundName = configManager.getSoundName(soundKey);
            Sound sound = Sound.valueOf(soundName);
            float volume = configManager.getSoundVolume(soundKey);
            float pitch  = configManager.getSoundPitch(soundKey);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Geçersiz ses adı: " + configManager.getSoundName(soundKey));
        }
    }
}
