package net.earthcomputer.clientcommands.compat;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * Replacement for the small set of Fabric networking helpers used by the ported code.
 *
 * <p>Fabric's {@code PayloadTypeRegistry.serverboundPlay().register(...)} and
 * {@code ClientPlayNetworking.send/canSend} have no direct NeoForge equivalents; NeoForge registers
 * payloads through a {@code RegisterPayloadHandlersEvent} and sends them via
 * {@code ClientPacketDistributor}. That registration lives in
 * {@link net.earthcomputer.clientcommands.ClientCommands}; this class only keeps the buffer
 * utilities the ported code needs so it stays readable.
 */
public final class NetworkingCompat {
    private NetworkingCompat() {
    }

    /**
     * Fabric's {@code NetworkingCompat.createBuf()}. An unpooled buffer is the closest equivalent and
     * is what the c2c encoder expects.
     */
    public static FriendlyByteBuf createBuf() {
        return new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
    }

    /** Registry-aware variant, for payloads that carry registry references. */
    public static RegistryFriendlyByteBuf createRegistryBuf(RegistryAccess registryAccess) {
        return new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), registryAccess);
    }
}
