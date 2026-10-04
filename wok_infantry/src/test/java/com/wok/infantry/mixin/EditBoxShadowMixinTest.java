package com.wok.infantry.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.components.EditBox;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins {@link EditBoxShadowMixin} to the EditBox of the Minecraft build the core compiles
 * against, without starting a client: every redirect target is parsed with Mixin's own selector
 * parser and matched against the real {@code renderWidget} bytecode, and every handler has the
 * exact signature Mixin requires. A Forge or Minecraft update that moves these calls fails here
 * instead of silently losing the tactical text-field look in game.
 */
class EditBoxShadowMixinTest {
    private static final String GUI_GRAPHICS = "net/minecraft/client/gui/GuiGraphics";
    private static final String RENDER_WIDGET_DESC = "(Lnet/minecraft/client/gui/GuiGraphics;IIF)V";
    /** Expected number of call sites per redirect handler in EditBox.renderWidget (1.20.1). */
    private static final Map<String, Integer> EXPECTED_SITES = Map.of(
            "wokInfantry$skipVanillaFrame", 2,
            "wokInfantry$drawSequence", 2,
            "wokInfantry$drawComponent", 1,
            "wokInfantry$drawString", 2);
    /** The GuiGraphics method each redirect must hit in development names. */
    private static final Map<String, String> EXPECTED_NAMES = Map.of(
            "wokInfantry$skipVanillaFrame", "fill",
            "wokInfantry$drawSequence", "drawString",
            "wokInfantry$drawComponent", "drawString",
            "wokInfantry$drawString", "drawString");

    @Test
    void mixinTargetsEditBoxWithoutRemappingLikeTheOtherCoreMixins() throws IOException {
        // @Mixin has CLASS retention, so read it from the bytecode as Mixin itself does.
        ClassNode mixin = classNode(EditBoxShadowMixin.class.getName().replace('.', '/') + ".class",
                EditBoxShadowMixin.class.getClassLoader());
        AnnotationNode annotation = mixin.invisibleAnnotations == null ? null
                : mixin.invisibleAnnotations.stream()
                .filter(node -> node.desc.equals(Type.getDescriptor(Mixin.class)))
                .findFirst().orElse(null);
        assertNotNull(annotation, "@Mixin missing");
        Map<String, Object> values = new HashMap<>();
        for (int index = 0; index + 1 < annotation.values.size(); index += 2) {
            values.put((String) annotation.values.get(index), annotation.values.get(index + 1));
        }
        assertEquals(List.of(Type.getType(EditBox.class)), values.get("value"));
        assertEquals(Boolean.FALSE, values.get("remap"),
                "no refmap is shipped; names are listed explicitly");
    }

    @Test
    void everyRedirectHitsExactlyTheExpectedRenderWidgetCalls() throws IOException {
        MethodNode renderWidget = renderWidget();
        List<Method> handlers = redirectHandlers();
        assertEquals(EXPECTED_SITES.keySet(), names(handlers));
        for (Method handler : handlers) {
            Redirect redirect = handler.getAnnotation(Redirect.class);
            assertTrue(Arrays.asList(redirect.method()).contains("renderWidget"),
                    () -> handler.getName() + " must name the development method");
            assertTrue(Arrays.asList(redirect.method()).contains("m_87963_"),
                    () -> handler.getName() + " must name the SRG method used in production");
            assertFalse(redirect.remap());
            assertEquals(0, redirect.require(), "cosmetic redirects never crash the game");

            MemberInfo target = MemberInfo.parse(redirect.at().target(), null);
            target.validate();
            assertEquals(GUI_GRAPHICS, target.getOwner());
            assertNull(target.getName(),
                    "descriptor-only target: production keeps class names but renames methods");
            List<MethodInsnNode> hits = new ArrayList<>();
            for (AbstractInsnNode insn : renderWidget.instructions) {
                if (insn instanceof MethodInsnNode call
                        && target.matches(call.owner, call.name, call.desc).isMatch()) {
                    hits.add(call);
                }
            }
            assertEquals(EXPECTED_SITES.get(handler.getName()), hits.size(),
                    () -> handler.getName() + " call sites changed: " + hits.size());
            for (MethodInsnNode hit : hits) {
                assertEquals(Opcodes.INVOKEVIRTUAL, hit.getOpcode());
                assertEquals(EXPECTED_NAMES.get(handler.getName()), hit.name,
                        () -> handler.getName() + " would redirect " + hit.name + hit.desc);
            }
            assertHandlerSignature(handler, Type.getMethodType(target.getDesc()));
        }
    }

    @Test
    void renderWidgetHasNoOtherShadowedTextCalls() throws IOException {
        // Every 5-argument drawString (they all draw with a shadow) is covered by a redirect.
        int shadowed = 0;
        for (AbstractInsnNode insn : renderWidget().instructions) {
            if (insn instanceof MethodInsnNode call && call.owner.equals(GUI_GRAPHICS)
                    && call.name.equals("drawString") && call.desc.endsWith("III)I")) {
                shadowed++;
            }
        }
        assertEquals(5, shadowed);
    }

    @Test
    void mixinIsRegisteredClientOnly() throws IOException {
        JsonObject config;
        try (InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("wok_infantry.mixins.json")) {
            assertNotNull(stream, "missing wok_infantry.mixins.json");
            config = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
        assertTrue(names(config.getAsJsonArray("client")).contains("EditBoxShadowMixin"));
        assertFalse(names(config.getAsJsonArray("mixins")).contains("EditBoxShadowMixin"),
                "EditBox only exists on the client");
        assertEquals("com.wok.infantry.mixin", config.get("package").getAsString());
    }

    private static void assertHandlerSignature(Method handler, Type target) {
        assertTrue(Modifier.isPrivate(handler.getModifiers()), handler.getName());
        assertFalse(Modifier.isStatic(handler.getModifiers()), "needs the EditBox instance");
        Type[] targetArgs = target.getArgumentTypes();
        Class<?>[] params = handler.getParameterTypes();
        assertEquals(targetArgs.length + 1, params.length, handler.getName());
        assertEquals(GUI_GRAPHICS, Type.getInternalName(params[0]), "receiver first");
        for (int index = 0; index < targetArgs.length; index++) {
            assertEquals(targetArgs[index], Type.getType(params[index + 1]),
                    handler.getName() + " parameter " + index);
        }
        assertEquals(target.getReturnType(), Type.getType(handler.getReturnType()),
                handler.getName() + " return type");
    }

    private static ClassNode classNode(String resource, ClassLoader loader) throws IOException {
        ClassNode node = new ClassNode();
        try (InputStream stream = loader.getResourceAsStream(resource)) {
            assertNotNull(stream, () -> resource + " not on the test classpath");
            new ClassReader(stream).accept(node, 0);
        }
        return node;
    }

    private static MethodNode renderWidget() throws IOException {
        ClassNode node = classNode("net/minecraft/client/gui/components/EditBox.class",
                EditBox.class.getClassLoader());
        return node.methods.stream()
                .filter(method -> method.name.equals("renderWidget")
                        && method.desc.equals(RENDER_WIDGET_DESC))
                .findFirst().orElseThrow(() -> new AssertionError("EditBox.renderWidget missing"));
    }

    private static List<Method> redirectHandlers() {
        return Arrays.stream(EditBoxShadowMixin.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Redirect.class))
                .toList();
    }

    private static Set<String> names(List<Method> methods) {
        Set<String> names = new HashSet<>();
        methods.forEach(method -> names.add(method.getName()));
        return names;
    }

    private static Set<String> names(JsonArray array) {
        Set<String> names = new HashSet<>();
        if (array != null) {
            for (JsonElement element : array) {
                names.add(element.getAsString());
            }
        }
        return names;
    }
}
