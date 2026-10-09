package net.earthcomputer.clientcommands.mixin.compat;

import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.neoforged.neoforge.client.renderstate.BaseRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Bridges Fabric API's render-state data bag onto NeoForge's native one.
 *
 * <p>Fabric mixes {@code getData}/{@code setData} directly into vanilla render state objects, keyed
 * by a {@code RenderStateDataKey}. NeoForge instead has {@code BaseRenderState.getRenderData}/
 * {@code setRenderData}, keyed by a {@code ContextKey}. Third-party libraries written against
 * Fabric (notably {@code simplewaypoints}) call the Fabric-shaped methods on
 * {@link LevelRenderState}, so the port supplies them here and forwards to NeoForge's bag.
 *
 * <p>This is purely additive: no vanilla behaviour is replaced, and NeoForge's own accessors keep
 * working unchanged.
 */
@Mixin(LevelRenderState.class)
public abstract class LevelRenderStateMixin {

    /**
     * Fabric's {@code getData}; reads NeoForge's ContextKey-keyed bag.
     *
     * @param key the Fabric-shaped key
     * @return the stored value, or null when absent
     */
    @Unique
    public <T> T getData(RenderStateDataKey<T> key) {
        return ((BaseRenderState) (Object) this).getRenderData(key.contextKey());
    }

    /**
     * Fabric's {@code setData}; writes NeoForge's ContextKey-keyed bag.
     *
     * @param key the Fabric-shaped key
     * @param value the value to store
     */
    @Unique
    public <T> void setData(RenderStateDataKey<T> key, T value) {
        ((BaseRenderState) (Object) this).setRenderData(key.contextKey(), value);
    }
}
