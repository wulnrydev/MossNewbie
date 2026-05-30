package dev.wulnry.mossnewbie.listeners;

import dev.wulnry.mossnewbie.MossNewbie;
import dev.wulnry.mossnewbie.managers.ConfigManager;
import dev.wulnry.mossnewbie.managers.ProtectionManager;
import dev.wulnry.mossnewbie.utils.ColorUtils;
import dev.wulnry.mossnewbie.utils.MessageUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.entity.Projectile;

/**
 * PvP ve PvE hasar olaylarını dinler.
 * Korumalı oyunculara gelen/giden hasarı engeller.
 */
public class DamageListener implements Listener {

    private final MossNewbie plugin;
    private final ProtectionManager protectionManager;
    private final ConfigManager configManager;

    public DamageListener(MossNewbie plugin) {
        this.plugin = plugin;
        this.protectionManager = plugin.getProtectionManager();
        this.configManager = plugin.getConfigManager();
    }

    /**
     * HIGHEST öncelikte, iptal edilmiş olayları atlayarak dinler.
     * Bu sayede diğer eklentilerle uyumlu çalışır ve gereksiz işlem yükünden kaçınılır.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity victim  = event.getEntity();

        // Eğer saldıran bir ok, iksir vs. ise asıl fırlatanı (shooter) damager olarak kabul et
        if (damager instanceof Projectile proj) {
            if (proj.getShooter() instanceof Entity shooter) {
                damager = shooter;
            }
        }

        // ---- Kurban Oyuncu mu? ----
        if (victim instanceof Player victimPlayer) {
            if (protectionManager.isProtected(victimPlayer.getUniqueId())) {

                // Saldıran diğer bir oyuncuysa (PvP) → her durumda engelle
                if (damager instanceof Player attackerPlayer) {
                    event.setCancelled(true);
                    notifyAttackerBlocked(attackerPlayer, victimPlayer);
                    notifyVictimProtected(victimPlayer);
                    playSound(attackerPlayer, "protection-hit-blocked");
                    return;
                }

                // Saldıran mob/yaratıksa (PvE) → config ayarına bak
                if (configManager.isBlockPveDamage()) {
                    event.setCancelled(true);
                    // PvE engellendi – sessiz, ActionBar'dan görünür
                }
                return;
            }
        }

        // ---- Saldıran Oyuncu mu? (korumalı oyuncu başkasına vuruyor mu?) ----
        if (damager instanceof Player attackerPlayer && victim instanceof Player) {
            if (protectionManager.isProtected(attackerPlayer.getUniqueId())) {
                event.setCancelled(true);
                String msg = ColorUtils.translate(
                        ColorUtils.replacePlaceholders(
                                configManager.getMessage("messages.attacker-blocked"),
                                "target", victim.getName()
                        )
                );
                String prefix = ColorUtils.translate(configManager.getMessage("messages.prefix"));
                attackerPlayer.sendMessage(prefix + msg);
                playSound(attackerPlayer, "protection-hit-blocked");
            }
        }
    }

    // ----------------------------------------------------------------
    // YARDIMCI METODLAR
    // ----------------------------------------------------------------

    private void notifyAttackerBlocked(Player attacker, Player victim) {
        String raw = configManager.getMessage("messages.attacker-blocked");
        String msg = ColorUtils.translate(
                ColorUtils.replacePlaceholders(raw, "target", victim.getName())
        );
        String prefix = ColorUtils.translate(configManager.getMessage("messages.prefix"));
        attacker.sendMessage(prefix + msg);
    }

    private void notifyVictimProtected(Player victim) {
        String raw = configManager.getMessage("messages.victim-blocked");
        String prefix = ColorUtils.translate(configManager.getMessage("messages.prefix"));
        victim.sendMessage(prefix + ColorUtils.translate(raw));
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
