package com.wok.bodyhealth.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reads the compiled mixin classes (their annotations are class-retained) and checks what a
 * production run depends on: remap off everywhere, optional injectors, the selectors verified
 * against the TaCZ 1.1.8 / SBW 0.8.9 bytecode, and the handler names the plugin looks for.
 */
final class MixinAnnotationSanityTest {
    private static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String PSEUDO = "Lorg/spongepowered/asm/mixin/Pseudo;";
    private static final String INJECT = "Lorg/spongepowered/asm/mixin/injection/Inject;";
    private static final String WRAP = "Lcom/llamalad7/mixinextras/injector/wrapoperation/WrapOperation;";

    private static final String TACZ_GHR = "getHitResult(Lnet/minecraft/world/entity/projectile/Projectile;"
            + "Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)"
            + "Lcom/tacz/guns/entity/EntityKineticBullet$EntityResult;";
    private static final String TACZ_F1 = "findEntityOnPath(Lnet/minecraft/world/entity/projectile/Projectile;"
            + "Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)"
            + "Lcom/tacz/guns/entity/EntityKineticBullet$EntityResult;";
    private static final String TACZ_FN = "findEntitiesOnPath(Lnet/minecraft/world/entity/projectile/Projectile;"
            + "Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/List;";
    private static final String SBW_GHR = "getHitResult(Lnet/minecraft/world/entity/Entity;"
            + "Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)"
            + "Lcom/atsuishio/superbwarfare/world/phys/EntityResult;";
    private static final String SBW_F1 = "findEntityOnPath(Lnet/minecraft/world/phys/Vec3;"
            + "Lnet/minecraft/world/phys/Vec3;)Lcom/atsuishio/superbwarfare/world/phys/EntityResult;";
    private static final String SBW_FN =
            "findEntitiesOnPath(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/List;";
    private static final String LVL_ARGS =
            "(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)"
                    + "Ljava/util/List;";
    private static final String CLIP_ARGS =
            "(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/Optional;";

    @Test
    void taczMixinMatchesTheVerifiedTargets() throws IOException {
        ClassNode node = read("com/wok/bodyhealth/mixin/tacz/TaczEntityUtilMixin");
        AnnotationNode mixin = annotation(node.invisibleAnnotations, MIXIN);
        List<?> classes = (List<?>) value(mixin, "value");
        assertEquals(1, classes.size());
        assertEquals("com/tacz/guns/util/EntityUtil", ((Type) classes.get(0)).getInternalName());
        assertEquals(Boolean.FALSE, value(mixin, "remap"));

        Map<String, AnnotationNode> injectors = injectors(node);
        assertEquals(List.of("wokBodyHealth$clipOrForcedMiss", "wokBodyHealth$proneHead",
                "wokBodyHealth$scopeEnter", "wokBodyHealth$scopeExit"), List.copyOf(injectors.keySet()));
        assertEquals(List.of(TACZ_F1, TACZ_FN), value(injectors.get("wokBodyHealth$scopeEnter"), "method"));
        assertEquals(List.of(TACZ_F1, TACZ_FN), value(injectors.get("wokBodyHealth$scopeExit"), "method"));
        assertEquals(List.of(TACZ_GHR), value(injectors.get("wokBodyHealth$proneHead"), "method"));
        assertEquals(Boolean.TRUE, value(injectors.get("wokBodyHealth$proneHead"), "cancellable"));

        AnnotationNode wrap = injectors.get("wokBodyHealth$clipOrForcedMiss");
        assertEquals(WRAP, wrap.desc);
        assertEquals(List.of(TACZ_GHR), value(wrap, "method"));
        List<String> targets = new ArrayList<>();
        for (Object at : (List<?>) value(wrap, "at")) {
            AnnotationNode atNode = (AnnotationNode) at;
            assertEquals("INVOKE", value(atNode, "value"));
            assertEquals(Boolean.FALSE, value(atNode, "remap"));
            targets.add((String) value(atNode, "target"));
        }
        assertEquals(List.of("Lnet/minecraft/world/phys/AABB;m_82371_" + CLIP_ARGS,
                "Lnet/minecraft/world/phys/AABB;clip" + CLIP_ARGS), targets);
    }

    @Test
    void sbwMixinIsPseudoAndMatchesTheVerifiedTargets() throws IOException {
        ClassNode node = read("com/wok/bodyhealth/mixin/sbw/SbwProjectileEntityMixin");
        assertNotNull(annotation(node.invisibleAnnotations, PSEUDO), "@Pseudo");
        AnnotationNode mixin = annotation(node.invisibleAnnotations, MIXIN);
        assertEquals(List.of("com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity"),
                value(mixin, "targets"));
        assertEquals(Boolean.FALSE, value(mixin, "remap"));

        Map<String, AnnotationNode> injectors = injectors(node);
        assertEquals(List.of("wokBodyHealth$proneHead", "wokBodyHealth$scopeEnter", "wokBodyHealth$scopeExit"),
                List.copyOf(injectors.keySet()));
        assertEquals(List.of(SBW_FN, SBW_F1), value(injectors.get("wokBodyHealth$scopeEnter"), "method"));
        assertEquals(List.of(SBW_GHR), value(injectors.get("wokBodyHealth$proneHead"), "method"));
        assertEquals(Boolean.TRUE, value(injectors.get("wokBodyHealth$proneHead"), "cancellable"));
    }

    @Test
    void levelMixinListsOfficialAndSrgNames() throws IOException {
        ClassNode node = read("com/wok/bodyhealth/mixin/LevelGetEntitiesMixin");
        AnnotationNode mixin = annotation(node.invisibleAnnotations, MIXIN);
        List<?> classes = (List<?>) value(mixin, "value");
        assertEquals(1, classes.size());
        assertEquals("net/minecraft/world/level/Level", ((Type) classes.get(0)).getInternalName());
        assertEquals(Boolean.FALSE, value(mixin, "remap"));

        Map<String, AnnotationNode> injectors = injectors(node);
        AnnotationNode inject = injectors.get("wokBodyHealth$addProneCandidates");
        assertNotNull(inject);
        assertEquals(List.of("getEntities" + LVL_ARGS, "m_6249_" + LVL_ARGS), value(inject, "method"));
        assertEquals(Boolean.TRUE, value(inject, "cancellable"));
        AnnotationNode at = (AnnotationNode) ((List<?>) value(inject, "at")).get(0);
        assertEquals("RETURN", value(at, "value"));
    }

    @Test
    void handlersAreOptionalAndNeverRemapped() throws IOException {
        for (String name : List.of("com/wok/bodyhealth/mixin/tacz/TaczEntityUtilMixin",
                "com/wok/bodyhealth/mixin/sbw/SbwProjectileEntityMixin",
                "com/wok/bodyhealth/mixin/LevelGetEntitiesMixin")) {
            Map<String, AnnotationNode> injectors = injectors(read(name));
            assertFalse(injectors.isEmpty(), name);
            for (Map.Entry<String, AnnotationNode> entry : injectors.entrySet()) {
                String where = name + "#" + entry.getKey();
                assertEquals(Boolean.FALSE, value(entry.getValue(), "remap"), where + " remap");
                assertEquals(0, value(entry.getValue(), "require"), where + " require");
                assertTrue(entry.getKey().startsWith("wokBodyHealth$"), where + " handler prefix");
            }
        }
    }

    private static ClassNode read(String internalName) throws IOException {
        try (InputStream in = MixinAnnotationSanityTest.class.getResourceAsStream("/" + internalName + ".class")) {
            assertNotNull(in, internalName);
            ClassNode node = new ClassNode();
            new ClassReader(in).accept(node, ClassReader.SKIP_CODE);
            return node;
        }
    }

    /** Injector annotations by handler name, sorted. */
    private static Map<String, AnnotationNode> injectors(ClassNode node) {
        Map<String, AnnotationNode> out = new TreeMap<>();
        for (MethodNode method : node.methods) {
            // Retention differs between annotations (Inject is runtime-visible), so look in both.
            AnnotationNode inject = annotation(method.visibleAnnotations, INJECT);
            if (inject == null) {
                inject = annotation(method.invisibleAnnotations, INJECT);
            }
            AnnotationNode wrap = annotation(method.visibleAnnotations, WRAP);
            if (wrap == null) {
                wrap = annotation(method.invisibleAnnotations, WRAP);
            }
            if (inject != null || wrap != null) {
                out.put(method.name, inject != null ? inject : wrap);
            }
        }
        return out;
    }

    private static AnnotationNode annotation(List<AnnotationNode> annotations, String desc) {
        if (annotations != null) {
            for (AnnotationNode annotation : annotations) {
                if (annotation.desc.equals(desc)) {
                    return annotation;
                }
            }
        }
        return null;
    }

    /** Annotation member value; enums come back as their constant name. */
    private static Object value(AnnotationNode annotation, String name) {
        assertNotNull(annotation, "annotation for " + name);
        List<Object> values = annotation.values;
        for (int i = 0; values != null && i < values.size(); i += 2) {
            if (values.get(i).equals(name)) {
                Object value = values.get(i + 1);
                return value instanceof String[] enumValue ? enumValue[1] : value;
            }
        }
        return null;
    }
}
