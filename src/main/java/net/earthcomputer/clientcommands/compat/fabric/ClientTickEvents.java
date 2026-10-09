package net.earthcomputer.clientcommands.compat.fabric;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Counterpart to Fabric API's {@code ClientTickEvents}. Registration is delegated to NeoForge's
 * client tick events on the game event bus.
 *
 * <p>Fabric invokes listeners with the {@link Minecraft} instance, so the ported callbacks take that
 * parameter; NeoForge's event does not carry it, so it is read from the singleton here.
 */
public final class ClientTickEvents {
    private ClientTickEvents() {
    }

    /** Mirrors {@code ClientTickEvents.START_CLIENT_TICK}. */
    public static final StartClientTick START_CLIENT_TICK = new StartClientTick();

    /** Mirrors {@code ClientTickEvents.END_CLIENT_TICK}. */
    public static final StartClientTick END_CLIENT_TICK = new StartClientTick(true);

    public static final class StartClientTick {
        private final boolean end;

        private StartClientTick() {
            this(false);
        }

        private StartClientTick(boolean end) {
            this.end = end;
        }

        public void register(TickListener listener) {
            if (end) {
                NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) ->
                    listener.onStartTick(Minecraft.getInstance()));
            } else {
                NeoForge.EVENT_BUS.addListener((ClientTickEvent.Pre event) ->
                    listener.onStartTick(Minecraft.getInstance()));
            }
        }
    }

    @FunctionalInterface
    public interface TickListener {
        void onStartTick(Minecraft client);
    }
}
