package net.earthcomputer.clientcommands.compat;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandSourceStack;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Bridges a Brigadier tree rooted at {@link FabricClientCommandSource} onto the vanilla
 * {@link CommandSourceStack} dispatcher that NeoForge exposes for client commands.
 *
 * <p>Brigadier's node types are parameterised by their source type and are invariant, so a
 * {@code LiteralCommandNode<FabricClientCommandSource>} is not a
 * {@code LiteralCommandNode<CommandSourceStack>} and the tree cannot simply be grafted. Instead each
 * clientcommands root is rebuilt as a {@code CommandSourceStack} node whose commands adapt the
 * incoming vanilla source into a {@link ClientCommandSourceImpl} via
 * {@link CommandContext#copyFor(Object)}.
 *
 * <p>Adapting at execution time is what keeps the ported bodies unchanged: they retain their
 * {@code CommandContext<FabricClientCommandSource>} signatures, and every helper in the
 * {@code clientarguments} library (compiled against that same type) keeps working.
 */
public final class CommandTreeBridge {
    private CommandTreeBridge() {
    }

    /** Copies every root child of {@code client} into {@code vanilla}, adapting sources on execute. */
    public static void bridge(CommandDispatcher<FabricClientCommandSource> client,
                              CommandDispatcher<CommandSourceStack> vanilla) {
        for (CommandNode<FabricClientCommandSource> node : client.getRoot().getChildren()) {
            vanilla.getRoot().addChild(convert(node));
        }
    }

    @SuppressWarnings("unchecked")
    private static CommandNode<CommandSourceStack> convert(CommandNode<FabricClientCommandSource> node) {
        CommandNode<CommandSourceStack> converted;

        if (node instanceof LiteralCommandNode<FabricClientCommandSource> literal) {
            LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder
                .<CommandSourceStack>literal(literal.getLiteral())
                .requires(adaptRequirement(literal.getRequirement()));
            if (literal.getCommand() != null) {
                builder.executes(adaptCommand(literal.getCommand()));
            }
            converted = builder.build();
        } else if (node instanceof ArgumentCommandNode<FabricClientCommandSource, ?> argument) {
            RequiredArgumentBuilder<CommandSourceStack, Object> builder = RequiredArgumentBuilder
                .<CommandSourceStack, Object>argument(argument.getName(),
                    (com.mojang.brigadier.arguments.ArgumentType<Object>) argument.getType())
                .requires(adaptRequirement(argument.getRequirement()));
            if (argument.getCommand() != null) {
                builder.executes(adaptCommand(argument.getCommand()));
            }
            converted = builder.build();
        } else {
            throw new IllegalArgumentException("Unsupported command node: " + node.getClass());
        }

        for (CommandNode<FabricClientCommandSource> child : node.getChildren()) {
            converted.addChild(convert(child));
        }
        return converted;
    }

    private static Predicate<CommandSourceStack> adaptRequirement(
            Predicate<FabricClientCommandSource> requirement) {
        return stack -> requirement.test(ClientCommandSourceImpl.of(stack));
    }

    private static Command<CommandSourceStack> adaptCommand(
            Command<FabricClientCommandSource> command) {
        return context -> {
            // Brigadier stores the source that a redirect produced on the context itself, so it is
            // read from there rather than rebuilt: /cenchant --simulate redirects back onto the base
            // node with a source that remembers the flag, and recreating the source here would drop it.
            ClientCommandSourceImpl previous = ClientCommandSourceImpl.ACTIVE.get();
            ClientCommandSourceImpl source = ClientCommandSourceImpl.adopt(context.getSource());
            ClientCommandSourceImpl.ACTIVE.set(source);
            try {
                return command.run(adaptContext(context));
            } finally {
                ClientCommandSourceImpl.ACTIVE.set(previous);
            }
        };
    }

    /**
     * Rebuilds the context with a {@link FabricClientCommandSource} source.
     *
     * <p>{@code copyFor} keeps the context's own source type parameter, so it cannot change
     * {@code CommandSourceStack} into {@link FabricClientCommandSource}; the constructor is used
     * instead.
     *
     * <p>The parsed arguments and the visited nodes must be carried across, not rebuilt. Brigadier
     * exposes no getter for either, so they are read from the original context's fields. Passing an
     * empty map here looks harmless for commands that only touch {@code getSource()}, but any command
     * that actually reads an argument fails with
     *
     *   IllegalArgumentException: No such argument '<name>' exists on this command
     *
     * which is what {@code /cenchant} does through
     * {@link net.earthcomputer.clientcommands.command.arguments.ItemAndEnchantmentsPredicateArgument}.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static CommandContext<FabricClientCommandSource> adaptContext(
            CommandContext<CommandSourceStack> context) {
        return new CommandContext(
            ClientCommandSourceImpl.ACTIVE.get(),
            context.getInput(),
            argumentsOf(context),
            context.getCommand(),
            context.getRootNode(),
            nodesOf(context),
            context.getRange(),
            null,
            null,
            context.isForked());
    }

    /**
     * Reads {@code CommandContext.arguments}, which has no accessor.
     *
     * <p>The map's values are {@code ParsedArgument<S, ?>} typed over the original source; they carry
     * only the raw parsed value and its range, and {@code getArgument} reads the value without
     * consulting the source type, so the same map is valid for the adapted context.
     */
    private static Map<String, ParsedArgument<CommandSourceStack, ?>> argumentsOf(
            CommandContext<CommandSourceStack> context) {
        try {
            Field field = CommandContext.class.getDeclaredField("arguments");
            field.setAccessible(true);
            return (Map<String, ParsedArgument<CommandSourceStack, ?>>) field.get(context);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to read CommandContext.arguments", e);
        }
    }

    /** Reads {@code CommandContext.nodes}, which likewise has no accessor. */
    private static List<ParsedCommandNode<CommandSourceStack>> nodesOf(
            CommandContext<CommandSourceStack> context) {
        try {
            Field field = CommandContext.class.getDeclaredField("nodes");
            field.setAccessible(true);
            return (List<ParsedCommandNode<CommandSourceStack>>) field.get(context);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to read CommandContext.nodes", e);
        }
    }
}
