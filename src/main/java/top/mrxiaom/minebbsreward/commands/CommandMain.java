package top.mrxiaom.minebbsreward.commands;

import com.google.common.collect.Lists;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.minebbsreward.MineBBSReward;
import top.mrxiaom.minebbsreward.func.AbstractPluginHolder;
import top.mrxiaom.minebbsreward.utils.Util;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

import static top.mrxiaom.minebbsreward.utils.Util.currentTimestamp;

public class CommandMain extends AbstractPluginHolder implements CommandExecutor, TabCompleter {
    YamlConfiguration data = null;
    File dataFile;
    private final Map<UUID, Long> rewardCommandTime = new HashMap<>();
    long timeoutTop;
    long cooldownTop;
    long cooldownRewardCmd;
    boolean checkOnlySuccess;
    String msgRequestFail, msgTopTimeout, msgTopCooldown, msgRewardCooldown, msgFetch, msgReload;
    String phNotInCooldown;
    List<String> rewardCommands;
    long localLastTime, localLastSuccessTime;
    public CommandMain(MineBBSReward plugin) {
        super(plugin);
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        registerCommand("minebbsreward", this);
        register();
    }

    public long getLocalLastTime() {
        return localLastTime;
    }

    public long getLocalLastSuccessTime() {
        return localLastSuccessTime;
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        timeoutTop = config.getLong("time.top-timeout") * 1000L;
        cooldownTop = config.getLong("time.top-cooldown") * 1000L;
        cooldownRewardCmd = config.getLong("time.reward-cooldown") * 1000L;
        checkOnlySuccess = config.getBoolean("time.check-only-success");
        rewardCommands = config.getStringList("reward-commands");
        phNotInCooldown = config.getString("messages.placeholder.not-in-cooldown");
        msgRequestFail = config.getString("messages.request-fail");
        msgTopTimeout = config.getString("messages.top-timeout");
        msgTopCooldown = config.getString("messages.top-cooldown");
        msgRewardCooldown = config.getString("messages.reward-cooldown");
        msgFetch = config.getString("messages.fetch");
        msgReload = config.getString("messages.reload");
        boolean request = data == null;
        data = YamlConfiguration.loadConfiguration(dataFile);
        localLastTime = data.getLong("local-last-time", 0L);
        localLastSuccessTime = data.getLong("local-last-success-time", 0L);
        if (request) Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Long time = plugin.getLastTime().orElse(null);
            if (time != null) {
                data.set("local-last-time", localLastTime = time);
                save();
            }
        });
    }

    public String getPlayerCooldown() {
        return plugin.toString(cooldownRewardCmd);
    }

    public String getTopCooldown() {
        return plugin.toString(cooldownTop);
    }

    public String getPlayerCooldownRemain(UUID uuid) {
        long now = currentTimestamp();
        long next = rewardCommandTime.getOrDefault(uuid, now);
        if (now < next) {
            return plugin.toString(next - now);
        }
        return phNotInCooldown;
    }

    public String getTopCooldownRemain() {
        long time = currentTimestamp();
        long lastTime = checkOnlySuccess
                ? localLastSuccessTime
                : localLastTime;
        if (time - lastTime <= cooldownTop) {
            return plugin.toString(time - lastTime);
        }
        return phNotInCooldown;
    }

    private void save() {
        try {
            data.save(dataFile);
        } catch (Throwable t) {
            warn(t);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("reward") && sender.hasPermission("minebbsreward.command.reward")) {
                if (!(sender instanceof Player)) {
                    return true;
                }
                Player player = (Player) sender;
                long now = currentTimestamp();
                long next = rewardCommandTime.getOrDefault(player.getUniqueId(), now);
                if (now < next) {
                    t(player, msgRewardCooldown.replace("%remain%", plugin.toString(next - now)));
                    return true;
                }
                rewardCommandTime.put(player.getUniqueId(), now + cooldownRewardCmd);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    Long time = plugin.getLastTime().orElse(null);
                    if (time == null) {
                        t(player, msgRequestFail);
                        rewardCommandTime.remove(player.getUniqueId());
                        return;
                    }
                    long lastTime = checkOnlySuccess ? localLastSuccessTime : localLastTime;
                    data.set("local-last-time", localLastTime = time);
                    if (now - time > timeoutTop) {
                        t(player, msgTopTimeout.replace("%time%", plugin.toString(timeoutTop)));
                        save();
                        return;
                    }
                    if (time - lastTime <= cooldownTop) {
                        t(player, msgTopCooldown.replace("%time%", plugin.toString(cooldownTop)));
                        save();
                        return;
                    }
                    data.set("local-last-success-time", localLastSuccessTime = time);
                    save();
                    Util.runCommands(player, rewardCommands);
                });
                return true;
            }
            if (args[0].equalsIgnoreCase("fetch") && sender.hasPermission("minebbsreward.command.fetch")) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    Long time = plugin.getLastTime().orElse(null);
                    if (time == null) {
                        t(sender, msgRequestFail);
                        return;
                    }
                    LocalDateTime localDateTime = Util.fromTimestamp(time);
                    t(sender, msgFetch.replace("%time%", plugin.toString(localDateTime)));
                });
                return true;
            }
            if (args[0].equalsIgnoreCase("reload") && sender.hasPermission("minebbsreward.command.reload")) {
                plugin.reloadConfig();
                return t(sender, msgReload);
            }
        }
        return true;
    }

    private static final List<String> emptyList = Lists.newArrayList();
    private static final List<String> listArg0 = Lists.newArrayList("reward", "fetch", "reload");
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return startsWith(sender, listArg0, args[0]);
        }
        return emptyList;
    }

    public List<String> startsWith(Permissible permissible, List<String> list, String s) {
        String s1 = s.toLowerCase();
        List<String> stringList = new ArrayList<>(list);
        stringList.removeIf(it -> !it.toLowerCase().startsWith(s1));
        stringList.removeIf(it -> !permissible.hasPermission("minebbsreward.command." + it));
        return stringList;
    }

    public static CommandMain inst() {
        return get(CommandMain.class).orElseThrow(IllegalStateException::new);
    }
}
