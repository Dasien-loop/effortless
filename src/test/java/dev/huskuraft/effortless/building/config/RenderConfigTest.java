package dev.huskuraft.effortless.building.config;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RenderConfigTest {

    @Test
    public void includesRenderLimitButExcludesLargerVolumes() {
        var config = config(1024);
        assertTrue(config.canRenderVolume(1023));
        assertTrue(config.canRenderVolume(1024));
        assertFalse(config.canRenderVolume(1025));
    }

    @Test
    public void zeroRenderLimitSkipsNonEmptySelections() {
        var config = config(0);
        assertTrue(config.canRenderVolume(0));
        assertFalse(config.canRenderVolume(1));
    }

    @Test
    public void changingLimitReevaluatesTheSameVolume() {
        assertFalse(config(512).canRenderVolume(1024));
        assertTrue(config(1024).canRenderVolume(1024));
        assertFalse(config(512).canRenderVolume(1024));
    }

    private static RenderConfig config(int maxVolume) {
        return new RenderConfig(true, true, false, maxVolume, 128);
    }
}
