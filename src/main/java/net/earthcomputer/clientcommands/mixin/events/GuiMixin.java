package net.earthcomputer.clientcommands.mixin.events;

import net.earthcomputer.clientcommands.event.MoreScreenEvents;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    public void onOpenScreen(@Nullable Screen screen, CallbackInfo ci) {
        if (!MoreScreenEvents.BEFORE_ADD.invoker().beforeScreenAdd(screen)) {
            ci.cancel();
        }
    }
}
