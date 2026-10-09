package net.earthcomputer.clientcommands.compat;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

/**
 * Holds the client command dispatcher built by the port.
 *
 * <p>Fabric API exposes its client dispatcher to mods through
 * {@code ClientCommandManager#getActiveDispatcher()}, and several command classes in this mod rely on
 * that (for example to re-parse a command so redirects and flags resolve). NeoForge registers
 * commands onto the vanilla dispatcher instead, so the port keeps its own client-source dispatcher
 * here and exposes it under the same shape.
 */
public final class ActiveDispatcher {
    private static CommandDispatcher<FabricClientCommandSource> dispatcher;

    private ActiveDispatcher() {
    }

    /** Installs the dispatcher built during client command registration. */
    public static void set(CommandDispatcher<FabricClientCommandSource> value) {
        dispatcher = value;
    }

    /** The active client command dispatcher, or {@code null} before registration has run. */
    public static CommandDispatcher<FabricClientCommandSource> get() {
        return dispatcher;
    }
}
