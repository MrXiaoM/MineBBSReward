package top.mrxiaom.minebbsreward.utils;

import com.google.common.collect.Lists;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import top.mrxiaom.minebbsreward.MineBBSReward;

import java.util.List;

public class PAPI {
    private static boolean isEnabled = false;
    protected static void init(MineBBSReward plugin) {
        isEnabled = Util.isPresent("me.clip.placeholderapi.PlaceholderAPI");
        if (isEnabled) registerPlaceholder(plugin);
    }

    private static void registerPlaceholder(MineBBSReward plugin) {
        Placeholder placeholder = new Placeholder(plugin);
        PlaceholderAPIPlugin.getInstance()
                .getLocalExpansionManager()
                .findExpansionByIdentifier(placeholder.getIdentifier())
                .ifPresent(PlaceholderExpansion::unregister);
        if (placeholder.isRegistered()) return;
        boolean result = placeholder.register();
        if (!result) {
            plugin.getLogger().info("无法注册 " + placeholder.getName() + " PAPI 变量");
        }
    }

    public static List<String> setPlaceholders(OfflinePlayer player, List<String> s) {
        if (!isEnabled) return player == null ? s : Lists.newArrayList(String.join("\n", s).replace("%player_name%", String.valueOf(player.getName())).split("\n"));
        return PlaceholderAPI.setPlaceholders(player, s);
    }
}
