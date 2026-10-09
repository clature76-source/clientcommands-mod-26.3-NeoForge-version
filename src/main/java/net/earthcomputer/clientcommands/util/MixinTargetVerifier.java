package net.earthcomputer.clientcommands.util;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;

/**
 * Verification-only helper that force-loads mixin target classes which the client does not otherwise
 * touch during a headless startup.
 *
 * <p>Mixin applies a mixin only when its target class is loaded. Most of clientcommands' client-side
 * targets are screens and menus that appear only once the player is in a world, so a startup-only run
 * cannot confirm they inject. Setting {@code -Dclientcommands.verifyMixinTargets=true} loads those
 * classes during client setup, which makes the Mixin transformer process them and emit its usual
 * "Mixing ... into ..." line -- or fail loudly.
 *
 * <p>This lives outside the {@code …clientcommands.mixin} package on purpose: Mixin treats that
 * package as its own and rejects direct references to classes inside it from the rest of the mod.
 *
 * <p>Inert unless the property is set, so it has no effect on normal gameplay.
 */
@EventBusSubscriber(modid = "clientcommands", value = Dist.CLIENT)
public final class MixinTargetVerifier {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Targets that a startup-only run leaves unloaded. Loaded purely to trigger mixin application.
     */
    private static final String[] TARGETS = {
        "com.mojang.brigadier.tree.ArgumentCommandNode",
        "net.minecraft.client.multiplayer.ClientSuggestionProvider",
        "net.minecraft.client.gui.components.EditBox",
        "net.minecraft.util.StringUtil",
        "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen$CustomCreativeSlot",
        "net.minecraft.client.multiplayer.MultiPlayerGameMode",
        "net.minecraft.network.protocol.game.ServerboundContainerClosePacket",
        "net.minecraft.client.gui.components.CommandSuggestions",
        "net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl",
        "net.minecraft.client.ClientClockManager$ClientClockInstance",
        "net.minecraft.client.gui.components.ChatComponent$1",
    };

    private MixinTargetVerifier() {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        if (!Boolean.getBoolean("clientcommands.verifyMixinTargets")) {
            return;
        }
        event.enqueueWork(() -> {
            for (String name : TARGETS) {
                try {
                    Class.forName(name, false, MixinTargetVerifier.class.getClassLoader());
                    LOGGER.info("[verify] loaded mixin target {}", name);
                } catch (Throwable t) {
                    LOGGER.warn("[verify] could not load mixin target {}: {}", name, t.toString());
                }
            }
        });
    }
}
