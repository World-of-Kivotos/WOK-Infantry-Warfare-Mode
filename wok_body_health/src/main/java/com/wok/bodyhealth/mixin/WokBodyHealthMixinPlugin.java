package com.wok.bodyhealth.mixin;

import net.minecraftforge.fml.loading.LoadingModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Applies the prone-hitbox mixins only when their gun mod is installed, and afterwards checks
 * that each injection really landed. The result goes into {@code wok.bodyhealth.mixin.<key>}
 * system properties for {@code ProneMixinStatus}; this class must not touch TaCZ, SBW or any
 * regular class of this mod, which live in another class loader at this point.
 */
public final class WokBodyHealthMixinPlugin implements IMixinConfigPlugin {
    /** Emergency switch: {@code -Dwok.bodyhealth.disableProneMixins=true} applies none of them. */
    static final String DISABLE_PROPERTY = "wok.bodyhealth.disableProneMixins";
    static final String TACZ_TARGET = "com.tacz.guns.util.EntityUtil";
    static final String SBW_TARGET = "com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity";
    static final String LEVEL_TARGET = "net.minecraft.world.level.Level";

    private static final Logger LOG = LogManager.getLogger("wok_body_health/mixin");
    private static final String STATUS_PREFIX = "wok.bodyhealth.mixin.";
    private static final String TACZ_GHR = "(Lnet/minecraft/world/entity/projectile/Projectile;"
            + "Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)"
            + "Lcom/tacz/guns/entity/EntityKineticBullet$EntityResult;";
    private static final String TACZ_F1 = "(Lnet/minecraft/world/entity/projectile/Projectile;"
            + "Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)"
            + "Lcom/tacz/guns/entity/EntityKineticBullet$EntityResult;";
    private static final String TACZ_FN = "(Lnet/minecraft/world/entity/projectile/Projectile;"
            + "Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/List;";
    private static final String SBW_GHR = "(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;"
            + "Lnet/minecraft/world/phys/Vec3;)Lcom/atsuishio/superbwarfare/world/phys/EntityResult;";
    private static final String SBW_FN =
            "(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/List;";
    private static final String LVL = "(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;"
            + "Ljava/util/function/Predicate;)Ljava/util/List;";
    private static final String AABB_OWNER = "net/minecraft/world/phys/AABB";
    private static final String CLIP_DESC =
            "(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/Optional;";
    private static final String WRAP_HANDLER = "wokBodyHealth$clipOrForcedMiss";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (Boolean.getBoolean(DISABLE_PROPERTY)) {
            return false;
        }
        return decide(mixinClassName, isLoaded("tacz"), isLoaded("superbwarfare"));
    }

    static boolean decide(String mixin, boolean tacz, boolean sbw) {
        if (mixin.endsWith(".tacz.TaczEntityUtilMixin")) {
            return tacz;
        }
        if (mixin.endsWith(".sbw.SbwProjectileEntityMixin")) {
            return sbw;
        }
        if (mixin.endsWith(".LevelGetEntitiesMixin")) {
            return tacz || sbw;
        }
        return false;
    }

    private static boolean isLoaded(String modId) {
        LoadingModList mods = LoadingModList.get();
        return mods != null && mods.getModFileById(modId) != null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass,
                         String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass,
                          String mixinClassName, IMixinInfo mixinInfo) {
        try {
            switch (targetClassName.replace('/', '.')) {
                case TACZ_TARGET -> {
                    mark("tacz.head", calls(targetClass, "getHitResult", TACZ_GHR, "wokBodyHealth$proneHead"));
                    mark("tacz.wrap", wrapPrepared(targetClass));
                    mark("tacz.scope", calls(targetClass, "findEntityOnPath", TACZ_F1, "wokBodyHealth$scopeEnter")
                            && calls(targetClass, "findEntitiesOnPath", TACZ_FN, "wokBodyHealth$scopeEnter"));
                }
                case SBW_TARGET -> {
                    mark("sbw.head", calls(targetClass, "getHitResult", SBW_GHR, "wokBodyHealth$proneHead"));
                    mark("sbw.scope", calls(targetClass, "findEntitiesOnPath", SBW_FN, "wokBodyHealth$scopeEnter"));
                }
                case LEVEL_TARGET -> mark("level",
                        calls(targetClass, "m_6249_", LVL, "wokBodyHealth$addProneCandidates")
                                || calls(targetClass, "getEntities", LVL, "wokBodyHealth$addProneCandidates"));
                default -> {
                }
            }
        } catch (Throwable throwable) {
            LOG.warn("Injection check failed for {}", targetClassName, throwable);
        }
    }

    /**
     * MixinExtras applies {@code @WrapOperation} after this post-apply hook, so the clip call may
     * not be rewritten yet. Accept either the finished wrap, or the merged handler together with
     * the AABB.clip call it will wrap. ProneTaczHooks additionally waits until the wrapper has run.
     */
    static boolean wrapPrepared(ClassNode node) {
        if (calls(node, "getHitResult", TACZ_GHR, WRAP_HANDLER)) {
            return true;
        }
        boolean handlerMerged = false;
        for (MethodNode method : node.methods) {
            if (method.name.contains(WRAP_HANDLER)) {
                handlerMerged = true;
                break;
            }
        }
        return handlerMerged && (invokes(node, "getHitResult", TACZ_GHR, AABB_OWNER, "m_82371_", CLIP_DESC)
                || invokes(node, "getHitResult", TACZ_GHR, AABB_OWNER, "clip", CLIP_DESC));
    }

    private static void mark(String key, boolean ok) {
        System.setProperty(STATUS_PREFIX + key, Boolean.toString(ok));
        if (ok) {
            LOG.info("Injected {}", key);
        } else {
            LOG.warn("Injection MISSING: {}", key);
        }
    }

    /**
     * Whether method {@code name desc} calls a method whose name contains {@code fragment}. Mixin
     * renames handlers ({@code handler$...$<name>}, {@code wrapOperation$...$<name>}) but keeps
     * the original name as a suffix.
     */
    static boolean calls(ClassNode node, String name, String desc, String fragment) {
        MethodNode method = find(node, name, desc);
        if (method == null) {
            return false;
        }
        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof MethodInsnNode call && call.name.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private static boolean invokes(ClassNode node, String name, String desc,
                                   String owner, String callName, String callDesc) {
        MethodNode method = find(node, name, desc);
        if (method == null) {
            return false;
        }
        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof MethodInsnNode call && call.owner.equals(owner)
                    && call.name.equals(callName) && call.desc.equals(callDesc)) {
                return true;
            }
        }
        return false;
    }

    private static MethodNode find(ClassNode node, String name, String desc) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) {
                return method;
            }
        }
        return null;
    }
}
