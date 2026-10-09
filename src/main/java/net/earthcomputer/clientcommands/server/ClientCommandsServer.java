package net.earthcomputer.clientcommands.server;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.netty.buffer.ByteBuf;
import net.earthcomputer.clientcommands.util.BuildInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permissions;

import java.net.URI;

/**
 * Server-side opt-in support, corresponding to the Fabric build's {@code ModInitializer}.
 *
 * <p>Payload registration moved to {@link net.earthcomputer.clientcommands.ClientCommands} because
 * NeoForge registers both directions through a single {@code RegisterPayloadHandlersEvent}.
 */
public class ClientCommandsServer {
    private static final URI WHY_URI = URI.create("https://github.com/Earthcomputer/clientcommands/blob/" + BuildInfo.COMMIT_HASH + "/docs/server_installation.md");
    private static final SimpleCommandExceptionType MUST_BE_INSTALLED_EXCEPTION = new SimpleCommandExceptionType(
        Component.translatable(
            "commands.client.mustBeInstalledOnServer",
            Component.translatable("commands.client.mustBeInstalledOnServer.why")
                .withStyle(style -> style.withUnderlined(true).withClickEvent(new ClickEvent.OpenUrl(WHY_URI)))
        )
    );

    public static void requirePrivileges() throws CommandSyntaxException {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            return;
        }

        throw MUST_BE_INSTALLED_EXCEPTION.create();
    }

    private enum OptInPayload implements CustomPacketPayload {
        INSTANCE;

        static final Type<OptInPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("clientcommands", "opt_in"));
        static final StreamCodec<ByteBuf, OptInPayload> CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<OptInPayload> type() {
            return TYPE;
        }
    }
}
