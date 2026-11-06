package dev.huskuraft.effortless.building.operation;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import dev.huskuraft.effortless.building.BuildState;
import dev.huskuraft.effortless.building.BuildType;
import dev.huskuraft.effortless.building.Context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class OperationTooltipTest {

    @Test
    public void pendingUntilServerSummaryArrivesWithoutLocalPreview() {
        var context = context();
        var pending = OperationTooltip.preview(context, null, null);

        assertSame(context, pending.context());
        assertEquals(OperationTooltip.Type.PREVIEW, pending.type());
        assertTrue(pending.itemSummary().isEmpty());
    }

    @Test
    public void retainsServerSummaryOnEveryTickWhenLocalRenderingIsSkipped() {
        var context = context();
        var server = summary(context, ItemSummary.BLOCKS_PLACED);

        // Above the render limit there is no local result. Server refreshes must
        // never alternate with the pending placeholder between replies (#1).
        for (int tick = 0; tick < 40; tick++) {
            assertSame(server, OperationTooltip.preview(context, server, null));
        }
    }

    @Test
    public void serverSummaryOverridesLocalInventoryShortage() {
        var context = context();
        var server = summary(context, ItemSummary.BLOCKS_PLACED);
        var local = summary(context, ItemSummary.BLOCKS_ITEMS_INSUFFICIENT);

        assertSame(server, OperationTooltip.preview(context, server, local));
    }

    @Test
    public void usesLocalSummaryBeforeServerReply() {
        var context = context();
        var local = summary(context, ItemSummary.BLOCKS_PLACED);

        assertSame(local, OperationTooltip.preview(context, null, local));
    }

    @Test
    public void ignoresServerSummaryFromPreviousSelection() {
        var context = context();
        var stale = summary(context(), ItemSummary.BLOCKS_PLACED);
        var local = summary(context, ItemSummary.BLOCKS_ITEMS_INSUFFICIENT);

        assertSame(local, OperationTooltip.preview(context, stale, local));
        assertTrue(OperationTooltip.preview(context, stale, null).itemSummary().isEmpty());
    }

    @Test
    public void ignoresLocalSummaryFromPreviousSelection() {
        var context = context();
        var stale = summary(context(), ItemSummary.BLOCKS_PLACED);

        assertTrue(OperationTooltip.preview(context, null, stale).itemSummary().isEmpty());
    }

    private static OperationTooltip summary(Context context, ItemSummary category) {
        return new OperationTooltip(OperationTooltip.Type.PREVIEW, context, Map.of(category, List.of()));
    }

    private static Context context() {
        // Tooltip selection needs only the ID; no Minecraft services are loaded.
        return new Context(UUID.randomUUID(), BuildState.PLACE_BLOCK, BuildType.PREVIEW,
                Context.Interactions.EMPTY, null, null, null, null, null, null);
    }
}
