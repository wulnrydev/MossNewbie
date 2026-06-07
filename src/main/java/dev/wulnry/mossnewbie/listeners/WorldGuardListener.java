package dev.wulnry.mossnewbie.listeners;

import dev.wulnry.mossnewbie.MossNewbie;
import dev.wulnry.mossnewbie.managers.ConfigManager;
import dev.wulnry.mossnewbie.managers.ProtectionManager;
import dev.wulnry.mossnewbie.utils.ColorUtils;
import dev.wulnry.mossnewbie.utils.MessageUtils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * WorldGuard bölge kontrolü – yeni oyuncuların yasak bölgelere girmesini engeller.
 * Bölgeye girerlerse geri itilir ve ekranlarında uyarı mesajı çıkar.
 */
public class WorldGuardListener implements Listener {

    private final MossNewbie plugin;
    private final ProtectionManager protectionManager;
    private final ConfigManager configManager;

    public WorldGuardListener(MossNewbie plugin) {
        this.plugin = plugin;
        this.protectionManager = plugin.getProtectionManager();
        this.configManager = plugin.getConfigManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Sadece blok değişikliklerini işle (performans optimizasyonu)
        if (!hasMovedBlock(event.getFrom(), event.getTo())) return;

        Player player = event.getPlayer();

        // Koruması yoksa bu kontrolü atlayabiliriz
        if (!protectionManager.isProtected(player.getUniqueId())) return;

        // WorldGuard aktif mi?
        if (!configManager.isWorldGuardEnabled()) return;
        if (!plugin.getWorldGuardHook().isAvailable()) return;

        List<String> blockedRegions = configManager.getBlockedRegions();
        if (blockedRegions.isEmpty()) return;

        Location toLocation = event.getTo();
        if (toLocation == null) return;

        // Hedef konum yasaklı bölgede mi?
        if (plugin.getWorldGuardHook().isInBlockedRegion(toLocation, blockedRegions)) {
            // event.setCancelled(true) kullanmak setVelocity ile çakışıp oyuncuyu havada dondurur.
            // Bunun yerine oyuncuyu geldiği yere ışınlayıp öyle geriye itiyoruz.
            player.teleport(event.getFrom());

            // Oyuncuyu geri it
            pushBack(player, event.getFrom(), toLocation);

            // Ekranda uyarı göster
            String denyTitle    = configManager.getDenyTitle();
            String denySubtitle = configManager.getDenySubtitle();
            MessageUtils.sendRawTitle(player, denyTitle, denySubtitle);

            // Ses çal
            playSound(player, "worldguard-deny");
        }
    }

    // ----------------------------------------------------------------
    // YARDIMCI METODLAR
    // ----------------------------------------------------------------

    /**
     * Oyuncuyu geldiği yönün tersine iter.
     */
    private void pushBack(Player player, Location from, Location to) {
        double strength = configManager.getPushBackStrength();

        // "from" ile "to" arasındaki yön vektörünü tersine çevir (bölgeden dışarı it)
        Vector direction = from.toVector().subtract(to.toVector()).normalize();
        if (direction.isZero()) {
            // Hareket yönü hesaplanamıyorsa basit geri TP yap
            player.teleport(from);
            return;
        }

        // Oyuncuyu havaya hafif kaldır (sürtünmeyi azaltıp uzağa atması için)
        direction.setY(0.4); 
        // 3-4 blok itmesi için configdeki değeri 2.5 katına çıkartıyoruz
        direction.multiply(strength * 2.5);
        player.setVelocity(direction);
    }

    /**
     * Yalnızca blok düzeyinde hareketi algılar (CPU optimizasyonu).
     */
    private boolean hasMovedBlock(Location from, Location to) {
        if (to == null) return false;
        return from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ();
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
