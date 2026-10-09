package net.earthcomputer.clientcommands.mixin.screen;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.platform.Window;
import net.earthcomputer.clientcommands.interfaces.ISafeZoneScreen;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Enlarges the GUI scale so screens can reserve space for clientcommands' safe zone.
 *
 * <p>On Fabric the client is fully constructed before {@code Window#calculateScale} runs, so the
 * active screen can be read directly. On NeoForge, {@code EarlyWindowHandoff} calls
 * {@code Minecraft#resizeGui} -> {@code calculateScale} from <em>inside</em> {@code Minecraft}'s
 * constructor, at a point where {@code Minecraft.gui} has not been assigned yet. Dereferencing it
 * there crashes startup with
 * {@code NullPointerException: Cannot invoke "Gui.screen()" because "mc.gui" is null}.
 *
 * <p>When no GUI exists yet there is no screen to reserve space for, so returning the original value
 * is both safe and correct.
 */
@Mixin(Window.class)
public class WindowMixin {
    @ModifyExpressionValue(method = "calculateScale", at = @At(value = "CONSTANT", args = "intValue=" + Window.BASE_WIDTH))
    private int getSafeZoneWidth(int original) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.gui != null && mc.gui.screen() instanceof ISafeZoneScreen safeZone) {
            original = Math.max(original, safeZone.getSafeZoneWidth());
        }
        return original;
    }

    @ModifyExpressionValue(method = "calculateScale", at = @At(value = "CONSTANT", args = "intValue=" + Window.BASE_HEIGHT))
    private int getSafeZoneHeight(int original) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.gui != null && mc.gui.screen() instanceof ISafeZoneScreen safeZone) {
            original = Math.max(original, safeZone.getSafeZoneHeight());
        }
        return original;
    }
}
