package me.alexdevs;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.gson.annotations.Expose;

import java.nio.file.Path;

public class ModFirewallConfig {
    @Expose
    private boolean discordWebhookEnable;
    @Expose
    private String discordWebhookUrl;

    public ModFirewallConfig(boolean discordWebhookEnable, String discordWebhookUrl) {
        this.discordWebhookEnable = discordWebhookEnable;
        this.discordWebhookUrl = discordWebhookUrl;
    }

    public static ModFirewallConfig read(Path path) {
        var defaultConfigLocation = ModFirewallConfig.class
            .getClassLoader()
            .getResource("default-modfirewall.toml");

        if (defaultConfigLocation == null)
            throw new RuntimeException("Default configuration file not found!");

        var config = CommentedFileConfig.builder(path)
            .defaultData(defaultConfigLocation)
            .autosave()
            .preserveInsertionOrder()
            .sync()
            .build();
        config.load();

        var discordWebhookEnable = config.getOrElse("discord-webhook-enable", false);
        var discordWebhookUrl = config.getOrElse("discord-webhook-url", "");
        return new ModFirewallConfig(discordWebhookEnable, discordWebhookUrl);
    }

    public boolean isDiscordWebhookEnabled() {
        return discordWebhookEnable;
    }

    public String getDiscordWebhookUrl() {
        return discordWebhookUrl;
    }
}
