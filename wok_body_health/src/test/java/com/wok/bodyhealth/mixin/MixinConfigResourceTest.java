package com.wok.bodyhealth.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wok.bodyhealth.prone.ProneMixinStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Mixin config resource and the config plugin's gating and injection check (spec 16-15). */
final class MixinConfigResourceTest {
    private static final String PACKAGE = "com.wok.bodyhealth.mixin.";
    private static final String TACZ_GHR = "(Lnet/minecraft/world/entity/projectile/Projectile;"
            + "Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)"
            + "Lcom/tacz/guns/entity/EntityKineticBullet$EntityResult;";
    private static final String TACZ_F1 = "(Lnet/minecraft/world/entity/projectile/Projectile;"
            + "Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)"
            + "Lcom/tacz/guns/entity/EntityKineticBullet$EntityResult;";
    private static final String TACZ_FN = "(Lnet/minecraft/world/entity/projectile/Projectile;"
            + "Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/List;";
    private static final String CLIP_DESC =
            "(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/Optional;";
    private static final List<String> STATUS_KEYS =
            List.of("tacz.head", "tacz.wrap", "tacz.scope", "sbw.head", "sbw.scope", "level");

    @AfterEach
    void clearProperties() {
        System.clearProperty(WokBodyHealthMixinPlugin.DISABLE_PROPERTY);
        for (String key : STATUS_KEYS) {
            System.clearProperty("wok.bodyhealth.mixin." + key);
        }
    }

    @Test
    void configIsOptionalAndListsExactlyTheThreeCommonMixins() throws IOException {
        JsonObject json;
        try (InputStream in = getClass().getResourceAsStream("/wok_body_health.mixins.json")) {
            assertNotNull(in, "mixin config resource");
            json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        assertFalse(json.get("required").getAsBoolean());
        assertEquals(0, json.getAsJsonObject("injectors").get("defaultRequire").getAsInt());
        assertEquals("com.wok.bodyhealth.mixin", json.get("package").getAsString());
        assertEquals(WokBodyHealthMixinPlugin.class.getName(), json.get("plugin").getAsString());
        assertEquals("JAVA_17", json.get("compatibilityLevel").getAsString());

        JsonArray mixins = json.getAsJsonArray("mixins");
        List<String> names = new ArrayList<>();
        for (JsonElement element : mixins) {
            names.add(element.getAsString());
        }
        assertEquals(List.of("LevelGetEntitiesMixin", "sbw.SbwProjectileEntityMixin", "tacz.TaczEntityUtilMixin"),
                names);
        // "server" would skip integrated servers; "client" has no business here; no refmap is shipped.
        assertFalse(json.has("client"));
        assertFalse(json.has("server"));
        assertFalse(json.has("refmap"));
    }

    @Test
    void eachMixinIsGatedOnItsGunMod() {
        boolean[] states = {false, true};
        for (boolean tacz : states) {
            for (boolean sbw : states) {
                assertEquals(tacz, WokBodyHealthMixinPlugin.decide(PACKAGE + "tacz.TaczEntityUtilMixin", tacz, sbw));
                assertEquals(sbw, WokBodyHealthMixinPlugin.decide(PACKAGE + "sbw.SbwProjectileEntityMixin", tacz, sbw));
                assertEquals(tacz || sbw,
                        WokBodyHealthMixinPlugin.decide(PACKAGE + "LevelGetEntitiesMixin", tacz, sbw));
                assertFalse(WokBodyHealthMixinPlugin.decide(PACKAGE + "SomethingElseMixin", tacz, sbw));
            }
        }
    }

    @Test
    void emergencySwitchAppliesNothing() {
        System.setProperty(WokBodyHealthMixinPlugin.DISABLE_PROPERTY, "true");
        WokBodyHealthMixinPlugin plugin = new WokBodyHealthMixinPlugin();
        assertFalse(plugin.shouldApplyMixin(WokBodyHealthMixinPlugin.LEVEL_TARGET, PACKAGE + "LevelGetEntitiesMixin"));
        assertFalse(plugin.shouldApplyMixin(WokBodyHealthMixinPlugin.TACZ_TARGET,
                PACKAGE + "tacz.TaczEntityUtilMixin"));
    }

    @Test
    void callsFindsRenamedHandlersOnlyInTheNamedMethod() {
        ClassNode node = new ClassNode();
        node.methods.add(method("getHitResult", TACZ_GHR,
                call("com/tacz/guns/util/EntityUtil", "handler$abc000$wokBodyHealth$proneHead", "()V")));

        assertTrue(WokBodyHealthMixinPlugin.calls(node, "getHitResult", TACZ_GHR, "wokBodyHealth$proneHead"));
        assertFalse(WokBodyHealthMixinPlugin.calls(node, "getHitResult", TACZ_GHR, "wokBodyHealth$scopeEnter"));
        assertFalse(WokBodyHealthMixinPlugin.calls(node, "getHitResult", TACZ_F1, "wokBodyHealth$proneHead"));
        assertFalse(WokBodyHealthMixinPlugin.calls(node, "findEntityOnPath", TACZ_GHR, "wokBodyHealth$proneHead"));
    }

    @Test
    void wrapCountsOnceItsHandlerIsMergedNextToTheClipCall() {
        // MixinExtras rewrites the clip call only after the plugin's post-apply hook.
        ClassNode pending = new ClassNode();
        pending.methods.add(method("getHitResult", TACZ_GHR,
                call("net/minecraft/world/phys/AABB", "m_82371_", CLIP_DESC)));
        pending.methods.add(method("wrapOperation$abc000$wokBodyHealth$clipOrForcedMiss", "()V"));
        assertTrue(WokBodyHealthMixinPlugin.wrapPrepared(pending));

        ClassNode applied = new ClassNode();
        applied.methods.add(method("getHitResult", TACZ_GHR,
                call("com/tacz/guns/util/EntityUtil", "wrapOperation$abc000$wokBodyHealth$clipOrForcedMiss", "()V")));
        assertTrue(WokBodyHealthMixinPlugin.wrapPrepared(applied));

        ClassNode noHandler = new ClassNode();
        noHandler.methods.add(method("getHitResult", TACZ_GHR,
                call("net/minecraft/world/phys/AABB", "m_82371_", CLIP_DESC)));
        assertFalse(WokBodyHealthMixinPlugin.wrapPrepared(noHandler));

        ClassNode noClip = new ClassNode();
        noClip.methods.add(method("getHitResult", TACZ_GHR));
        noClip.methods.add(method("wrapOperation$abc000$wokBodyHealth$clipOrForcedMiss", "()V"));
        assertFalse(WokBodyHealthMixinPlugin.wrapPrepared(noClip));
    }

    @Test
    void postApplyPublishesTheInjectionStatus() {
        ClassNode node = new ClassNode();
        node.methods.add(method("getHitResult", TACZ_GHR,
                call("com/tacz/guns/util/EntityUtil", "handler$abc000$wokBodyHealth$proneHead", "()V"),
                call("net/minecraft/world/phys/AABB", "m_82371_", CLIP_DESC)));
        node.methods.add(method("findEntityOnPath", TACZ_F1,
                call("com/tacz/guns/util/EntityUtil", "handler$abc001$wokBodyHealth$scopeEnter", "()V")));
        node.methods.add(method("findEntitiesOnPath", TACZ_FN));
        node.methods.add(method("wrapOperation$abc002$wokBodyHealth$clipOrForcedMiss", "()V"));

        new WokBodyHealthMixinPlugin().postApply(WokBodyHealthMixinPlugin.TACZ_TARGET, node,
                PACKAGE + "tacz.TaczEntityUtilMixin", null);

        assertTrue(ProneMixinStatus.taczHead());
        assertTrue(ProneMixinStatus.taczWrap());
        // findEntitiesOnPath has no scope callback, so the scope pair counts as missing.
        assertFalse(ProneMixinStatus.taczScope());
        assertFalse(ProneMixinStatus.sbwHead());
    }

    private static MethodNode method(String name, String desc, MethodInsnNode... calls) {
        MethodNode method = new MethodNode(Opcodes.ACC_STATIC, name, desc, null, null);
        for (MethodInsnNode call : calls) {
            method.instructions.add(call);
        }
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        return method;
    }

    private static MethodInsnNode call(String owner, String name, String desc) {
        return new MethodInsnNode(Opcodes.INVOKESTATIC, owner, name, desc, false);
    }
}
