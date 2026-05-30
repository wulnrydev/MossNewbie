package dev.wulnry.mossnewbie.hooks;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * WorldGuard API entegrasyon sınıfı.
 * Sunucuda WorldGuard kurulu değilse tüm metodlar güvenle false döner.
 */
public class WorldGuardHook {

    private final boolean available;

    public WorldGuardHook() {
        boolean found;
        try {
            // WorldGuard API erişim testi
            WorldGuard.getInstance().getPlatform().getRegionContainer();
            found = true;
        } catch (Throwable t) {
            found = false;
        }
        this.available = found;
    }

    /** WorldGuard eklentisi mevcut ve çalışıyor mu? */
    public boolean isAvailable() {
        return available;
    }

    /**
     * Verilen konumun, yasaklı bölgeler listesindeki bölgelerden herhangi birinin
     * içinde olup olmadığını kontrol eder.
     *
     * @param location       Kontrol edilecek konum
     * @param blockedRegions Yasaklı bölge isimlerinin listesi
     * @return Konum yasaklı bir bölgedeyse true
     */
    public boolean isInBlockedRegion(Location location, List<String> blockedRegions) {
        if (!available || location.getWorld() == null || blockedRegions.isEmpty()) {
            return false;
        }

        try {
            RegionContainer container = WorldGuard.getInstance()
                    .getPlatform().getRegionContainer();

            RegionManager manager = container.get(
                    BukkitAdapter.adapt(location.getWorld())
            );

            if (manager == null) return false;

            // Konumun bulunduğu tüm bölgeleri al
            com.sk89q.worldguard.protection.ApplicableRegionSet regions =
                    manager.getApplicableRegions(
                            BukkitAdapter.asBlockVector(location)
                    );

            for (ProtectedRegion region : regions) {
                if (blockedRegions.contains(region.getId())) {
                    return true;
                }
            }
        } catch (Throwable t) {
            // API hatası – güvenli dön
        }

        return false;
    }

    /**
     * WorldGuard oyuncu API'sine erişim (ileride kullanılabilir).
     */
    public com.sk89q.worldguard.LocalPlayer wrapPlayer(Player player) {
        return WorldGuardPlugin.inst().wrapPlayer(player);
    }
}
