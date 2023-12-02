package me.alexdevs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerModInfoEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyReloadEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.util.ModInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.awt.*;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

@Plugin(id = "modfirewall", name = "ModFirewall", version = "0.1.0-SNAPSHOT", url = "https://alexdevs.me", description = "Get what mods clients are using.", authors = {"AlexDevs"})
public class ModFirewall {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public ModListConfig modListConfig = null;
    public ModFirewallConfig config;

    @Inject
    public ModFirewall(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    public void saveFile() {
        var dir = dataDirectory.toFile();
        var modListFilePath = Path.of(dir.getAbsolutePath(), "modlist.json");
        if (!dir.exists()) {
            dir.mkdir();
        }

        try {
            var writer = new FileWriter(modListFilePath.toString());

            if (modListConfig == null)
                modListConfig = new ModListConfig();

            writer.write(gson.toJson(modListConfig));
            writer.close();
        } catch (IOException e) {
            logger.error("Could not write to file: " + e.getMessage());
        }
    }

    public void readFile() {
        var dir = dataDirectory.toFile();
        var modListFilePath = Path.of(dir.getAbsolutePath(), "modlist.json");
        var modListFile = modListFilePath.toFile();
        if (!dir.exists()) {
            dir.mkdir();
        }

        if (!modListFile.exists()) {
            saveFile();
        }

        try {
            var reader = new FileReader(modListFilePath.toString());

            modListConfig = gson.fromJson(reader, ModListConfig.class);

            reader.close();
        } catch (IOException e) {
            logger.error("Could not read from file: " + e.getMessage());
        }
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        var commandManager = server.getCommandManager();
        var meta = commandManager.metaBuilder("firewall");
        meta
            .plugin(this)
            .build();
        var firewallCommandRegister = FirewallCommand.createBrigadierCommand(this, server);
        commandManager.register(firewallCommandRegister);

        Path configPath = dataDirectory.resolve("ModFirewall.toml");
        config = ModFirewallConfig.read(configPath);
        readFile();
    }

    @Subscribe
    public void onProxyReload(ProxyReloadEvent event) {
        try {
            Path configPath = dataDirectory.resolve("ModFirewall.toml");
            var newConfig = ModFirewallConfig.read(configPath);
            readFile();

            config = newConfig;
        } catch (Exception e) {
            logger.error(e.toString());
        }
    }

    @Subscribe
    public void onPlayerModInfoEvent(PlayerModInfoEvent event) {
        var player = event.getPlayer();
        var modInfo = event.getModInfo();

        var stringBuilder = new StringBuilder();

        var hasUnknownMods = false;
        var mods = modInfo.getMods();
        for (var mod : mods) {
            var modName = getModId(mod);
            if (modListConfig.bannedMods.contains(modName)) {
                var text = Component
                    .text("You are not allowed to join with the mod:")
                    .color(NamedTextColor.RED)
                    .appendNewline()
                    .append(Component
                        .text(mod.getId())
                        .color(NamedTextColor.YELLOW)
                    )
                    .appendNewline()
                    .append(Component
                        .text("This incident has been reported.")
                        .color(NamedTextColor.GRAY));

                player.disconnect(text);
                logger.warn("Kicked " + player.getUsername() + " for the mod: " + modName);

                var embed = new DiscordWebhook.EmbedObject();
                embed.setAuthor(player.getUsername() + " (" + player.getUniqueId() + ")", null, null);
                embed.setTitle("Kicked for forbidden mod");
                embed.addField("Mod name", modName, true);
                embed.setColor(new Color(0xff5555));

                sendNotification(player, embed);

                return;
            }

            if (!modListConfig.allowedMods.contains(modName)) {
                stringBuilder.append(modName);
                stringBuilder.append(", ");
                hasUnknownMods = true;
            }
        }

        if (hasUnknownMods) {
            logger.info(player.getUsername() + " joined with the unknown mods: " + stringBuilder);

            var embed = new DiscordWebhook.EmbedObject();
            embed.setAuthor(player.getUsername() + " (" + player.getUniqueId() + ")", null, null);
            embed.setTitle("Joined with unknown mods");
            embed.addField("Mods", stringBuilder.toString(), true);
            embed.setColor(new Color(0xffff55));

            sendNotification(player, embed);
        }

    }

    public String getModId(ModInfo.Mod mod) {
        return mod.getId() + ":" + mod.getVersion();
    }

    public void sendNotification(Player player, DiscordWebhook.EmbedObject embed) {
        if (!config.isDiscordWebhookEnabled())
            return;

        try {
            var webhook = new DiscordWebhook(config.getDiscordWebhookUrl());

            webhook.setUsername("ModFirewall");
            webhook.addEmbed(embed);
            webhook.execute();
        } catch (IOException e) {
            logger.error(e.toString());
        }
    }

    public static class ModListConfig {
        private final ArrayList<String> allowedMods;
        private final ArrayList<String> bannedMods;

        public ModListConfig() {
            allowedMods = new ArrayList<>();
            bannedMods = new ArrayList<>();
        }

        public ArrayList<String> getAllowedMods() {
            return allowedMods;
        }

        public ArrayList<String> getBannedMods() {
            return bannedMods;
        }
    }
}
