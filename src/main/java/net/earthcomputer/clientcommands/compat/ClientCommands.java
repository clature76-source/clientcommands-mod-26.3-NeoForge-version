package net.earthcomputer.clientcommands.compat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

/**
 * Drop-in replacement for Fabric API's
 * {@code net.fabricmc.fabric.api.client.command.v2.ClientCommands} static helpers.
 *
 * <p>These build a Brigadier tree whose source type is {@link FabricClientCommandSource}, matching
 * both the ported command bodies and the {@code clientarguments} library, which is compiled against
 * that same type.
 */
public final class ClientCommands {
    private ClientCommands() {
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    public static <T> RequiredArgumentBuilder<FabricClientCommandSource, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    /**
     * The active client command dispatcher.
     *
     * <p>Mirrors {@code ClientCommandManager#getActiveDispatcher()} from Fabric API, which several
     * command classes use to re-parse a command so that redirects and flags resolve correctly.
     *
     * @return the dispatch tree built at registration time, or {@code null} before that
     */
    public static CommandDispatcher<FabricClientCommandSource> getActiveDispatcher() {
        return ActiveDispatcher.get();
    }
}
