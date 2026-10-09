/*
 * Reimplementation of Fabric API's render-state data key, kept under its original package name.
 *
 * Fabric mixes a generic `getData`/`setData` bag into vanilla render state objects, keyed by these
 * opaque identities. NeoForge has the same idea under a different name: `BaseRenderState` carries a
 * `ContextKey`-keyed bag exposed as `getRenderData`/`setRenderData`.
 *
 * This type therefore wraps a NeoForge `ContextKey`, so that
 * `LevelRenderStateMixin`'s Fabric-shaped `getData`/`setData` bridge can hand the same identity to
 * NeoForge's native bag. That keeps third-party libraries (`simplewaypoints`) working unmodified.
 */

package net.fabricmc.fabric.api.client.rendering.v1;

import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * An opaque, typed key identifying one piece of per-frame render data.
 */
public final class RenderStateDataKey<T> {
    private static final AtomicInteger NEXT_ID = new AtomicInteger();

    /** Backing NeoForge key; identity is what matters, the name is only for debugging. */
    private final ContextKey<T> contextKey;

    private RenderStateDataKey(Supplier<String> debugName) {
        this.contextKey = new ContextKey<>(
            Identifier.fromNamespaceAndPath("clientcommands", "render_state_data/" + NEXT_ID.getAndIncrement()));
        this.debugName = debugName;
    }

    private final Supplier<String> debugName;

    public static <T> RenderStateDataKey<T> create(Supplier<String> debugName) {
        return new RenderStateDataKey<>(debugName);
    }

    public static <T> RenderStateDataKey<T> create() {
        return new RenderStateDataKey<>(() -> "unnamed");
    }

    /** The NeoForge-side key this Fabric key maps onto. */
    public ContextKey<T> contextKey() {
        return contextKey;
    }

    @Override
    public String toString() {
        return "RenderStateDataKey(" + debugName.get() + ")";
    }
}
