package top.mrxiaom.minebbsreward.utils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.minebbsreward.MineBBSReward;
import top.mrxiaom.minebbsreward.commands.CommandMain;

import java.util.UUID;

public class Placeholder extends PlaceholderExpansion {
    private final MineBBSReward plugin;
    public Placeholder(MineBBSReward plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return plugin.getName().toLowerCase();
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equalsIgnoreCase("cooldown_reward_remain")) {
            UUID uuid = player == null ? null : player.getUniqueId();
            return uuid == null ? "" : CommandMain.inst().getPlayerCooldownRemain(uuid);
        }
        if (params.equalsIgnoreCase("cooldown_reward")) {
            return CommandMain.inst().getPlayerCooldown();
        }
        if (params.equalsIgnoreCase("cooldown_top_remain")) {
            return CommandMain.inst().getTopCooldownRemain();
        }
        if (params.equalsIgnoreCase("cooldown_top")) {
            return CommandMain.inst().getTopCooldown();
        }
        if (params.equalsIgnoreCase("last_time")) {
            long time = CommandMain.inst().getLocalLastTime();
            return time == 0 ? "" : plugin.toString(Util.fromTimestamp(time));
        }
        if (params.equalsIgnoreCase("last_success_time")) {
            long time = CommandMain.inst().getLocalLastSuccessTime();
            return time == 0 ? "" : plugin.toString(Util.fromTimestamp(time));
        }
        return super.onRequest(player, params);
    }
}
