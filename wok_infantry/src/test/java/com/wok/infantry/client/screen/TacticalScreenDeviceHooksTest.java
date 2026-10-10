package com.wok.infantry.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The device hooks of {@link TacticalScreen#render} (0.5.0-beta.4, IMPL_PLAN D6): the backdrop
 * comes first and outside the scope, the scope wraps device, page, modal, glass and tooltip, its
 * effects are the last thing a frame draws, and it is closed before the screen pops its pose, even
 * when the frame throws.
 */
class TacticalScreenDeviceHooksTest {
    private static final class OrderScreen extends TacticalScreen {
        final List<String> log;
        boolean fail;

        OrderScreen(List<String> log) {
            super(Component.literal("order"));
            this.log = log;
        }

        @Override
        protected void initTactical() {
        }

        @Override
        protected void renderTactical(GuiGraphics graphics, int mouseX, int mouseY,
                                      float partialTick) {
            log.add("page");
            if (fail) {
                throw new IllegalStateException("page failed");
            }
        }

        @Override
        protected void renderGlassOverlay(GuiGraphics graphics, float partialTick) {
            log.add("glass");
        }
    }

    /**
     * Records every hook call; the backdrop alpha 0 draws nothing and the screen draws nothing, so
     * the frame runs without a client ({@link TacticalScreen#renderFrame} with a null graphics).
     */
    private static final class Recording implements TacticalScreen.DeviceHooks {
        final List<String> log;
        final PoseStack pose;

        Recording(List<String> log, PoseStack pose) {
            this.log = log;
            this.pose = pose;
        }

        @Override
        public float backdropAlpha(TacticalScreen screen) {
            log.add("backdrop");
            return 0.0F;
        }

        @Override
        public TacticalScreen.DeviceScope beginDevice(GuiGraphics g, TacticalScreen screen) {
            log.add("begin");
            pose.pushPose();
            return new TacticalScreen.DeviceScope() {
                private boolean open = true;

                @Override
                public void drawEffects() {
                    end();
                    log.add("effects");
                }

                @Override
                public void close() {
                    // The screen's own pose is still pushed: more than the root entry is open.
                    log.add(pose.clear() ? "close-after-pop" : "close");
                    end();
                }

                private void end() {
                    if (open) {
                        open = false;
                        pose.popPose();
                    }
                }
            };
        }
    }

    @AfterEach
    void restoreHooks() {
        TacticalScreen.installDeviceHooks(null);
    }

    @Test
    void withoutAnAnimationTheHooksChangeNothing() {
        assertSame(TacticalScreen.NO_HOOKS, TacticalScreen.deviceHooks());
        assertEquals(1.0F, TacticalScreen.NO_HOOKS.backdropAlpha(null));
        assertEquals(1.0F, TacticalScreen.NO_HOOKS.shadowAlpha(null));
        assertSame(TacticalScreen.DeviceScope.NONE, TacticalScreen.NO_HOOKS.beginDevice(null, null));
        TacticalScreen.installDeviceHooks(new TacticalScreen.DeviceHooks() {
        });
        TacticalScreen.installDeviceHooks(null);
        assertSame(TacticalScreen.NO_HOOKS, TacticalScreen.deviceHooks(), "null restores the defaults");
    }

    @Test
    void backdropThenTheScopeAroundPageAndGlassThenEffectsThenClose() {
        List<String> log = new ArrayList<>();
        PoseStack pose = new PoseStack();
        TacticalScreen.installDeviceHooks(new Recording(log, pose));
        new OrderScreen(log).renderFrame(null, pose, 0, 0, 0.0F);

        assertEquals(List.of("backdrop", "begin", "page", "glass", "effects", "close"), log);
        assertTrue(pose.clear(), "every pose pushed in the frame was popped");
    }

    @Test
    void aFrameThatThrowsClosesTheScopeButDrawsNoEffects() {
        List<String> log = new ArrayList<>();
        PoseStack pose = new PoseStack();
        TacticalScreen.installDeviceHooks(new Recording(log, pose));
        OrderScreen screen = new OrderScreen(log);
        screen.fail = true;

        assertThrows(IllegalStateException.class, () -> screen.renderFrame(null, pose, 0, 0, 0.0F));
        assertEquals(List.of("backdrop", "begin", "page", "close"), log);
        assertTrue(pose.clear(), "the scope and the screen both restored the pose");
    }
}
