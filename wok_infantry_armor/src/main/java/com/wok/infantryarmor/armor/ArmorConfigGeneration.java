package com.wok.infantryarmor.armor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 两个护甲配置文件的加载代数。
 *
 * <p>Forge 的 Loading/Reloading/Unloading 事件可能在 FileWatcher 线程或网络线程上触发，另一个文件也可能还没同步，
 * 所以事件里只把代数加一，不在事件里重建任何缓存；读取方在服务端线程或渲染线程上发现代数变化后自行重建。</p>
 */
public final class ArmorConfigGeneration {

    private static final AtomicInteger GENERATION = new AtomicInteger();

    private ArmorConfigGeneration() {
    }

    public static int current() {
        return GENERATION.get();
    }

    /** 只允许配置事件调用；永不抛异常。 */
    public static int advance() {
        return GENERATION.incrementAndGet();
    }
}
