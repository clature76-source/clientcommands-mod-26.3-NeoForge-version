package net.earthcomputer.clientcommands.mixin.rngevents;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import net.earthcomputer.clientcommands.features.PlayerRandCracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Models the player RNG call made when shears are used on a shearable entity.
 *
 * <p>26.3 reworked entity shearing. Previously each entity's {@code mobInteract} contained the shear
 * branch inline, guarded by {@code instanceof ServerLevel} -- which is what this port originally
 * hooked, across {@code MushroomCow}, {@code Sheep} and {@code SnowGolem} in one mixin. In 26.3:
 *
 * <ul>
 *   <li>{@code Sheep} and {@code SnowGolem} lost that branch entirely; their {@code mobInteract} now
 *       delegates and the shear body moved to
 *       {@code Shearable#shear(ServerLevel, SoundSource, ItemStack)}.</li>
 *   <li>All shearable entities are now reached through a single Neoforge-aware entry point,
 *       {@code ShearsItem#interactLivingEntity}.</li>
 * </ul>
 *
 * <p>Hooking the old {@code @Expression} against all three classes therefore failed with
 * "expected 1 invocation(s) but 0 succeeded. Scanned 0 target(s)". This mixin instead targets the
 * unified entry point, which is both closer to the original intent (it runs exactly when a player
 * shears a living entity, on either side) and avoids duplicating the hook per entity.
 *
 * <p>The {@code instanceof ServerLevel} guard in {@code interactLivingEntity} is the point at which
 * the server actually performs the shear -- and therefore where the player's RNG is consumed.
 */
@Mixin(ShearsItem.class)
public class MushroomCowSheepAndSnowGolemMixin {

    @Definition(id = "ServerLevel", type = ServerLevel.class)
    @Expression("? instanceof ServerLevel")
    @Inject(method = "interactLivingEntity", at = @At("MIXINEXTRAS:EXPRESSION"))
    public void onInteract(ItemStack shears, Player player, LivingEntity target, InteractionHand hand,
                           CallbackInfoReturnable<net.minecraft.world.InteractionResult> ci) {
        PlayerRandCracker.onItemDamage(1, player, shears);
    }
}
