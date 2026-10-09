/*
 * Reimplementation of Fabric API's HUD element registry, kept under its original package name.
 *
 * IMPORTANT: in real Fabric API `HudElementRegistry` is an *interface*, so call sites in
 * Fabric-targeted jars compile to an `InterfaceMethodref`. Declaring it as a class would trip
 * `IncompatibleClassChangeError` at link time, exactly as happened with `FabricLoader`.
 *
 * `simplewaypoints` calls `HudElementRegistry.addLast(id, element)` to draw waypoint markers.
 * NeoForge renders the HUD as an ordered list of named layers registered through
 * `RegisterGuiLayersEvent`.
 *
 * The callback signatures line up exactly -- `GuiLayer.render` and `HudElement.extractRenderState`
 * both take `(GuiGraphicsExtractor, DeltaTracker)` -- so elements are queued here and flushed onto
 * NeoForge's layer stack when the mod-bus event fires. `addLast` maps to `registerAboveAll`, matching
 * Fabric's "last" semantics.
 */

package net.fabricmc.fabric.api.client.rendering.v1.hud;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Registers {@link HudElement}s onto NeoForge's GUI layer stack.
 */
public interface HudElementRegistry {
    List<Pending> PENDING = new ArrayList<>();
    boolean[] HOOKED = {false};

    /** Adds an element at the end of the HUD layer order. */
    static void addLast(Identifier id, HudElement element) {
        synchronized (PENDING) {
            PENDING.add(new Pending(id, element));
            if (!HOOKED[0]) {
                HOOKED[0] = true;
                // RegisterGuiLayersEvent is a mod-bus event; hook it so queued elements are flushed
                // onto the layer stack when NeoForge asks for GUI layers.
                net.neoforged.fml.ModList.get().getModContainerById("clientcommands")
                    .ifPresent(container -> container.getEventBus()
                        .addListener(HudElementRegistry::onRegisterGuiLayers));
            }
        }
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        synchronized (PENDING) {
            for (Pending pending : PENDING) {
                event.registerAboveAll(pending.id(),
                    (graphics, deltaTracker) -> pending.element().extractRenderState(graphics, deltaTracker));
            }
            PENDING.clear();
        }
    }

    /** A queued registration awaiting the mod-bus event. */
    record Pending(Identifier id, HudElement element) {
    }
}
