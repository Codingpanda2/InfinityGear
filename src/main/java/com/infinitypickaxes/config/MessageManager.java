package com.infinitypickaxes.config;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.core.pickaxe.InfinityPickaxe;
import com.infinitypickaxes.utils.ProgressBarUtil;
import com.infinitypickaxes.utils.TextUtil;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MessageManager {

    private final InfinityPickaxes plugin;
    private final Map<UUID, Double> accumulatedStreakXp = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastMinedTime = new ConcurrentHashMap<>();

    public MessageManager(InfinityPickaxes plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration getSenderConfig(CommandSender sender) {
        if (sender instanceof Player player) {
            String lang = player.locale().getLanguage();
            return plugin.getConfigManager().getLocaleConfig(lang);
        }
        return plugin.getConfigManager().getMessagesConfig();
    }

    public String getPrefix(CommandSender sender) {
        return getSenderConfig(sender).getString("prefix", "<gradient:#00E5FF:#0077FE><b>InfinityPickaxes</b></gradient> <dark_gray>»</dark_gray> ");
    }

    public void sendMessage(CommandSender sender, String key, String... placeholders) {
        if (sender == null) return;
        FileConfiguration config = getSenderConfig(sender);
        String message = config.getString(key);
        if (message == null || message.isEmpty()) {
            // Fallback to English
            config = plugin.getConfigManager().getLocaleConfig("en");
            message = config.getString(key);
        }
        if (message == null || message.isEmpty()) return;

        message = applyPlaceholders(message, placeholders);
        sender.sendMessage(TextUtil.parse(getPrefix(sender) + message));
    }

    public void sendRawMessage(CommandSender sender, String rawMessage, String... placeholders) {
        if (sender == null || rawMessage == null) return;
        sender.sendMessage(TextUtil.parse(applyPlaceholders(rawMessage, placeholders)));
    }

    public void sendMiningActionbar(Player player, InfinityPickaxe pickaxe, double gainedXp) {
        if (player == null || pickaxe == null) return;
        FileConfiguration messagesConfig = getSenderConfig(player);
        FileConfiguration config = plugin.getConfigManager().getConfig();

        if (!messagesConfig.getBoolean("mining-actionbar.enabled", true)) {
            return;
        }

        long now = System.currentTimeMillis();
        UUID uuid = player.getUniqueId();
        Long lastTime = lastMinedTime.get(uuid);
        long timeout = messagesConfig.getLong("mining-actionbar.streak-timeout-ms", 2500L);

        double streakXp;
        if (lastTime != null && (now - lastTime) < timeout) {
            streakXp = accumulatedStreakXp.getOrDefault(uuid, 0.0) + gainedXp;
        } else {
            streakXp = gainedXp;
        }

        accumulatedStreakXp.put(uuid, streakXp);
        lastMinedTime.put(uuid, now);

        String template = messagesConfig.getString("mining-actionbar.message",
                "<gradient:#00FF88:#00E5FF><b>+%gained_xp% XP</b></gradient> <dark_gray>┃</dark_gray> %xp_bar% <dark_gray>┃</dark_gray> <yellow>%current_xp%<gray>/<yellow>%required_xp% XP <dark_gray>(<gold>Lv.%level%<dark_gray>)");

        double reqXp = plugin.getLevelManager().getRequiredXp(pickaxe.getLevel());
        String bar = ProgressBarUtil.getProgressBar(
                pickaxe.getXp(),
                reqXp,
                config.getInt("progress-bar.total-bars", 10),
                config.getString("progress-bar.completed-symbol", "■"),
                config.getString("progress-bar.uncompleted-symbol", "□"),
                config.getString("progress-bar.completed-color", "<#00FF88>"),
                config.getString("progress-bar.uncompleted-color", "<#555555>")
        );

        String formatted = template
                .replace("%gained_xp%", String.format("%.0f", streakXp))
                .replace("%single_block_xp%", String.format("%.0f", gainedXp))
                .replace("%current_xp%", String.format("%.0f", pickaxe.getXp()))
                .replace("%required_xp%", String.format("%.0f", reqXp))
                .replace("%xp_bar%", bar)
                .replace("%level%", String.valueOf(pickaxe.getLevel()))
                .replace("%blocks_mined%", String.valueOf(pickaxe.getBlocksMined()));

        player.sendActionBar(TextUtil.parse(formatted));
    }

    public void sendLevelUp(Player player, InfinityPickaxe pickaxe, int oldLevel, int newLevel) {
        if (player == null || pickaxe == null) return;
        FileConfiguration config = getSenderConfig(player);

        // 1. Chat Message
        List<String> chatLines = config.getStringList("messages.level-up-chat");
        String summary = config.getString("messages.level-up-unlocks-summary", "");
        for (String line : chatLines) {
            String processed = line.replace("%level%", String.valueOf(newLevel))
                                   .replace("%old_level%", String.valueOf(oldLevel))
                                   .replace("%player%", player.getName())
                                   .replace("%unlocks_summary%", summary);
            player.sendMessage(TextUtil.parse(processed));
        }

        // 2. Title & Subtitle
        String titleStr = config.getString("messages.level-up-title", "<gradient:#00FF88:#00E5FF><b>LEVEL %level%!</b></gradient>")
                .replace("%level%", String.valueOf(newLevel));
        String subtitleStr = config.getString("messages.level-up-subtitle", "<gray>New sockets and abilities unlocked!</gray>")
                .replace("%level%", String.valueOf(newLevel));

        Title title = Title.title(
                TextUtil.parse(titleStr),
                TextUtil.parse(subtitleStr),
                Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(1800), Duration.ofMillis(500))
        );
        player.showTitle(title);

        // 3. Actionbar
        String actionbarStr = config.getString("messages.level-up-actionbar", "<green>+1 Pickaxe Level <dark_gray>(<yellow>Lv.%level%<dark_gray>)</green>")
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
