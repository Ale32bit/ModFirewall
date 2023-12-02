package me.alexdevs;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public final class FirewallCommand {
    public static BrigadierCommand createBrigadierCommand(ModFirewall firewall, final ProxyServer proxy) {
        var firewallNode = LiteralArgumentBuilder
            .<CommandSource>literal("firewall")
            .requires(source -> source.hasPermission("modfirewall.firewall"))
            .then(LiteralArgumentBuilder
                .<CommandSource>literal("allow")
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("modid", StringArgumentType.greedyString())
                    .suggests((ctx, builder) -> {
                        for (var mod : firewall.discoveredMods) {
                            if (!firewall.modListConfig.getAllowedMods().contains(mod)
                                && !firewall.modListConfig.getBannedMods().contains(mod)) {
                                builder.suggest(mod);
                            }
                        }

                        return builder.buildFuture();
                    })
                    .executes(context -> {
                        var modid = context.getArgument("modid", String.class);
                        firewall.modListConfig.getAllowedMods().add(modid);
                        firewall.saveFile();

                        var source = context.getSource();
                        var text = Component.text("Added " + modid + " to allowed mods!").color(NamedTextColor.GREEN).asComponent();
                        if (firewall.modListConfig.getBannedMods().contains(modid)) {
                            text = text.appendNewline()
                                .append(Component
                                    .text("Warning: This mod is in the banned list!")
                                    .color(NamedTextColor.GOLD)
                                    .decoration(TextDecoration.UNDERLINED, true));
                        }

                        source.sendMessage(firewall.textPrefix.append(text));

                        return Command.SINGLE_SUCCESS;
                    }))
            )

            .then(LiteralArgumentBuilder
                .<CommandSource>literal("forget")
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("modid", StringArgumentType.greedyString())
                    .suggests((ctx, builder) -> {
                        for (var mod : firewall.discoveredMods) {
                            if (firewall.modListConfig.getAllowedMods().contains(mod)) {
                                builder.suggest(mod);
                            }
                        }

                        return builder.buildFuture();
                    })
                    .executes(context -> {
                        var modid = context.getArgument("modid", String.class);
                        var source = context.getSource();

                        Component text;
                        var allowedMods = firewall.modListConfig.getAllowedMods();
                        if (allowedMods.contains(modid)) {
                            allowedMods.remove(modid);
                            firewall.saveFile();
                            text = Component.text("Removed " + modid + " from allowed mods!").color(NamedTextColor.GOLD);
                        } else {
                            text = Component.text(modid + " not found in allowed mods!").color(NamedTextColor.RED);
                        }

                        source.sendMessage(firewall.textPrefix.append(text));

                        return Command.SINGLE_SUCCESS;
                    }))
            )

            .then(LiteralArgumentBuilder
                .<CommandSource>literal("ban")
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("modid", StringArgumentType.greedyString())
                    .suggests((ctx, builder) -> {
                        for (var mod : firewall.discoveredMods) {
                            if (!firewall.modListConfig.getAllowedMods().contains(mod)
                                && !firewall.modListConfig.getBannedMods().contains(mod)) {
                                builder.suggest(mod);
                            }
                        }

                        return builder.buildFuture();
                    })
                    .executes(context -> {
                        var modid = context.getArgument("modid", String.class);
                        firewall.modListConfig.getBannedMods().add(modid);
                        firewall.saveFile();

                        var source = context.getSource();
                        var text = Component.text("Added " + modid + " to banned mods!").color(NamedTextColor.GOLD).asComponent();
                        if (firewall.modListConfig.getAllowedMods().contains(modid)) {
                            text = text.appendNewline()
                                .append(Component
                                    .text("Warning: This mod is in the allowed list!")
                                    .color(NamedTextColor.RED)
                                    .decoration(TextDecoration.UNDERLINED, true));
                        }
                        source.sendMessage(firewall.textPrefix.append(text));

                        return Command.SINGLE_SUCCESS;
                    }))
            )

            .then(LiteralArgumentBuilder
                .<CommandSource>literal("unban")
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("modid", StringArgumentType.greedyString())
                    .suggests((ctx, builder) -> {
                        for (var mod : firewall.discoveredMods) {
                            if (firewall.modListConfig.getBannedMods().contains(mod)) {
                                builder.suggest(mod);
                            }
                        }

                        return builder.buildFuture();
                    })
                    .executes(context -> {
                        var modid = context.getArgument("modid", String.class);
                        var source = context.getSource();

                        Component text;
                        var bannedMods = firewall.modListConfig.getBannedMods();
                        if (bannedMods.contains(modid)) {
                            bannedMods.remove(modid);
                            firewall.saveFile();
                            text = Component.text("Removed " + modid + " from banned mods!").color(NamedTextColor.GREEN);
                        } else {
                            text = Component.text(modid + " not found in banned mods!").color(NamedTextColor.RED);
                        }

                        source.sendMessage(firewall.textPrefix.append(text));

                        return Command.SINGLE_SUCCESS;
                    }))
            )
            .then(LiteralArgumentBuilder
                .<CommandSource>literal("check")
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("modid", StringArgumentType.greedyString())
                    .suggests((ctx, builder) -> {
                        for (var mod : firewall.discoveredMods) {
                            builder.suggest(mod);
                        }

                        return builder.buildFuture();
                    })
                    .executes(context -> {
                        var modid = context.getArgument("modid", String.class);
                        var source = context.getSource();

                        var isAllowed = firewall.modListConfig.getAllowedMods().contains(modid);
                        var isBanned = firewall.modListConfig.getBannedMods().contains(modid);

                        Component text;
                        if (isAllowed && isBanned) {
                            text = Component.text(modid + " is in the allowed and banned lists!").color(NamedTextColor.GOLD);
                        } else if (isAllowed) {
                            text = Component.text(modid + " is in the allowed list!").color(NamedTextColor.GREEN);
                        } else if (isBanned) {
                            text = Component.text(modid + " is in the banned list!").color(NamedTextColor.RED);
                        } else {
                            text = Component.text(modid + " not found in any list!").color(NamedTextColor.WHITE);
                        }

                        source.sendMessage(firewall.textPrefix.append(text));

                        return Command.SINGLE_SUCCESS;
                    }))
            )

            .build();

        return new BrigadierCommand(firewallNode);
    }

}
