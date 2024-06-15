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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.minebbsreward.MineBBSReward;
import top.mrxiaom.minebbsreward.func.AbstractPluginHolder;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

public class CommandMain extends AbstractPluginHolder implements CommandExecutor, TabCompleter {
    YamlConfiguration data = null;
    File dataFile;
    private final Map<UUID, Long> rewardCommandTime = new HashMap<>();
    long timeoutTop;
    long cooldownTop;
    long cooldownRewardCmd;
    String msgRequestFail, msgTopTimeout, msgTopCooldown, msgRewardCooldown, msgReload;
    public CommandMain(MineBBSReward plugin) {
        super(plugin);
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        registerCommand("minebbsreward", this);
        register();
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        timeoutTop = config.getLong("time.top-timeout") * 1000L;
        cooldownTop = config.getLong("time.top-cooldown") * 1000L;
        cooldownRewardCmd = config.getLong("time.reward-cooldown") * 1000L;
        msgRequestFail = config.getString("messages.request-fail");
        msgTopTimeout = config.getString("messages.top-timeout");
        msgTopCooldown = config.getString("messages.top-cooldown");
        msgRewardCooldown = config.getString("messages.reward-cooldown");
        msgReload = config.getString("messages.reload");
        boolean request = data == null;
        data = YamlConfiguration.loadConfiguration(dataFile);
        if (request) Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Long time = plugin.getLastTime().orElse(null);
            if (time != null) {
                data.set("local-last-time", time);
                save();
            }
        });
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
            if (args[0].equalsIgnoreCase("reward")) {
                if (!(sender instanceof Player)) {
                    return true;
                }
                Player player = (Player) sender;
                long now = System.currentTimeMillis();
                long next = rewardCommandTime.getOrDefault(player.getUniqueId(), now);
                if (now < next) {
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
                    Long lastTime = data.getLong("local-last-success-time", 0L);
                    if (time - lastTime <= cooldownTop) {
                        t(player, msgTopCooldown);
                        return;
                    }
                });
                return true;
            }
            if (args[0].equalsIgnoreCase("fetch") && sender.isOp()) {
                Long time = plugin.getLastTime().orElse(null);
                if (time == null) {
                    return t(sender, msgRequestFail);
                }
                LocalDateTime localDateTime = LocalDateTime.from(Instant.ofEpochSecond(time));
                return t(sender, "&b" + localDateTime.getYear() + "年" + localDateTime.getMonthValue() + "月" + localDateTime.getDayOfMonth() + "日 "
                        + localDateTime.getHour() + ":" + localDateTime.getMinute() + ":" + localDateTime.getSecond());
            }
            if (args[0].equalsIgnoreCase("reload") && sender.isOp()) {
                plugin.reloadConfig();
                return t(sender, msgReload);
            }
        }
        return true;
    }

    private static final List<String> emptyList = Lists.newArrayList();
    private static final List<String> listArg0 = Lists.newArrayList("reward");
    private static final List<String> listAdminArg0 = Lists.newArrayList("reward", "reload");
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return startsWith(sender.isOp() ? listAdminArg0 : listArg0, args[0]);
        }
        return emptyList;
    }

    public List<String> startsWith(List<String> list, String s) {
        String s1 = s.toLowerCase();
        List<String> stringList = new ArrayList<>(list);
        stringList.removeIf(it -> !it.toLowerCase().startsWith(s1));
        return stringList;
    }
}
