package net.earthcomputer.clientcommands.compat.fabric;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Installs the global NeoForge listeners that fan out to the per-screen callbacks recorded in
 * {@link ScreenEvents}. Installation is deferred until the first registration so the mod does not
 * touch the event bus unless the relog screen hooks are actually used.
 *
 * <p>NeoForge has no per-screen tick event, so ticking is driven from
 * {@link ClientTickEvent.Pre} and dispatched to whichever screen is currently open.
 */
final class ScreenEventDispatcher {
    private static boolean installed;

    private ScreenEventDispatcher() {
    }

    static void install() {
        if (installed) {
            return;
        }
        installed = true;

        NeoForge.EVENT_BUS.addListener(ScreenEventDispatcher::onRender);
        NeoForge.EVENT_BUS.addListener(ScreenEventDispatcher::onTick);
    }

    private static void onRender(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        var callbacks = ScreenEvents.extractCallbacks(screen);
        if (callbacks.isEmpty()) {
            return;
        }
        for (ScreenEvents.AfterExtract callback : callbacks) {
            callback.afterExtract(screen, event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
        }
    }

    private static void onTick(ClientTickEvent.Pre event) {
        Screen screen = Minecraft.getInstance().gui.screen();
        if (screen == null) {
            return;
        }
        var callbacks = ScreenEvents.tickCallbacks(screen);
        for (ScreenEvents.AfterTick callback : callbacks) {
            callback.afterTick(screen);
        }
    }
}
