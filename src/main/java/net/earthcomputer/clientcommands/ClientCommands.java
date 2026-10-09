// CHECKSTYLE:OFF: AvoidStarImport allow commands to be wildcard imported
package net.earthcomputer.clientcommands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.logging.LogUtils;
import dev.xpple.betterconfig.BetterConfigClient;
import dev.xpple.betterconfig.api.BetterConfigAPI;
import dev.xpple.betterconfig.api.ModConfigBuilder;
import dev.xpple.clientarguments.ClientArguments;
import dev.xpple.simplewaypoints.SimpleWaypoints;
import dev.xpple.simplewaypoints.api.SimpleWaypointsAPI;
import net.earthcomputer.clientcommands.command.*;
import net.earthcomputer.clientcommands.compat.ActiveDispatcher;
import net.earthcomputer.clientcommands.compat.CommandTreeBridge;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.earthcomputer.clientcommands.compat.fabric.FabricLoader;
import net.earthcomputer.clientcommands.event.ClientConnectionEvents;
import net.earthcomputer.clientcommands.features.CommandExecutionCustomPayload;
import net.earthcomputer.clientcommands.features.EnchantmentCracker;
import net.earthcomputer.clientcommands.features.FishingCracker;
import net.earthcomputer.clientcommands.features.PlayerRandCracker;
import net.earthcomputer.clientcommands.features.Relogger;
import net.earthcomputer.clientcommands.features.ServerBrandManager;
import net.earthcomputer.clientcommands.features.Waypoints;
import net.earthcomputer.clientcommands.render.RenderQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * NeoForge entrypoint for the ported clientcommands, corresponding to the Fabric build's
 * {@code ClientModInitializer}.
 *
 * <p>The main structural difference is command registration. Fabric supplies a dedicated client
 * command dispatcher; NeoForge's {@link RegisterClientCommandsEvent} instead exposes the vanilla
 * dispatcher. The ported tree is therefore built on a {@link CommandDispatcher} of
 * {@link FabricClientCommandSource} and grafted onto the vanilla root as literal nodes, which keeps the
 * commands strictly client-side and never sent to the server.
 */
@Mod(value = "clientcommands", dist = Dist.CLIENT)
@EventBusSubscriber(modid = "clientcommands", value = Dist.CLIENT)
public class ClientCommands {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("clientcommands");
    private static final Set<String> clientcommandsCommands = new HashSet<>();
    private static final Set<String> COMMANDS_TO_NOT_SEND_TO_SERVER = Set.of("cwe", "cnote"); // could contain private information

    public static boolean scrambleWindowTitle = false;

    private static final Set<String> SCRAMBLE_WINDOW_TITLE_VICTIMS = Set.of(
        "fa68270b-1071-46c6-ac5c-6c4a0b777a96", // Earthcomputer
        "d4557649-e553-413e-a019-56d14548df96", // Azteched
        "8dc3d945-cf90-47c1-a122-a576319d05a7", // samnrad
        "c5d72740-cabc-42d1-b789-27859041d553", // allocator
        "e4093360-a200-4f99-aa13-be420b8d9a79", // Rybot666
        "083fb87e-c9e4-4489-8fb7-a45b06bfca90", // Kerbaras
        "973e8f6e-2f51-4307-97dc-56fdc71d194f" // KatieTheQt
    );

    private static final Set<String> CHAT_COMMAND_USERS = Set.of(
        "b793c3b9-425f-4dd8-a056-9dec4d835e24", // wsb
        "0071ccd7-467f-4e71-8237-cb15f229a1ff", // 8YX
        "c3bca648-b8ce-491d-bf6a-36bb42c5a70b" // Y99
    );

    public ClientCommands() {
        // NOTE: NeoForge constructs the mod class during `constructMods`, which runs before the
        // Minecraft client instance exists. Anything touching Minecraft.getInstance() must therefore
        // be deferred until the client is up (see shouldScrambleWindowTitle).
        RenderQueue.register();
    }

    /**
     * Resolves the scramble-window-title flag lazily, at the moment the window title is actually
     * built.
     *
     * <p>On Fabric the mod initializer runs after the client exists, so this could be computed once
     * up front. On NeoForge the mod constructor runs during mod construction
     * ({@code ClientModLoader.begin()}, before {@code Minecraft} is constructed), where
     * {@code Minecraft.getInstance()} is still null. Resolving it here removes that lifecycle
     * dependency: {@code createTitle} only runs once the client and its user profile exist. The
     * result is cached, so the shuffle stays stable across title updates.
     */
    public static boolean shouldScrambleWindowTitle() {
        if (scrambleWindowTitle) {
            return true;
        }
        if (Boolean.getBoolean("clientcommands.scrambleWindowTitle")) {
            return scrambleWindowTitle = true;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getUser() == null) {
            return false;
        }
        String playerUUID = String.valueOf(minecraft.getUser().getProfileId());
        if (SCRAMBLE_WINDOW_TITLE_VICTIMS.contains(playerUUID)) {
            return scrambleWindowTitle = true;
        }
        return false;
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // This mirrors Fabric's `onInitializeClient`: it runs after the client exists, which is the
        // right place for the config/event wiring below. The mod constructor only holds work that is
        // safe during mod construction.
        event.enqueueWork(() -> {
            // Config
            try {
                Files.createDirectories(CONFIG_DIR);
            } catch (IOException e) {
                LOGGER.error("Failed to create config dir", e);
            }

            new ModConfigBuilder<>("clientcommands", Configs.class).build();
            ClientConnectionEvents.DISCONNECT.register(() -> {
                if (!Relogger.isRelogging) {
                    BetterConfigAPI.getInstance().getModConfig("clientcommands").resetTemporaryConfigs();
                }
            });

            Waypoints.migrateWaypoints();
            SimpleWaypointsAPI.getInstance().registerCommandAlias("cwaypoint");

            // Events
            EnchantmentCracker.registerEvents();
            FishingCracker.registerEvents();
            PlayerRandCracker.registerEvents();
            ServerBrandManager.registerEvents();

            CreativeTabCommand.registerCreativeTabs();
        });

        // Disconnect notification is a NeoForge client event rather than a Fabric mixin.
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut loggingOut) ->
            ClientConnectionEvents.DISCONNECT.invoker().onDisconnect());
    }

    @SubscribeEvent
    static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        // Serverbound: the client tells an opted-in server which commands it ran. A vanilla server
        // never sees this because sending is gated on the server advertising support.
        registrar.playToServer(CommandExecutionCustomPayload.TYPE, CommandExecutionCustomPayload.CODEC,
            (payload, context) -> {
            });
    }

    @SubscribeEvent
    static void onRegisterRenderPipelines(RegisterRenderPipelinesEvent event) {
        RenderQueue.registerPipelines(event);
    }

    private static Set<String> getCommands(CommandDispatcher<?> dispatcher) {
        return dispatcher.getRoot().getChildren().stream().flatMap(node -> node instanceof LiteralCommandNode<?> literal ? Stream.of(literal.getLiteral()) : Stream.empty()).collect(Collectors.toSet());
    }

    public static void sendCommandExecutionToServer(String command) {
        StringReader reader = new StringReader(command);
        reader.skipWhitespace();
        String theCommand = reader.readUnquotedString();
        if (clientcommandsCommands.contains(theCommand) && !COMMANDS_TO_NOT_SEND_TO_SERVER.contains(theCommand)) {
            if (Minecraft.getInstance().getConnection() != null) {
                ClientPacketDistributor.sendToServer(new CommandExecutionCustomPayload(command));
            }
        }
    }

    public static boolean isClientcommandsCommand(String commandName) {
        return clientcommandsCommands.contains(commandName);
    }

    /**
     * Builds the client command tree and grafts it onto the vanilla client dispatcher.
     *
     * <p>Each clientcommands root literal is added as a child of the vanilla root, which is how
     * NeoForge client commands are exposed. Commands execute against a {@link FabricClientCommandSource} so
     * the ported bodies behave as they did on Fabric.
     */
    @SubscribeEvent
    static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> vanillaDispatcher = event.getDispatcher();
        Set<String> existingCommands = getCommands(vanillaDispatcher);

        CommandDispatcher<FabricClientCommandSource> dispatcher = new CommandDispatcher<>();
        registerCommands(dispatcher, event.getBuildContext());

        // The embedded xpple libraries are Fabric mods. Fabric Loader used to invoke their entrypoints
        // from their own fabric.mod.json, which is excluded here (NeoForge cannot read it, and a stray
        // descriptor breaks mod discovery), so nothing called them and their commands never appeared.
        //
        // This is not cosmetic: /cenchant tells the player to run
        // "/cconfig clientcommands enchantingPrediction set true" when enchantment prediction is off,
        // and /cconfig is registered by BetterConfig through this very callback.
        //
        // Two constraints shape the calls below:
        //
        //  * Each entrypoint must run exactly once. This event fires again every time the server sends
        //    its command tree (ClientCommandHandler.mergeServerCommands), and BetterConfig's
        //    ModConfigBuilder.build uses putIfAbsent and throws IllegalArgumentException when a config
        //    id is registered twice. Re-entering here therefore disconnects the player.
        //  * A failure must stay local. An exception escaping this listener aborts command registration
        //    and the client desyncs, so each call is contained on its own.
        initializeEmbeddedLibrary("betterconfig", () -> new BetterConfigClient().onInitializeClient());
        initializeEmbeddedLibrary("simplewaypoints", () -> new SimpleWaypoints().onInitializeClient());
        initializeEmbeddedLibrary("clientarguments", () -> new ClientArguments().onInitializeClient());

        // NeoForge has no equivalent of this Fabric callback, so the port dispatches it itself once the
        // client tree exists. Listeners append their roots to `dispatcher`, which the bridge below then
        // grafts onto the vanilla dispatcher exactly like this mod's own commands.
        try {
            ClientCommandRegistrationCallback.EVENT.invoker().register(dispatcher, event.getBuildContext());
        } catch (Throwable t) {
            LOGGER.error("A ClientCommandRegistrationCallback listener failed", t);
        }

        // Expose the client tree to commands that re-parse through the active dispatcher.
        ActiveDispatcher.set(dispatcher);

        // The client tree is rooted at FabricClientCommandSource while the vanilla dispatcher is
        // rooted at CommandSourceStack; the bridge rebuilds the tree and adapts sources on execute.
        CommandTreeBridge.bridge(dispatcher, vanillaDispatcher);

        clientcommandsCommands.clear();
        for (String command : getCommands(vanillaDispatcher)) {
            if (!existingCommands.contains(command)) {
                clientcommandsCommands.add(command);
            }
        }
    }

    public static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext context) {
        // Each command is registered through `register`, which isolates failures: command registration
        // is all-or-nothing, so without this a single failing command (for example one hitting a
        // library incompatibility) would abort registration of every other command and leave the
        // player with no clientcommands at all.
        register("alias", () -> AliasCommand.register(dispatcher));
        register("areastats", () -> AreaStatsCommand.register(dispatcher, context));
        register("cauditmixins", () -> AuditMixinsCommand.register(dispatcher));
        register("book", () -> BookCommand.register(dispatcher));
        register("buildinfo", () -> BuildInfoCommand.register(dispatcher));
        register("calc", () -> CalcCommand.register(dispatcher));
        register("calcstack", () -> CalcStackCommand.register(dispatcher, context));
        register("callback", () -> CallbackCommand.register(dispatcher));
        register("chess", () -> ChessCommand.register(dispatcher));
        register("cdebug", () -> CDebugCommand.register(dispatcher));
        register("cenchant", () -> CEnchantCommand.register(dispatcher, context));
        register("cfunction", () -> CFunctionCommand.register(dispatcher));
        register("cgamemode", () -> CGameModeCommand.register(dispatcher));
        register("cgive", () -> CGiveCommand.register(dispatcher, context));
        register("chorus", () -> ChorusCommand.register(dispatcher));
        register("connectfour", () -> ConnectFourCommand.register(dispatcher));
        register("cparticle", () -> CParticleCommand.register(dispatcher, context));
        register("cplaysound", () -> CPlaySoundCommand.register(dispatcher));
        register("crackrng", () -> CrackRNGCommand.register(dispatcher));
        register("creativetab", () -> CreativeTabCommand.register(dispatcher, context));
        register("cstopsound", () -> CStopSoundCommand.register(dispatcher));
        register("cteleport", () -> CTeleportCommand.register(dispatcher));
        register("ctellraw", () -> CTellRawCommand.register(dispatcher, context));
        register("ctime", () -> CTimeCommand.register(dispatcher));
        register("ctitle", () -> CTitleCommand.register(dispatcher, context));
        register("findblock", () -> FindBlockCommand.register(dispatcher, context));
        register("find", () -> FindCommand.register(dispatcher));
        register("finditem", () -> FindItemCommand.register(dispatcher, context));
        register("fish", () -> FishCommand.register(dispatcher, context));
        register("fov", () -> FovCommand.register(dispatcher));
        register("framerate", () -> FramerateCommand.register(dispatcher));
        register("gamma", () -> GammaCommand.register(dispatcher));
        register("getdata", () -> GetDataCommand.register(dispatcher));
        register("ghostblock", () -> GhostBlockCommand.register(dispatcher, context));
        register("glow", () -> GlowCommand.register(dispatcher));
        register("hotbar", () -> HotbarCommand.register(dispatcher));
        register("kit", () -> KitCommand.register(dispatcher));
        register("listen", () -> ListenCommand.register(dispatcher));
        register("look", () -> LookCommand.register(dispatcher));
        register("map", () -> MapCommand.register(dispatcher));
        register("minesweeper", () -> MinesweeperCommand.register(dispatcher));
        register("mote", () -> MoteCommand.register(dispatcher));
        register("note", () -> NoteCommand.register(dispatcher));
        register("permissionlevel", () -> PermissionLevelCommand.register(dispatcher));
        register("ping", () -> PingCommand.register(dispatcher));
        // PlayerInfoCommand.register(dispatcher);
        register("plugins", () -> PluginsCommand.register(dispatcher));
        register("pos", () -> PosCommand.register(dispatcher));
        register("posteffect", () -> PostEffectCommand.register(dispatcher));
        register("predictbrushables", () -> PredictBrushablesCommand.register(dispatcher));
        register("relog", () -> RelogCommand.register(dispatcher));
        register("render", () -> RenderCommand.register(dispatcher));
        register("reply", () -> ReplyCommand.register(dispatcher));
        register("shrug", () -> ShrugCommand.register(dispatcher));
        register("signsearch", () -> SignSearchCommand.register(dispatcher));
        register("snake", () -> SnakeCommand.register(dispatcher));
        register("snap", () -> SnapCommand.register(dispatcher));
        register("startup", () -> StartupCommand.register(dispatcher));
        register("task", () -> TaskCommand.register(dispatcher));
        register("tictactoe", () -> TicTacToeCommand.register(dispatcher));
        register("tooltip", () -> TooltipCommand.register(dispatcher, context));
        register("translate", () -> TranslateCommand.register(dispatcher));
        register("usagetree", () -> UsageTreeCommand.register(dispatcher));
        register("uuid", () -> UuidCommand.register(dispatcher));
        register("var", () -> VarCommand.register(dispatcher));
        register("weather", () -> WeatherCommand.register(dispatcher));
        register("whisperencrypted", () -> WhisperEncryptedCommand.register(dispatcher));
        register("wiki", () -> WikiCommand.register(dispatcher));
        register("windowsize", () -> WindowSizeCommand.register(dispatcher));

        Calendar calendar = Calendar.getInstance();
        boolean registerChatCommand = calendar.get(Calendar.MONTH) == Calendar.APRIL && calendar.get(Calendar.DAY_OF_MONTH) == 1;
        registerChatCommand |= CHAT_COMMAND_USERS.contains(String.valueOf(Minecraft.getInstance().getUser().getProfileId()));
        registerChatCommand |= Boolean.getBoolean("clientcommands.debugChatCommand");
        if (registerChatCommand) {
            register("chat", () -> ChatCommand.register(dispatcher));
        }

        if (!failedCommands.isEmpty()) {
            LOGGER.error("{} clientcommands command(s) failed to register and will be unavailable: {}",
                failedCommands.size(), String.join(", ", failedCommands));
        }
    }

    /** Names of commands whose registration threw, reported once registration completes. */
    private static final Set<String> failedCommands = new HashSet<>();

    /**
     * Runs one command's registration, keeping a failure local to that command.
     *
     * <p>Registration builds a single shared dispatch tree, so an uncaught throw used to abort the
     * whole loop and leave the player with no clientcommands at all. Isolating each command means a
     * broken one costs only itself.
     */
    private static void register(String name, Runnable registration) {
        try {
            registration.run();
        } catch (Throwable t) {
            failedCommands.add(name);
            LOGGER.error("Failed to register clientcommands command /{}", name, t);
        }
    }

    /** Embedded libraries whose entrypoint has already been invoked; see the call site for why. */
    private static final Set<String> initializedLibraries = new HashSet<>();

    /**
     * Invokes an embedded library's Fabric entrypoint exactly once, containing any failure.
     *
     * <p>The once-only guard is essential rather than defensive: {@code RegisterClientCommandsEvent}
     * fires again for every command tree the server sends, and BetterConfig's
     * {@code ModConfigBuilder.build} throws {@code IllegalArgumentException} if a config id is
     * registered twice. Without the guard the second command packet disconnects the player with
     * {@code IllegalArgumentException: simplewaypoints}.
     *
     * <p>Containment matters as much as the guard: an exception escaping the listener aborts command
     * registration part-way, which leaves the client's command tree inconsistent and crashes the
     * render thread later. A library that fails here only loses its own commands.
     */
    private static void initializeEmbeddedLibrary(String name, Runnable initializer) {
        if (!initializedLibraries.add(name)) {
            return;
        }
        try {
            initializer.run();
        } catch (Throwable t) {
            LOGGER.error("Failed to initialize embedded library '{}'; its commands will be unavailable", name, t);
        }
    }
}
