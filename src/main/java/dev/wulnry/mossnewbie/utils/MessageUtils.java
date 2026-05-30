package dev.wulnry.mossnewbie.utils;

import dev.wulnry.mossnewbie.MossNewbie;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.time.Duration;

/**
 * Mesaj gönderme yardımcı sınıfı.
 * Prefix ekleme, renk çevirme ve oyuncuya/konsola mesaj gönderme işlemleri.
 */
public final class MessageUtils {

    private MessageUtils() {}

    /**
     * Prefix eklenmiş, renk kodları çevrilmiş mesajı sender'a gönderir.
     */
    public static void send(CommandSender sender, String rawMessage) {
        String prefix = ColorUtils.translate(
                MossNewbie.getInstance().getConfigManager().getMessage("messages.prefix")
        );
        sender.sendMessage(prefix + ColorUtils.translate(rawMessage));
    }

    /**
     * Prefix ve yer tutucu uygulamasıyla birlikte mesaj gönderir.
     */
    public static void send(CommandSender sender, String rawMessage, String... placeholders) {
        String replaced = ColorUtils.replacePlaceholders(rawMessage, placeholders);
        send(sender, replaced);
    }

    /**
     * Config'den mesaj okuyarak prefix ile gönderir.
     */
    public static void sendCfg(CommandSender sender, String configPath) {
        String raw = MossNewbie.getInstance().getConfigManager().getMessage(configPath);
        send(sender, raw);
    }

    /**
     * Config'den mesaj okuyup yer tutucuları uygulayarak gönderir.
     */
    public static void sendCfg(CommandSender sender, String configPath, String... placeholders) {
        String raw = MossNewbie.getInstance().getConfigManager().getMessage(configPath);
        send(sender, raw, placeholders);
    }

    /**
     * Sadece Player'a ActionBar mesajı gönderir.
     */
    public static void sendActionBar(Player player, String rawMessage) {
        player.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(
                ColorUtils.translate(rawMessage)
        ));
    }

    /**
     * Player'a Title gönderir.
     */
    public static void sendTitle(Player player, String titlePath, String subtitlePath) {
        String title = ColorUtils.translate(MossNewbie.getInstance().getConfigManager().getMessage(titlePath));
        String subtitle = ColorUtils.translate(MossNewbie.getInstance().getConfigManager().getMessage(subtitlePath));

        Title titleObj = Title.title(
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(title),
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(subtitle),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000))
        );
        player.showTitle(titleObj);
    }

    /**
     * Player'a ham title gönderir (config path değil, doğrudan string).
     */
    public static void sendRawTitle(Player player, String title, String subtitle) {
        Title titleObj = Title.title(
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(ColorUtils.translate(title)),
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(ColorUtils.translate(subtitle)),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000))
        );
        player.showTitle(titleObj);
    }
}
