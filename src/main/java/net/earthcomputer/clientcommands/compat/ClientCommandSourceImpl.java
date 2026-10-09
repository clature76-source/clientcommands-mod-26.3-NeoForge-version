package net.earthcomputer.clientcommands.compat;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.earthcomputer.clientcommands.command.Flag;
import net.earthcomputer.clientcommands.interfaces.IClientSuggestionsProvider;
import net.earthcomputer.clientcommands.interfaces.IClientSuggestionsProvider_Alias;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Concrete {@link FabricClientCommandSource} backed by the live {@link Minecraft} client,
 * optionally pinned to a specific entity.
 *
 * <p>Fabric ships this implementation inside its command API; NeoForge has no client command
 * dispatcher, so the port provides one. Output goes to the client HUD and never reaches the server,
 * matching Fabric's strictly client-side behaviour.
 *
 * <p>Because {@link FabricClientCommandSource} extends {@link SharedSuggestionProvider} (itself
 * extending {@code PermissionSetSupplier}), the suggestion surface is implemented by mirroring
 * vanilla's {@link CommandSourceStack} where a client-side equivalent exists, and returning empty
 * results where the information only exists server-side.
 */
public final class ClientCommandSourceImpl implements FabricClientCommandSource, IClientSuggestionsProvider, IClientSuggestionsProvider_Alias {
    private final Minecraft client;
    @Nullable
    private final Entity entity;
    private final Map<Flag<?>, Object> flags;
    private final Set<String> seenAliases;

    public ClientCommandSourceImpl() {
        this(null);
    }

    public ClientCommandSourceImpl(@Nullable Entity entity) {
        this(entity, Map.of(), new HashSet<>());
    }

    private ClientCommandSourceImpl(@Nullable Entity entity, Map<Flag<?>, Object> flags, Set<String> seenAliases) {
        this.client = Minecraft.getInstance();
        this.entity = entity;
        this.flags = flags;
        this.seenAliases = seenAliases;
    }

    /** Builds a source from a vanilla command source, preserving its executing entity. */
    public static ClientCommandSourceImpl of(CommandSourceStack stack) {
        return adopt(stack);
    }

    /**
     * Recovers the {@link ClientCommandSourceImpl} that a value travelling through Brigadier carries.
     *
     * <p>Two shapes reach here. During normal traversal the value is a plain
     * {@link CommandSourceStack}, so the active source is returned -- the bridge installed it for this
     * execution and it already holds the flags parsed so far. After a flag redirect, Brigadier stores
     * whatever the {@code RedirectModifier} returned, which is the {@link FabricClientCommandSource}
     * produced by {@code ClientCommandHelper.withFlag}; that object is unwrapped directly so the flag
     * it carries survives.
     */
    public static ClientCommandSourceImpl adopt(Object source) {
        if (source instanceof ClientCommandSourceImpl impl) {
            return impl;
        }
        if (source instanceof FlaggedSource flagged) {
            return flagged.clientcommands_source();
        }
        return ACTIVE.get();
    }

    /**
     * The source instance currently executing a bridged client command.
     *
     * <p>Installed by {@link CommandTreeBridge} for the duration of each execution. Redirect chains
     * re-enter {@link #of(CommandSourceStack)} from inside the command body, which is why the state
     * has to be reachable without threading it through Brigadier.
     */
    static final ThreadLocal<ClientCommandSourceImpl> ACTIVE =
        ThreadLocal.withInitial(ClientCommandSourceImpl::new);

    /**
     * Marker for a flag-bearing source.
     *
     * <p>A redirect modifier returns a {@link FabricClientCommandSource} even though the tree it is
     * registered on is typed over {@link CommandSourceStack}; the value survives on the context only
     * because of erasure. Marking it lets {@link #adopt(Object)} recognise it without an unchecked cast
     * back to a type the node was never parameterised with.
     */
    interface FlaggedSource {
        ClientCommandSourceImpl clientcommands_source();
    }

    // ---- IClientSuggestionsProvider ----------------------------------------
    //
    // Fabric stores flag state on its own ClientSuggestionProvider, which it also installs as the
    // source of the client command dispatcher. NeoForge has no such dispatcher: the port's commands
    // run against this class. The flag surface therefore lives here instead of on a mixin into
    // vanilla's ClientSuggestionProvider, which the bridged tree never sees.

    @SuppressWarnings("unchecked")
    @Override
    public <T> T clientcommands_getFlag(Flag<T> flag) {
        return (T) this.flags.getOrDefault(flag, flag.getDefaultValue());
    }

    @Override
    public <T> IClientSuggestionsProvider clientcommands_withFlag(Flag<T> flag, T value) {
        Map<Flag<?>, Object> newFlags = new HashMap<>(this.flags);
        newFlags.put(flag, value);
        return new ClientCommandSourceImpl(this.entity, Map.copyOf(newFlags), this.seenAliases);
    }

    @Override
    @Nullable
    public List<Suggestion> clientcommands_filterSuggestions(List<Suggestion> suggestions) {
        if (flags.isEmpty()) {
            return null;
        }
        return suggestions.stream().filter(suggestion -> {
            String text = suggestion.getText();
            return !Flag.isFlag(text) || flags.keySet().stream().noneMatch(arg -> !arg.isRepeatable() && (text.equals(arg.getFlag()) || text.equals(arg.getShortFlag())));
        }).toList();
    }

    // ---- IClientSuggestionsProvider_Alias ----------------------------------

    @Override
    public void clientcommands_addSeenAlias(String alias) {
        seenAliases.add(alias);
    }

    @Override
    public void clientcommands_removeSeenAlias(String alias) {
        seenAliases.remove(alias);
    }

    @Override
    public boolean clientcommands_isAliasSeen(String alias) {
        return seenAliases.contains(alias);
    }

    @Override
    public void sendFeedback(Component message) {
        client.gui.hud.getChat().addClientSystemMessage(message);
    }

    @Override
    public void sendError(Component message) {
        sendFeedback(Component.empty().append(message).withStyle(net.minecraft.ChatFormatting.RED));
    }

    @Override
    public Minecraft getClient() {
        return client;
    }

    @Override
    public LocalPlayer getPlayer() {
        return client.player;
    }

    /**
     * The level name comes from the real Fabric API interface, which declares {@code getLevel()} and
     * has no {@code getWorld()}. This mod's own code never called {@code getWorld()}, so only the name
     * has to match here.
     */
    @Override
    public ClientLevel getLevel() {
        return client.level;
    }

    @Override
    public Entity getEntity() {
        if (entity != null) {
            return entity;
        }
        return getPlayer();
    }

    @Override
    public Vec3 getPosition() {
        Entity e = getEntity();
        return e == null ? Vec3.ZERO : e.position();
    }

    @Override
    public Vec2 getRotation() {
        Entity e = getEntity();
        return e == null ? Vec2.ZERO : e.getRotationVector();
    }

    // ---- PermissionSetSupplier ----------------------------------------------

    @Override
    public PermissionSet permissions() {
        // Client commands run with whatever permissions the player has locally; the mod gates
        // server-affecting commands separately via ClientCommandsServer#requirePrivileges.
        LocalPlayer player = getPlayer();
        return player == null ? PermissionSet.NO_PERMISSIONS : player.permissions();
    }

    // ---- SharedSuggestionProvider -------------------------------------------

    @Override
    public Collection<String> getOnlinePlayerNames() {
        var connection = client.getConnection();
        if (connection == null) {
            return List.of();
        }
        return connection.getOnlinePlayers().stream()
            .map(info -> info.getProfile().name())
            .toList();
    }

    @Override
    public Collection<String> getAllTeams() {
        ClientLevel level = getLevel();
        if (level == null) {
            return List.of();
        }
        return level.getScoreboard().getTeamNames();
    }

    @Override
    public Stream<Identifier> getAvailableSounds() {
        return BuiltInRegistries.SOUND_EVENT.stream().map(SoundEvent::location);
    }

    @Override
    public Stream<Identifier> getAvailablePostEffects() {
        return Stream.empty();
    }

    @Override
    public CompletableFuture<Suggestions> customSuggestion(CommandContext<?> context) {
        return Suggestions.empty();
    }

    @Override
    public <E> CompletableFuture<Suggestions> suggestRegistryElements(ResourceKey<? extends Registry<E>> key,
                                                                     SharedSuggestionProvider.ElementSuggestionType elements,
                                                                     SuggestionsBuilder builder,
                                                                     CommandContext<?> context,
                                                                     Predicate<E> filter) {
        return getLookup(key)
            .map(registry -> {
                suggestRegistryElements(registry, elements, builder, filter);
                return builder.buildFuture();
            })
            .orElseGet(Suggestions::empty);
    }

    @SuppressWarnings("unchecked")
    private <E> Optional<? extends HolderLookup<E>> getLookup(ResourceKey<? extends Registry<E>> key) {
        Optional<? extends Registry<E>> lookup = registryAccess().lookup(key);
        return lookup;
    }

    @Override
    public Set<ResourceKey<Level>> levels() {
        var connection = client.getConnection();
        return connection == null ? Set.of() : connection.levels();
    }

    @Override
    public RegistryAccess registryAccess() {
        ClientLevel level = getLevel();
        return level == null ? RegistryAccess.EMPTY : level.registryAccess();
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        var connection = client.getConnection();
        return connection == null ? FeatureFlags.VANILLA_SET : connection.enabledFeatures();
    }
}
