package com.wok.infantry.client.ui.probe;

import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rule vectors of {@link UiLayoutReport}, mirroring the preview's {@code check()} cases
 * ({@code ui-preview/kit/mcgui.js}) plus the WOK步战 baseline rules.
 */
class UiLayoutReportTest {
    private static final UiLayoutReport.Options ZH = UiLayoutReport.Options.of("zh_cn");
    private static final UiLayoutReport.Options EN = UiLayoutReport.Options.of("en_us");

    @AfterEach
    void resetProbe() {
        UiLayoutProbe.disable();
    }

    private static UiLayoutFrame frame() {
        // 320×240 at GUI 3: one GUI pixel is three physical pixels.
        return new UiLayoutFrame(320, 240, 3.0D, 1);
    }

    private static UiLayoutFrame.Rect rect(float l, float t, float r, float b) {
        return new UiLayoutFrame.Rect(l, t, r, b);
    }

    private static void text(UiLayoutFrame frame, String text, float l, float t, float r) {
        frame.addText(text, rect(l, t, r, t + 8), 1.0F, false, false, text, false);
    }

    private static List<String> rules(UiLayoutFrame frame, UiLayoutReport.Options options) {
        return UiLayoutReport.check(frame, options).stream().map(v -> v.rule().id()).toList();
    }

    @Test
    void textInsideItsBoxAndTheScreenPasses() {
        UiLayoutFrame frame = frame();
        frame.beginBox("header", rect(3, 3, 317, 19), true);
        text(frame, "WOK // 组件规范", 14, 6, 90);
        frame.endBox();
        assertEquals(List.of(), rules(frame, ZH));
    }

    @Test
    void textLeavingItsBoxOrTheScreenIsReportedOnce() {
        UiLayoutFrame frame = frame();
        frame.beginBox("footer", rect(3, 225, 317, 237), true);
        text(frame, "页脚文字越过了右边", 250, 227, 330);
        frame.endBox();
        assertEquals(List.of("text-offscreen"), rules(frame, ZH),
                "off-screen text is not additionally reported as overflowing its box");

        UiLayoutFrame inside = frame();
        inside.beginBox("footer", rect(3, 225, 317, 237), true);
        text(inside, "回执", 300, 227, 320);
        inside.endBox();
        assertEquals(List.of("text-overflow"), rules(inside, ZH));
    }

    @Test
    void clippedTextIsExemptLikeThePreview() {
        UiLayoutFrame frame = frame();
        frame.beginBox("list", rect(10, 10, 100, 60), true);
        frame.pushClip(rect(10, 10, 100, 60));
        text(frame, "被裁剪的长行会在滚动区里继续", 12, 52, 200);
        frame.popClip();
        frame.endBox();
        assertEquals(List.of(), rules(frame, ZH));
    }

    @Test
    void boxesMustStayInsideTheirParentAndTheScreen() {
        UiLayoutFrame frame = frame();
        frame.beginBox("panel", rect(10, 10, 100, 100), true);
        frame.addBox("child", rect(50, 50, 120, 90), true);
        frame.endBox();
        frame.addBox("offscreen", rect(300, 10, 330, 20), true);
        assertEquals(List.of("box-outside", "box-outside"), rules(frame, ZH));
    }

    @Test
    void onlySolidSiblingsMayNotOverlap() {
        UiLayoutFrame frame = frame();
        frame.addBox("header", rect(3, 3, 317, 19), true);
        frame.addBox("panel", rect(3, 18, 317, 60), true);
        assertEquals(List.of("box-overlap"), rules(frame, ZH));

        UiLayoutFrame touching = frame();
        touching.addBox("header", rect(3, 3, 317, 19), true);
        touching.addBox("panel", rect(3, 19, 317, 60), true);
        touching.addBox("modal", rect(40, 10, 280, 200), false);
        assertEquals(List.of(), rules(touching, ZH),
                "touching edges and non-solid layers are fine");

        UiLayoutFrame nested = frame();
        nested.beginBox("a", rect(0, 0, 100, 100), true);
        nested.addBox("a1", rect(0, 0, 60, 60), true);
        nested.endBox();
        nested.beginBox("b", rect(100, 0, 200, 100), true);
        nested.addBox("b1", rect(100, 0, 160, 60), true);
        nested.endBox();
        assertEquals(List.of(), rules(nested, ZH), "boxes of different parents are not siblings");
    }

    @Test
    void negativeAndUnbalancedBoxesAreReported() {
        UiLayoutFrame frame = frame();
        frame.addBox("negative", rect(50, 50, 40, 60), true);
        frame.beginBox("never-closed", rect(0, 0, 10, 10), true);
        List<String> rules = rules(frame, ZH);
        assertTrue(rules.contains("box-negative"));
        assertTrue(rules.contains("unbalanced-boxes"));
    }

    @Test
    void textMayNotShrinkBelowItsLayoutScale() {
        UiLayoutFrame frame = frame();
        frame.addText("small", rect(10, 10, 20, 14), 0.5F, false, false, "small", false);
        assertEquals(List.of("text-scaled-down"), rules(frame, EN));

        UiLayoutFrame twice = new UiLayoutFrame(960, 720, 1.0D, 2);
        twice.addText("正常", rect(20, 20, 56, 36), 2.0F, false, false, "正常", false);
        assertEquals(List.of(), rules(twice, ZH), "2x layout drawn at pose scale 2 is 1x of it");
    }

    @Test
    void cjkNeedsTwoPhysicalPixelsPerTextPixel() {
        UiLayoutFrame gui1 = new UiLayoutFrame(960, 720, 1.0D, 1);
        text(gui1, "编制投票", 10, 10, 46);
        text(gui1, "Formation vote", 10, 30, 90);
        assertEquals(List.of("cjk-too-small"), rules(gui1, ZH),
                "960×720 at GUI 1 without the 2x rule draws unreadable 8px unifont");

        UiLayoutFrame minimum2x = new UiLayoutFrame(960, 720, 1.0D, 2);
        minimum2x.addText("编制投票", rect(20, 20, 92, 36), 2.0F, false, false, "编制投票", false);
        assertEquals(List.of(), rules(minimum2x, ZH));
    }

    @Test
    void truncatedTextNeedsAFullTextTooltip() {
        UiLayoutFrame frame = frame();
        frame.addText("千禧年研讨会…", rect(10, 10, 80, 18), 1.0F, true, true,
                "千禧年研讨会机动部队", false);
        assertEquals(List.of("text-truncated-no-tip"), rules(frame, ZH));

        UiLayoutFrame tipped = frame();
        tipped.addText("千禧年研讨会…", rect(10, 10, 80, 18), 1.0F, true, true,
                "千禧年研讨会机动部队", true);
        assertEquals(List.of(), rules(tipped, ZH));
    }

    @Test
    void shortenedControlLabelIsCoveredByAWrappedTooltip() {
        UiLayoutFrame frame = frame();
        String label = "超长按钮文字会省略而不是跑马灯滚动：配置当前兵种装备";
        // A widget draws its label first and reports itself afterwards.
        frame.addText("超长按钮文字…", rect(13, 13, 107, 21), 1.0F, true, true, "", false);
        frame.addControl("kit.button.long", "button", "NORMAL", rect(10, 10, 110, 24), true, true,
                false, true, label, "超长按钮文字会省略而不是跑马灯滚\n动：配置当前兵种装备", "", false);
        assertEquals(List.of(), rules(frame, ZH), "the text inside the key belongs to the key");

        UiLayoutFrame bare = frame();
        bare.addText("超长按钮文字…", rect(13, 13, 107, 21), 1.0F, true, true, "", false);
        bare.addControl("kit.button.long", "button", "NORMAL", rect(10, 10, 110, 24), true, true,
                false, false, label, "", "", false);
        assertEquals(List.of("control-truncated-no-tip"), rules(bare, ZH),
                "a shortened label inside a control is reported once, on the control");

        UiLayoutFrame list = frame();
        list.addControl("list/3", "list-row", "NORMAL", rect(10, 10, 110, 24), true, true,
                false, true, label, "", "", true);
        assertEquals(List.of(), rules(list, ZH), "lists offer the full row text on hover");
    }

    @Test
    void controlLabelsMustStayInsideTheirKey() {
        UiLayoutFrame frame = frame();
        // Centre (36.5) still on the key, right edge (60) past it.
        text(frame, "确认部署出发", 13, 13, 60);
        frame.addControl("text:确认", "button", "NORMAL", rect(10, 10, 40, 24), true, true, false,
                false, "确认部署出发", "", "", false);
        assertEquals(List.of("control-text-overflow"), rules(frame, ZH));
    }

    @Test
    void disabledControlsNeedAReasonUnlessTheyAreTheCurrentEntry() {
        UiLayoutFrame frame = frame();
        frame.addControl("a", "button", "DISABLED", rect(10, 10, 40, 24), false, true, false,
                false, "职业管理", "", "", false);
        frame.addControl("b", "button", "DISABLED", rect(50, 10, 80, 24), false, true, false,
                false, "职业管理", "编制没有这个职业", "编制没有这个职业", false);
        frame.addControl("c", "tab", "CURRENT", rect(90, 10, 120, 24), false, true, false,
                false, "小队", "", "", false);
        frame.addControl("d", "button", "DISABLED", rect(130, 10, 160, 24), false, false, false,
                false, "隐藏", "", "", false);
        List<UiLayoutReport.Violation> violations = UiLayoutReport.check(frame, ZH);
        assertEquals(1, violations.size());
        assertEquals(UiLayoutReport.Rule.CONTROL_DISABLED_NO_REASON, violations.get(0).rule());
        assertEquals("a", violations.get(0).subject());
    }

    @Test
    void controlsMustStayOnScreen() {
        UiLayoutFrame frame = frame();
        frame.addControl("far", "button", "NORMAL", rect(300, 10, 340, 24), true, true, false,
                false, "x", "", "", false);
        assertEquals(List.of("control-offscreen"), rules(frame, ZH));
    }

    @Test
    void englishStatusPlaceholdersAreOnlyViolationsOnAChineseClient() {
        UiLayoutFrame frame = frame();
        text(frame, "F-15EX JDAM READY", 10, 10, 100);
        text(frame, "[SL] Aris_Tendou", 10, 30, 100);
        text(frame, "ALREADY HK416D 90/210", 10, 50, 120);
        assertEquals(List.of("placeholder-text", "placeholder-text"), rules(frame, ZH));
        assertEquals(List.of(), rules(frame, EN));
    }

    @Test
    void coversIgnoresWhitespaceAndCjkIsDetected() {
        assertTrue(UiLayoutReport.covers("a b\nc", "abc"));
        assertTrue(UiLayoutReport.covers("全部", ""));
        assertFalse(UiLayoutReport.covers("", "abc"));
        assertFalse(UiLayoutReport.covers("ab", "abc"));
        assertTrue(UiLayoutReport.containsCjk("编制"));
        assertTrue(UiLayoutReport.containsCjk("Ｇ"));
        assertFalse(UiLayoutReport.containsCjk("Formation 1/2 · …"));
    }

    @Test
    void ownerIsTheControlReportedRightAfterTheText() {
        UiLayoutFrame frame = frame();
        frame.addText("小队", rect(20, 13, 38, 21), 1.0F, true, false, "", false);
        frame.addControl("tabs/squads", "tab", "NORMAL", rect(10, 10, 50, 24), true, true, false,
                false, "小队", "", "", true);
        frame.addControl("tabs", "tabs", "FULL", rect(10, 10, 200, 24), true, true, false, false,
                "", "", "", false);
        // Drawn later, on top of the strip: a tooltip line never belongs to the tab below it.
        frame.addText("小队页：成员与部署", rect(20, 13, 120, 21), 1.0F, false, false, "", false);
        // A section title drawn before an unrelated key does not belong to that key either.
        frame.addText("标题", rect(60, 40, 78, 48), 1.0F, true, false, "", false);
        frame.addControl("key", "button", "NORMAL", rect(10, 60, 50, 74), true, true, false,
                false, "键", "", "", false);
        List<UiLayoutFrame.Text> texts = frame.texts();
        assertEquals("tabs/squads",
                UiLayoutReport.owner(frame.controls(), texts.get(0)).uiId());
        assertNull(UiLayoutReport.owner(frame.controls(), texts.get(1)));
        assertNull(UiLayoutReport.owner(frame.controls(), texts.get(2)));
        assertEquals(List.of(), rules(frame, ZH));
    }

    @Test
    void transformMapsPoseSpaceToGuiCoordinates() {
        Matrix4f pose = new Matrix4f().translate(4.0F, 6.0F, 300.0F).scale(2.0F, 2.0F, 1.0F);
        UiLayoutFrame.Rect rect = UiLayoutFrame.transform(pose, 10, 20, 30, 28);
        assertEquals(rect(24, 46, 64, 62), rect);
        assertEquals(2.0F, UiLayoutFrame.scaleOf(pose));
        assertEquals(rect(1, 2, 3, 4), UiLayoutFrame.transform(null, 3, 4, 1, 2));
    }

    @Test
    void probeIsInertUntilEnabledAndRecordsOnlyItsFrame() {
        Object widget = new Object();
        assertSame(widget, UiLayoutProbe.tag(widget, "kit.button"));
        assertNull(UiLayoutProbe.tagOf(widget), "no tags without a receiver");
        UiLayoutProbe.beginFrame(frame());
        assertFalse(UiLayoutProbe.recording(), "no frame without a receiver");
        UiLayoutProbe.fittedText(null, FormattedCharSequence.EMPTY, "x", 0, 0, 10, true);
        assertNull(UiLayoutProbe.endFrame());

        UiLayoutProbe.enable();
        UiLayoutProbe.tag(widget, "kit.button");
        assertEquals("kit.button", UiLayoutProbe.tagOf(widget));
        UiLayoutFrame target = frame();
        UiLayoutProbe.beginFrame(target);
        assertTrue(UiLayoutProbe.recording());
        UiLayoutProbe.begin(null, "box", 0, 0, 100, 20, true);
        UiLayoutProbe.fittedText(null, Component.literal("长名称…").getVisualOrderText(),
                Component.literal("长名称很长"), 4, 6, 40, true);
        UiLayoutProbe.end(null);
        UiLayoutProbe.clipGui(0, 0, 50, 50);
        UiLayoutProbe.unclip();
        UiLayoutProbe.icon("infantry", "NORMAL", 30, 30, 60, 60, 3.0D);
        assertSame(target, UiLayoutProbe.endFrame());
        assertFalse(UiLayoutProbe.recording());

        assertEquals(1, target.boxes().size());
        assertEquals(1, target.texts().size());
        UiLayoutFrame.Text text = target.texts().get(0);
        assertEquals("长名称…", text.text());
        assertEquals("长名称很长", text.fullText());
        assertTrue(text.truncated());
        assertEquals(0, text.box());
        assertEquals(rect(4, 6, 44, 14), text.rect());
        assertEquals(0, target.unbalanced());
        UiLayoutFrame.Icon icon = target.icons().get(0);
        assertEquals(30, icon.physicalWidth());
        assertEquals(rect(10, 10, 20, 20), icon.rect());

        UiLayoutProbe.disable();
        assertNull(UiLayoutProbe.tagOf(widget), "disabling forgets the tags");
    }

    @Test
    void derivedIdsUseTheTranslationKeyOrTheLabel() {
        assertEquals("key:screen.wok_infantry.confirm.ok",
                UiLayoutProbe.derivedId(Component.translatable("screen.wok_infantry.confirm.ok")));
        assertEquals("text:职业管理", UiLayoutProbe.derivedId(Component.literal("职业管理")));
        assertEquals("text:", UiLayoutProbe.derivedId(null));
    }

    @Test
    void tippedMarksTheTextDrawnAtThatPoint() {
        UiLayoutProbe.enable();
        UiLayoutFrame target = frame();
        UiLayoutProbe.beginFrame(target);
        UiLayoutProbe.fittedText(null, Component.literal("行标题…").getVisualOrderText(),
                "行标题很长很长", 12, 30, 40, true);
        UiLayoutProbe.tipped(null, 12, 30);
        UiLayoutProbe.endFrame();
        assertTrue(target.texts().get(0).tipped());
        assertEquals(List.of(), rules(target, ZH));
    }
}
