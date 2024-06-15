package top.mrxiaom.minebbsreward;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import top.mrxiaom.minebbsreward.func.AbstractPluginHolder;
import top.mrxiaom.minebbsreward.utils.Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static top.mrxiaom.minebbsreward.func.AbstractPluginHolder.reloadAllConfig;
import static top.mrxiaom.minebbsreward.utils.Util.stackTraceToString;

public class MineBBSReward extends JavaPlugin implements Listener {
    private static MineBBSReward instance;

    public static MineBBSReward getInstance() {
        return instance;
    }
    private static final String defaultThread = "https://www.minebbs.com/threads/XXX";
    private String threadUrl;
    private Pattern matcherRegex;
    private int matcherFindTimes;
    private int matcherGroup;
    private final Map<String, String> headers = new HashMap<>();
    private String sHour, sHours, sMinute, sMinutes, sSecond, sSeconds;
    private DateTimeFormatter timePattern;
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
        if (threadUrl.equals(defaultThread) || matcherFindTimes <= 0) return Optional.empty();
        try {
            URL url = new URL(threadUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
            int code = conn.getResponseCode();
            if (code == 200) {
                try (InputStream in = conn.getInputStream()) {
                    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                        byte[] buffer = new byte[1024 * 10];
                        int length;
                        while ((length = in.read(buffer)) != -1) {
                            out.write(buffer, 0, length);
                        }
                        String s = new String(out.toByteArray(), StandardCharsets.UTF_8);
                        Matcher matcher = matcherRegex.matcher(s);
                        for (int i = 0; i < matcherFindTimes; i++) {
                            if (!matcher.find()) {
                                throw new IllegalStateException("Only found " + i + " times. (" + matcherFindTimes + " expected)");
                            }
                        }
                        String group = matcher.group(matcherGroup);
                        return Util.parseLong(group);
                    }
                }
            }
            throw new IOException("code = " + code + "; " + conn.getResponseMessage());
        } catch (Throwable t) {
            getLogger().warning(stackTraceToString(t));
            return Optional.empty();
        }
    }

    public String toString(long timeDiff) {
        long second = timeDiff % 60;
        long minute = (timeDiff / 60) % 60;
        long hour = timeDiff / 3600;
        StringBuilder sb = new StringBuilder();
        if (hour > 0) sb.append(hour).append(hour > 1 ? sHours : sHour);
        if (minute > 0 || hour > 0) sb.append(minute).append(minute > 1 ? sMinutes : sMinute);
        if (second > 0 || minute > 0 || hour > 0) sb.append(second).append(second > 1 ? sSeconds : sSecond);
        return sb.toString();
    }

    public String toString(LocalDateTime time) {
        return time.format(timePattern);
    }

    @Override
    public void reloadConfig() {
        this.saveDefaultConfig();
        super.reloadConfig();

        FileConfiguration config = getConfig();

        threadUrl = config.getString("thread-url", defaultThread);
        matcherRegex = Pattern.compile(config.getString("matcher.regex", ""));
        matcherFindTimes = config.getInt("matcher.find-times");
        matcherGroup = config.getInt("matcher.group");
        headers.clear();
        ConfigurationSection section = config.getConfigurationSection("headers");
        if (section != null) for (String key : section.getKeys(false)) {
            headers.put(key, section.getString(key));
        }

        sHour = config.getString("messages.time.hour");
        sHours = config.getString("messages.time.hours");
        sMinute = config.getString("messages.time.minute");
        sMinutes = config.getString("messages.time.minutes");
        sSecond = config.getString("messages.time.second");
        sSeconds = config.getString("messages.time.seconds");

        String timePatternString = config.getString("messages.placeholder.time-pattern", "yyyy年MM月dd日 HH:mm:ss");
        try {
            timePattern = DateTimeFormatter.ofPattern(timePatternString);
        } catch (IllegalArgumentException e) {
            timePattern = DateTimeFormatter.ISO_DATE_TIME;
        }

        reloadAllConfig(config);
    }
}
