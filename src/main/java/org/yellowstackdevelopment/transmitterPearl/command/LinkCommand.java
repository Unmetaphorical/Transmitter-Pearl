package org.yellowstackdevelopment.transmitterPearl.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.yellowstackdevelopment.transmitterPearl.item.TransmitterPearlItem;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class LinkCommand {
    private static final Map<UUID, Set<UUID>> TRUSTED_PLAYERS = new HashMap<>();

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("linkpearl")
                .then(CommandManager.literal("trust")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                        .executes(context -> {
                                            ServerPlayerEntity owner = context.getSource().getPlayerOrThrow();
                                            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                                            boolean enabled = BoolArgumentType.getBool(context, "enabled");
                                            
                                            setTrust(owner, target, enabled);
                                            
                                            String status = enabled ? "§atrusted" : "§cuntrusted";
                                            context.getSource().sendFeedback(() -> Text.literal("§7Player " + target.getName().getString() + " is now " + status), false);
                                            return 1;
                                        }))))
                .then(CommandManager.literal("clear")
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                            TransmitterPearlItem.clearAllLinksToPlayer(player);
                            context.getSource().sendFeedback(() -> Text.literal("§7All active links pointing to you have been cleared."), false);
                            return 1;
                        }))
        );
    }

    private static void setTrust(ServerPlayerEntity owner, ServerPlayerEntity target, boolean enabled) {
        Set<UUID> trusted = TRUSTED_PLAYERS.computeIfAbsent(owner.getUuid(), k -> new HashSet<>());
        if (enabled) {
            trusted.add(target.getUuid());
        } else {
            trusted.remove(target.getUuid());
        }
    }

    public static boolean isTrusted(ServerPlayerEntity owner, ServerPlayerEntity requester) {
        if (owner.getUuid().equals(requester.getUuid())) return true;
        Set<UUID> trusted = TRUSTED_PLAYERS.get(owner.getUuid());
        return trusted != null && trusted.contains(requester.getUuid());
    }
}
