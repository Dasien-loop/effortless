package dev.huskuraft.effortless;

import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.UnaryOperator;

import dev.huskuraft.universal.api.core.Player;
import dev.huskuraft.universal.api.core.World;
import dev.huskuraft.universal.api.platform.Server;
import dev.huskuraft.universal.api.text.ChatFormatting;
import dev.huskuraft.universal.api.text.Text;
import dev.huskuraft.effortless.building.BuildResult;
import dev.huskuraft.effortless.building.Context;
import dev.huskuraft.effortless.building.Storage;
import dev.huskuraft.effortless.building.StructureBuilder;
import dev.huskuraft.effortless.building.clipboard.Clipboard;
import dev.huskuraft.effortless.building.history.OperationResultStack;
import dev.huskuraft.effortless.building.operation.OperationResult;
import dev.huskuraft.effortless.building.operation.batch.BatchOperationResult;
import dev.huskuraft.effortless.building.operation.block.BlockOperationResult;
import dev.huskuraft.effortless.building.pattern.Pattern;
import dev.huskuraft.effortless.building.replace.Replace;
import dev.huskuraft.effortless.building.session.BatchBuildSession;
import dev.huskuraft.effortless.building.structure.builder.Structure;
import dev.huskuraft.effortless.networking.packets.player.PlayerBuildPacket;
import dev.huskuraft.effortless.networking.packets.player.PlayerBuildTooltipPacket;
import dev.huskuraft.effortless.networking.packets.player.PlayerMaterialSnapshotPacket;

public final class EffortlessStructureBuilder extends StructureBuilder {

    private final Effortless entrance;

    private final Map<UUID, Context> contexts = new HashMap<>();
    private final Map<UUID, OperationResultStack> undoRedoStacks = new HashMap<>();

    public EffortlessStructureBuilder(Effortless entrance) {
        this.entrance = entrance;

        getEntrance().getEventRegistry().getPlayerChangeWorldEvent().register(this::onPlayerChangeWorld);
        getEntrance().getEventRegistry().getPlayerRespawnEvent().register(this::onPlayerRespawn);
        getEntrance().getEventRegistry().getPlayerLoggedInEvent().register(this::onPlayerLoggedIn);
        getEntrance().getEventRegistry().getPlayerLoggedOutEvent().register(this::onPlayerLoggedOut);

        getEntrance().getEventRegistry().getServerStartedEvent().register(this::onServerStarted);
        getEntrance().getEventRegistry().getServerStoppedEvent().register(this::onServerStopped);
    }

    public Effortless getEntrance() {
        return entrance;
    }


    @Override
    public BuildResult updateContext(Player player, UnaryOperator<Context> updater) {
        var context = updater.apply(getContext(player));
        if (context == null) {
            return BuildResult.CANCELED;
        }
        setContext(player, context);
        return context.isFulfilled() ? BuildResult.COMPLETED
                : context.isIdle() ? BuildResult.CANCELED : BuildResult.PARTIAL;
    }

    @Override
    public Context getDefaultContext(Player player) {
        var config = getEntrance().getSessionConfigStorage().get();
        var constraint = config == null ? dev.huskuraft.effortless.session.config.ConstraintConfig.DEFAULT : config.getByPlayer(player);
        return Context.defaultSet().withConstraintConfig(constraint);
    }

    @Override
    public Context getContext(Player player) {
        return contexts.computeIfAbsent(player.getId(), ignored -> getDefaultContext(player));
    }

    @Override
    public Context getContextTraced(Player player) {
        var context = getContext(player).finalize(player, dev.huskuraft.effortless.building.BuildStage.INTERACT);
        if (context.isInteractionEmpty()) {
            if (context.clipboard().enabled()) {
                context = context.withBuildState(context.clipboard().isEmpty()
                        ? dev.huskuraft.effortless.building.BuildState.COPY_STRUCTURE
                        : dev.huskuraft.effortless.building.BuildState.PASTE_STRUCTURE);
            } else if (player.getItemStack(dev.huskuraft.universal.api.core.InteractionHand.MAIN).isBlock()) {
                context = context.withBuildState(dev.huskuraft.effortless.building.BuildState.PLACE_BLOCK);
            } else if (player.getItemStack(dev.huskuraft.universal.api.core.InteractionHand.MAIN).isDamageableItem()) {
                context = context.withBuildState(dev.huskuraft.effortless.building.BuildState.BREAK_BLOCK);
            } else {
                context = context.withBuildState(dev.huskuraft.effortless.building.BuildState.INTERACT_BLOCK);
            }
        }
        return context.withNextInteraction(context.trace(player));
    }

    @Override
    public Map<UUID, Context> getAllContexts() {
        return contexts;
    }

    public void onTick() {
    }

    @Override
    public boolean setContext(Player player, Context context) {
        if (context == null) return false;
        contexts.put(player.getId(), context);
        return true;
    }

    @Override
    public boolean setStructure(Player player, Structure structure) {
        if (structure == null) return false;
        return setContext(player, getContext(player).newInteraction().withStructure(structure).withEmptyClipboard());
    }

    @Override
    public boolean setClipboard(Player player, Clipboard clipboard) {
        if (clipboard == null) return false;
        return setContext(player, getContext(player).newInteraction().withClipboard(clipboard));
    }

    @Override
    public boolean setPattern(Player player, Pattern pattern) {
        if (pattern == null) return false;
        return setContext(player, getContext(player).withPattern(pattern));
    }

    @Override
    public boolean setReplace(Player player, Replace replace) {
        if (replace == null) return false;
        return setContext(player, getContext(player).withReplace(replace));
    }

    @Override
    public void resetAll() {
        contexts.clear();
        undoRedoStacks.clear();
    }

    @Override
    public void onContextReceived(Player player, Context context) {
        if (!checkPermission(player, context)) {
            if (context.isBuildType()) {
                player.sendMessage(Effortless.getSystemMessage(Text.text("Your session config is outdated. Please try to rejoin the server!")));
                Effortless.LOGGER.warn("%s has an outdated session config".formatted(player.getProfile().getName()));
            }
            return;
        }

        // Keep the authoritative context used for server-side execution and
        // history. The old port accepted packets but discarded this state.
        setContext(player, context);

        if (context.isBuildClientType()) {
            Effortless.LOGGER.debug("Received BUILD_CLIENT request from %s".formatted(player.getProfile().getName()));
            return;
        }

        var server = getEntrance().getServerManager().getRunningServer();
        if (server == null) {
            Effortless.LOGGER.warn("Ignoring context from %s because no server is running.".formatted(player.getProfile().getName()));
            return;
        }

        if (context.isBuildType()) {
            Effortless.LOGGER.debug("Received BUILD request from %s".formatted(player.getProfile().getName()));
            var result = new BatchBuildSession(getEntrance(), player, context).commit();
            logOperationFailures("build", player, result);
            // Do not let failed interactions hide the last real build behind
            // a zero-effect history entry.  They have no inverse operation.
            if (result.getAffectedBlockCount() > 0) {
                getOperationResultStack(player).push(result);
            }
            getEntrance().getChannel().sendPacket(PlayerBuildTooltipPacket.build(result), player);
            return;
        }

        if (context.isPreviewType() && context.isVolumeInBounds()) {
            var result = new BatchBuildSession(getEntrance(), player, context).commit();
            getEntrance().getChannel().sendPacket(PlayerBuildTooltipPacket.preview(result), player);
            sendRefinedStorageSnapshot(player, result);
        }

        for (var otherPlayer : server.getPlayerList().getPlayers()) {
            if (otherPlayer.getId().equals(player.getId()) || otherPlayer.getPosition().distance(player.getPosition()) > 128) {
                continue;
            }
            Effortless.LOGGER.debug("Received PREVIEW request from %s".formatted(player.getProfile().getName()));
            getEntrance().getChannel().sendPacket(PlayerBuildPacket.by(player, context), otherPlayer);
//            getEntrance().getChannel().sendPacket(PlayerBuildTooltipPacket.build(result), player);
        }
    }

    @Override
    public OperationResultStack getOperationResultStack(Player player) {
        return undoRedoStacks.computeIfAbsent(player.getId(), uuid -> new OperationResultStack());
    }

    @Override
    public void undo(Player player) {
        Effortless.LOGGER.debug("Received undo request from %s".formatted(player.getProfile().getName()));
        var stack = getOperationResultStack(player);
        try {
            var result = stack.undo();
            logOperationFailures("undo", player, result);
            var context = result.getOperation().getContext();

            getEntrance().getChannel().sendPacket(PlayerBuildTooltipPacket.undo(result), player);
            var countText = Text.text("[").append(String.valueOf(stack.undoSize())).append("/").append(String.valueOf(stack.redoSize())).append("]").withStyle(ChatFormatting.WHITE);
            var buildStateText = Text.text("[").append(context.buildState().getDisplayName(context.buildMode())).append("]").withStyle(switch (context.buildState()) {
                case IDLE -> ChatFormatting.RESET;
                case BREAK_BLOCK -> ChatFormatting.RED;
                case PLACE_BLOCK -> ChatFormatting.WHITE;
                case INTERACT_BLOCK -> ChatFormatting.YELLOW;
                case COPY_STRUCTURE -> ChatFormatting.GREEN;
                case PASTE_STRUCTURE -> ChatFormatting.WHITE;
            }).withStyle(ChatFormatting.GOLD);
            var affectedText = Text.text("[").append(String.valueOf(result.getAffectedBlockCount())).append("]").withStyle(ChatFormatting.AQUA);
            player.sendMessage(Effortless.getMessage(countText.append(" ").append(Text.translate("effortless.message.history.server.undo", buildStateText, affectedText))));
        } catch (EmptyStackException e) {
            getEntrance().getChannel().sendPacket(PlayerBuildTooltipPacket.nothingToUndo(), player);
            var countText = Text.text("[").append(String.valueOf(stack.undoSize())).append("/").append(String.valueOf(stack.redoSize())).append("]").withStyle(ChatFormatting.WHITE);
            player.sendMessage(Effortless.getMessage(countText.append(" ").append(Text.translate("effortless.history.nothing_to_undo"))));
        }
    }

    @Override
    public void redo(Player player) {
        Effortless.LOGGER.debug("Received redo request from %s".formatted(player.getProfile().getName()));
        var stack = getOperationResultStack(player);
        try {
            var result = stack.redo();
            logOperationFailures("redo", player, result);
            var context = result.getOperation().getContext();

            getEntrance().getChannel().sendPacket(PlayerBuildTooltipPacket.redo(result), player);
            var countText = Text.text("[").append(String.valueOf(stack.undoSize())).append("/").append(String.valueOf(stack.redoSize())).append("]").withStyle(ChatFormatting.WHITE);
            var buildStateText = Text.text("[").append(context.buildState().getDisplayName(context.buildMode())).append("]").withStyle(switch (context.buildState()) {
                case IDLE -> ChatFormatting.RESET;
                case BREAK_BLOCK -> ChatFormatting.RED;
                case PLACE_BLOCK -> ChatFormatting.WHITE;
                case INTERACT_BLOCK -> ChatFormatting.YELLOW;
                case COPY_STRUCTURE -> ChatFormatting.GREEN;
                case PASTE_STRUCTURE -> ChatFormatting.WHITE;
            }).withStyle(ChatFormatting.GOLD);
            var affectedText = Text.text("[").append(String.valueOf(result.getAffectedBlockCount())).append("]").withStyle(ChatFormatting.AQUA);
            player.sendMessage(Effortless.getMessage(countText.append(" ").append(Text.translate("effortless.message.history.server.redo", buildStateText, affectedText))));
        } catch (EmptyStackException e) {
            getEntrance().getChannel().sendPacket(PlayerBuildTooltipPacket.nothingToRedo(), player);
            var countText = Text.text("[").append(String.valueOf(stack.undoSize())).append("/").append(String.valueOf(stack.redoSize())).append("]").withStyle(ChatFormatting.WHITE);
            player.sendMessage(Effortless.getMessage(countText.append(" ").append(Text.translate("effortless.history.nothing_to_redo"))));
        }
    }

    private void onPlayerChangeWorld(Player player, World origin, World destination) {
    }

    private void onPlayerRespawn(Player oldPlayer, Player newPlayer, boolean alive) {
    }

    private void onPlayerLoggedIn(Player player) {
    }

    private void onPlayerLoggedOut(Player player) {
    }

    private void onServerStarted(Server server) {
        resetAll();
    }

    private void onServerStopped(Server server) {
        resetAll();
    }

    private boolean checkPermission(Player player, Context context) {
        var config = getEntrance().getSessionManager().getLastSessionConfig().getByPlayer(player);
        return context != null && context.configs() != null
                && Objects.equals(context.configs().constraintConfig(), config)
                && context.hasPermission()
                && context.isVolumeInBounds();
    }

    private void logOperationFailures(String action, Player player, OperationResult result) {
        if (result instanceof BatchOperationResult batch) {
            batch.getResults().forEach(child -> logOperationFailures(action, player, child));
            return;
        }
        if (result instanceof BlockOperationResult block && block.result().fail()) {
            Effortless.LOGGER.warn("{} failed for {} at {}: {}", action,
                    player.getProfile().getName(), block.getOperation().getBlockPosition(), block.result());
        }
    }

    private void sendRefinedStorageSnapshot(Player player, OperationResult result) {
        var requestedItems = new HashSet<dev.huskuraft.universal.api.core.Item>();
        result.getTooltip().itemSummary().entrySet().stream()
                .filter(entry -> entry.getKey() == dev.huskuraft.effortless.building.operation.ItemSummary.BLOCKS_PLACED
                        || entry.getKey() == dev.huskuraft.effortless.building.operation.ItemSummary.BLOCKS_ITEMS_INSUFFICIENT)
                .flatMap(entry -> entry.getValue().stream())
                .filter(stack -> stack != null && !stack.isEmpty())
                .map(dev.huskuraft.universal.api.core.ItemStack::getItem)
                .forEach(requestedItems::add);
        var snapshot = Storage.refinedStorageSnapshot(player, requestedItems);
        getEntrance().getChannel().sendPacket(
                new PlayerMaterialSnapshotPacket(player.getId(), result.getTooltip().context().id(), snapshot), player);
    }

}



