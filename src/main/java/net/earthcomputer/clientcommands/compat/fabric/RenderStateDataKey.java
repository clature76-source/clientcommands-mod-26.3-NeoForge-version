package net.earthcomputer.clientcommands.compat.fabric;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Counterpart to Fabric API's
 * {@code net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey}.
 *
 * <p>Fabric mixes a generic {@code getData}/{@code setData} bag into vanilla render state objects;
 * NeoForge's {@link net.minecraft.client.renderer.state.level.LevelRenderState} has no such bag, so
 * the port keeps that data in {@link #store()} keyed by these identities.
 */
public final class RenderStateDataKey<T> {
    private final Supplier<String> name;

    private RenderStateDataKey(Supplier<String> debugName) {
        this.name = debugName;
    }

    public static <T> RenderStateDataKey<T> create(Supplier<String> debugName) {
        return new RenderStateDataKey<>(debugName);
    }

    public static <T> RenderStateDataKey<T> create() {
        return new RenderStateDataKey<>(() -> "unnamed");
    }

    @Override
    public String toString() {
        return "RenderStateDataKey(" + name.get() + ")";
    }

    private static final Store STORE = new Store();

    /**
     * Per-frame storage standing in for Fabric's render-state data bag. Keyed by the
     * {@link RenderStateDataKey} identity, holding one value per key, cleared each frame.
     */
    public static Store store() {
        return STORE;
    }

    public static final class Store {
        private final Map<RenderStateDataKey<?>, Object> data = new HashMap<>();

        private Store() {
        }

        @SuppressWarnings("unchecked")
        public <T> T get(RenderStateDataKey<T> key) {
            return (T) data.get(key);
        }

        public <T> void set(RenderStateDataKey<T> key, T value) {
            data.put(key, value);
        }

        /** Called at the start of each extraction pass so stale frame data cannot leak through. */
        public void clear() {
            data.clear();
        }
    }
}
