package com.infinitypickaxes.config;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import com.infinitypickaxes.utils.TextUtil;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.List;

public class MessageManager {

    private final InfinityPickaxes plugin;

    public MessageManager(InfinityPickaxes plugin) {
        this.plugin = plugin;
    }

    public String getPrefix() {
        return plugin.getConfigManager().getMessagesConfig().getString("prefix", "<gradient:#00E5FF:#0077FE><b>InfinityPickaxes</b></gradient> <dark_gray>»</dark_gray> ");
    }

    public void sendMessage(CommandSender sender, String key, String... placeholders) {
        if (sender == null) return;
        FileConfiguration config = plugin.getConfigManager().getMessagesConfig();
        String message = config.getString(key);
        if (message == null || message.isEmpty()) return;

        message = applyPlaceholders(message, placeholders);
        sender.sendMessage(TextUtil.parse(getPrefix() + message));
    }

    public void sendRawMessage(CommandSender sender, String rawMessage, String... placeholders) {
        if (sender == null || rawMessage == null) return;
        sender.sendMessage(TextUtil.parse(applyPlaceholders(rawMessage, placeholders)));
    }

    public void sendLevelUp(Player player, InfinityPickaxe pickaxe, int oldLevel, int newLevel) {
        if (player == null || pickaxe == null) return;
        FileConfiguration config = plugin.getConfigManager().getMessagesConfig();

        // 1. Chat Message
        List<String> chatLines = config.getStringList("messages.level-up-chat");
        for (String line : chatLines) {
            String processed = line.replace("%level%", String.valueOf(newLevel))
                                   .replace("%old_level%", String.valueOf(oldLevel))
                                   .replace("%player%", player.getName())
                                   .replace("%unlocks_summary%", "<center><green>Nuevos límites de encantamiento y habilidades disponibles</green></center>");
            player.sendMessage(TextUtil.parse(processed));
        }

        // 2. Title & Subtitle
        String titleStr = config.getString("messages.level-up-title", "<gradient:#00FF88:#00E5FF><b>¡NIVEL %level%!</b></gradient>")
                .replace("%level%", String.valueOf(newLevel));
        String subtitleStr = config.getString("messages.level-up-subtitle", "<gray>¡Nuevos sockets y habilidades desbloqueados!</gray>")
                .replace("%level%", String.valueOf(newLevel));

        Title title = Title.title(
                TextUtil.parse(titleStr),
                TextUtil.parse(subtitleStr),
                Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(1800), Duration.ofMillis(500))
        );
        player.showTitle(title);

        // 3. Actionbar
        String actionbarStr = config.getString("messages.level-up-actionbar", "<green>+1 Nivel de Pico <dark_gray>(<yellow>Nv.%level%<dark_gray>)</green>")
                .replace("%level%", String.valueOf(newLevel));
        player.sendActionBar(TextUtil.parse(actionbarStr));
    }

    private String applyPlaceholders(String text, String... placeholders) {
        if (placeholders == null || placeholders.length < 2) return text;
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length && placeholders[i] != null && placeholders[i + 1] != null) {
                text = text.replace(placeholders[i], placeholders[i + 1]);
            }
        }
        return text;
    }
}
