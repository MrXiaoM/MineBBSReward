package top.mrxiaom.minebbsreward;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import top.mrxiaom.minebbsreward.func.AbstractPluginHolder;
import top.mrxiaom.minebbsreward.utils.Util;

import java.util.Optional;

import static top.mrxiaom.minebbsreward.func.AbstractPluginHolder.reloadAllConfig;
import static top.mrxiaom.minebbsreward.utils.Util.stackTraceToString;

public class MineBBSReward extends JavaPlugin implements Listener {
    private static MineBBSReward instance;

    public static MineBBSReward getInstance() {
        return instance;
    }
    private static final String defaultThread = "https://www.minebbs.com/threads/XXX";
    private String threadUrl;
    private String matcherRegex;
    private int matcherGroup;
    @Override
    public void onEnable() {
        Util.init(instance = this);

        loadFunctions();
        reloadConfig();

        getLogger().info("MineBBSReward 加载完毕");
    }

    public void loadFunctions() {
        AbstractPluginHolder.loadModules(this);
    }

    @Override
    public void onDisable() {
        AbstractPluginHolder.callDisable();
        HandlerList.unregisterAll((Plugin) this);
        Bukkit.getScheduler().cancelTasks(this);
    }

    public Optional<Long> getLastTime() {
        if (threadUrl.equals(defaultThread)) return Optional.empty();
        try {
            // TODO: 获取上次顶帖时间
        } catch (Throwable t) {
            getLogger().warning(stackTraceToString(t));
            return Optional.empty();
        }
    }

    @Override
    public void reloadConfig() {
        this.saveDefaultConfig();
        super.reloadConfig();

        FileConfiguration config = getConfig();

        threadUrl = config.getString("thread-url", defaultThread);
        matcherRegex = config.getString("matcher.regex");
        matcherGroup = config.getInt("matcher.group");

        reloadAllConfig(config);
    }
}
