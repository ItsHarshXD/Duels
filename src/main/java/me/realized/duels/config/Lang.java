package me.realized.duels.config;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.util.Log;
import me.realized.duels.util.StringUtil;
import me.realized.duels.util.config.AbstractConfiguration;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class Lang extends AbstractConfiguration<DuelsPlugin> {

    private final Map<String, String> messages = new HashMap<>();

    public Lang(final DuelsPlugin plugin) {
        super(plugin, "lang");
    }

    @Override
    protected void loadValues(FileConfiguration configuration) throws IOException {
        // TODO: 26/04/2018 Temp disabled to allow /reload, enable later
//        if (configuration.getInt("config-version") < getLatestVersion()) {
//            configuration = convert(null);
//        }

        final Map<String, String> strings = new HashMap<>();

        for (String key : configuration.getKeys(true)) {
            if (key.equals("config-version")) {
                continue;
            }

            // Fixes a weird occurrence with FileConfiguration#getKeys that an extra separator char is prepended when called after FileConfiguration#set
            if (key.startsWith(".")) {
                key = key.substring(1);
            }

            final Object value = configuration.get(key);

            if (value == null || value instanceof MemorySection) {
                continue;
            }

            final String message = value instanceof List ? StringUtil.fromList((List<?>) value) : value.toString();

            if (key.startsWith("STRINGS")) {
                final String[] args = key.split(Pattern.quote("."));
                strings.put(args[args.length - 1], message);
            } else {
                messages.put(key, message);
            }
        }

        messages.replaceAll((key, value) -> {
            for (final Map.Entry<String, String> entry : strings.entrySet()) {
                final String placeholder = "{" + entry.getKey() + "}";

                if (StringUtils.containsIgnoreCase(value, placeholder)) {
                    value = value.replaceAll("(?i)" + Pattern.quote(placeholder), entry.getValue());
                }
            }

            return value;
        });
    }

    @Override
    public void handleUnload() {
        messages.clear();
    }

    public void sendMessage(final CommandSender receiver, final String key, final Object... replacers) {
        final Optional<String> message = getMessage(key, replacers);
        message.ifPresent(receiver::sendMessage);
    }

    public void sendMessage(final Collection<UUID> receivers, final String key, final Object... replacers) {
        final Optional<String> message = getMessage(key, replacers);
        message.ifPresent(s -> receivers.forEach(uuid -> {
            final Player player = Bukkit.getPlayer(uuid);

            if (player != null) {
                player.sendMessage(s);
            }
        }));
    }

    private Optional<String> getMessage(final String key, final Object... replacers) {
        final String message = messages.get(key);

        if (message == null) {
            Log.error(this, "Failed to send message: provided key '" + key + "' has no assigned value");
            return Optional.empty();
        }

        return Optional.of(StringUtil.color(replace(message, replacers)));
    }

    private String replace(String message, final Object... replacers) {
        for (int i = 0; i < replacers.length; i += 2) {
            if (i + 1 >= replacers.length) {
                break;
            }

            message = message.replace("%" + replacers[i].toString() + "%", String.valueOf(replacers[i + 1]));
        }

        return message;
    }
}