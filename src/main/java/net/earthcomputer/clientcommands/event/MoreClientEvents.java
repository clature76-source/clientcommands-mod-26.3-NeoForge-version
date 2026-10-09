package net.earthcomputer.clientcommands.event;

import net.earthcomputer.clientcommands.compat.fabric.Event;
import net.earthcomputer.clientcommands.compat.fabric.EventFactory;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;

public final class MoreClientEvents {
    public static final Event<TimeSync> TIME_SYNC_ON_NETWORK_THREAD = EventFactory.createArrayBacked(TimeSync.class, listeners -> packet -> {
        for (TimeSync listener : listeners) {
            listener.onTimeSync(packet);
        }
    });
    public static final Event<TimeSync> TIME_SYNC = EventFactory.createArrayBacked(TimeSync.class, listeners -> packet -> {
        for (TimeSync listener : listeners) {
            listener.onTimeSync(packet);
        }
    });

    @FunctionalInterface
    public interface TimeSync {
        void onTimeSync(ClientboundSetTimePacket packet);
    }
}
