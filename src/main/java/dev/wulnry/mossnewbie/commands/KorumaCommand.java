package dev.wulnry.mossnewbie.commands;

import dev.wulnry.mossnewbie.MossNewbie;
import dev.wulnry.mossnewbie.managers.ConfigManager;
import dev.wulnry.mossnewbie.managers.ProtectionManager;
import dev.wulnry.mossnewbie.utils.ColorUtils;
import dev.wulnry.mossnewbie.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /koruma komutu işleyicisi.
 *
 * <p>Oyuncu kullanımı:
 * <ul>
 *   <li>/koruma kapat – 5 saniyelik çift onay ile korumayı kaldır</li>
 * </ul>
 *
 * <p>Admin kullanımı (mossnewbie.admin):
 * <ul>
 *   <li>/koruma admin reload – Config'i yenile</li>
 *   <li>/koruma admin ver &lt;oyuncu&gt; – Koruma ver</li>
 *   <li>/koruma admin sil &lt;oyuncu&gt; – Korumayı sil</li>
 * </ul>
 */
public class KorumaCommand implements CommandExecutor, TabCompleter {

    private final MossNewbie plugin;
    private final ProtectionManager protectionManager;
    private final ConfigManager configManager;

    /**
     * UUID → Bekleyen onay görevi (BukkitTask).
     * Oyuncu ilk /koruma kapat yazdığında bir görev başlatılır ve buraya kaydedilir.
     * İkinci kez yazdığında görev iptal edilip koruma kaldırılır.
     */
    private final Map<UUID, BukkitTask> pendingConfirm = new HashMap<>();

    public KorumaCommand(MossNewbie plugin) {
        this.plugin = plugin;
        this.protectionManager = plugin.getProtectionManager();
        this.configManager = plugin.getConfigManager();
    }

    // ================================================================
    // KOMUT İŞLEYİCİ
    // ================================================================

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        // Konsol koruması: admin subkomutları hariç yalnızca oyuncular kullanabilir
        if (args.length == 0) {
            if (sender instanceof Player player) {
                MessageUtils.sendCfg(player, "messages.usage-player");
            } else {
                MessageUtils.sendCfg(sender, "messages.usage-admin");
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        // ---- OYUNCU: /koruma kapat ----
        if (sub.equals("kapat")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ColorUtils.translate("&cBu komutu yalnızca oyuncular kullanabilir."));
                return true;
            }
            handlePlayerRemove(player);
            return true;
        }

        // ---- ADMIN KOMUTLARI: /koruma admin ... ----
        if (sub.equals("admin")) {
            if (!sender.hasPermission("mossnewbie.admin")) {
                MessageUtils.sendCfg(sender, "messages.admin-no-permission");
                return true;
            }
            handleAdmin(sender, args);
            return true;
        }

        // Bilinmeyen alt komut
        if (sender instanceof Player player) {
            MessageUtils.sendCfg(player, "messages.usage-player");
        } else {
            MessageUtils.sendCfg(sender, "messages.usage-admin");
        }
        return true;
    }

    // ================================================================
    // OYUNCU: KORUMA KAPATMA (ÇİFT ONAY)
    // ================================================================

    private void handlePlayerRemove(Player player) {
        UUID uuid = player.getUniqueId();

        // Koruması var mı?
        if (!protectionManager.isProtected(uuid)) {
            MessageUtils.sendCfg(player, "messages.remove-no-protection");
            return;
        }

        // Bekleyen bir onay var mı? (ikinci kez yazdı)
        if (pendingConfirm.containsKey(uuid)) {
            // Onay görevi iptal et
            pendingConfirm.get(uuid).cancel();
            pendingConfirm.remove(uuid);

            // Korumayı kaldır
            protectionManager.removeProtection(uuid);

            // Bildirimler
            MessageUtils.sendTitle(player, "messages.remove-success-title", "messages.remove-success-subtitle");
            MessageUtils.sendCfg(player, "messages.remove-success-chat");
            playSound(player, "protection-removed");
            return;
        }

        // İLK KEZ yazdı – onay iste, 5 saniyelik zamanlayıcı başlat
        MessageUtils.sendCfg(player, "messages.remove-confirm");
        playSound(player, "protection-confirm-pending");

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                // 5 saniye geçti, onay gelmedi
                pendingConfirm.remove(uuid);
                if (player.isOnline()) {
                    MessageUtils.sendCfg(player, "messages.remove-timeout");
                    playSound(player, "protection-timeout");
                }
            }
        }.runTaskLater(plugin, 100L); // 100 tick = 5 saniye

        pendingConfirm.put(uuid, task);
    }

    // ================================================================
    // ADMIN KOMUTLARI
    // ================================================================

    private void handleAdmin(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtils.sendCfg(sender, "messages.usage-admin");
            return;
        }

        String adminSub = args[1].toLowerCase();

        switch (adminSub) {

            // /koruma admin reload
            case "reload" -> {
                configManager.reload();
                MessageUtils.sendCfg(sender, "messages.admin-reload");
            }

            // /koruma admin ver <oyuncu>
            case "ver" -> {
                if (args.length < 3) {
                    MessageUtils.sendCfg(sender, "messages.usage-admin");
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    MessageUtils.sendCfg(sender, "messages.admin-player-not-found",
                            "player", args[2]);
                    return;
                }

                if (protectionManager.isProtected(target.getUniqueId())) {
                    MessageUtils.sendCfg(sender, "messages.admin-already-has-protection",
                            "player", target.getName());
                    return;
                }

                protectionManager.giveProtection(target.getUniqueId());
                long durationMinutes = configManager.getProtectionDurationMinutes();
                String durationStr = formatDuration(durationMinutes);

                MessageUtils.sendCfg(sender, "messages.admin-give",
                        "player", target.getName(),
                        "duration", durationStr);

                // Hedef oyuncuya da bildir
                MessageUtils.sendTitle(target, "messages.join-title", "messages.join-subtitle");
                playSound(target, "admin-give");
            }

            // /koruma admin sil <oyuncu>
            case "sil" -> {
                if (args.length < 3) {
                    MessageUtils.sendCfg(sender, "messages.usage-admin");
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    MessageUtils.sendCfg(sender, "messages.admin-player-not-found",
                            "player", args[2]);
                    return;
                }

                if (!protectionManager.isProtected(target.getUniqueId())) {
                    MessageUtils.sendCfg(sender, "messages.admin-no-protection",
                            "player", target.getName());
                    return;
                }

                protectionManager.removeProtection(target.getUniqueId());
                cancelConfirm(target.getUniqueId());

                MessageUtils.sendCfg(sender, "messages.admin-remove",
                        "player", target.getName());

                // Hedef oyuncuya bildir
                MessageUtils.sendTitle(target, "messages.remove-success-title", "messages.remove-success-subtitle");
                playSound(target, "admin-remove");
            }

            default -> MessageUtils.sendCfg(sender, "messages.usage-admin");
        }
    }

    // ================================================================
    // TAB TAMAMLAMA
    // ================================================================

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subs = new java.util.ArrayList<>(List.of("kapat"));
            if (sender.hasPermission("mossnewbie.admin")) {
                subs.add("admin");
            }
            return filterStartsWith(subs, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("admin")
                && sender.hasPermission("mossnewbie.admin")) {
            return filterStartsWith(Arrays.asList("reload", "ver", "sil"), args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("admin")
                && (args[1].equalsIgnoreCase("ver") || args[1].equalsIgnoreCase("sil"))
                && sender.hasPermission("mossnewbie.admin")) {
            // Çevrimiçi oyuncu isimlerini öner
            List<String> names = new java.util.ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                names.add(p.getName());
            }
            return filterStartsWith(names, args[2]);
        }

        return Collections.emptyList();
    }

    // ================================================================
    // YARDIMCI METODLAR
    // ================================================================

    /**
     * Oyuncunun bekleyen onay görevini iptal eder (çıkış vb. durumlar için).
     */
    public void cancelConfirm(UUID uuid) {
        BukkitTask task = pendingConfirm.remove(uuid);
        if (task != null) {
            task.cancel();
        }
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

    private String formatDuration(long minutes) {
        if (minutes >= 60) {
            long h = minutes / 60;
            long m = minutes % 60;
            return m > 0 ? h + " saat " + m + " dakika" : h + " saat";
        }
        return minutes + " dakika";
    }

    private List<String> filterStartsWith(List<String> list, String prefix) {
        List<String> result = new java.util.ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(prefix.toLowerCase())) {
                result.add(s);
            }
        }
        return result;
    }
}
