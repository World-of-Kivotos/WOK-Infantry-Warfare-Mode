package com.wok.infantry.configtransfer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/** Bounded archive files plus a recoverable two-file import transaction. */
public final class CatalogFiles {
    private static final List<String> CONFIG_NAMES = List.of("formations.json", "loadouts.json");
    private final Path root;
    private final Path archives;
    private final Path pending;

    public CatalogFiles(Path root) {
        this.root = root.toAbsolutePath().normalize();
        archives = this.root.resolve("catalogs");
        pending = this.root.resolve(".catalog-import");
    }

    public Path directory() { return archives; }

    public static String fileName(String name) {
        if (name == null) throw new IllegalArgumentException("请输入数据包文件名");
        String stem = name.endsWith(".json") ? name.substring(0, name.length() - 5) : name;
        if (stem.length() > 64 || !stem.matches("[\\p{L}\\p{N}_-][\\p{L}\\p{N}_.-]{0,63}") || stem.endsWith(".")
                || stem.matches("(?i)(con|prn|aux|nul|com[0-9]|lpt[0-9])(?:\\..*)?")) {
            throw new IllegalArgumentException("文件名限 1–64 个字母、汉字、数字、_、-、.，不能包含路径");
        }
        return stem + ".json";
    }

    public List<String> list() throws IOException {
        prepareDirectory(archives);
        try (var paths = Files.list(archives)) {
            return paths.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".json") && validName(name))
                    .sorted(String.CASE_INSENSITIVE_ORDER).toList();
        }
    }

    private static boolean validName(String name) {
        try { return fileName(name).equals(name); }
        catch (IllegalArgumentException ignored) { return false; }
    }

    public byte[] read(String name) throws IOException {
        prepareDirectory(archives);
        return readBounded(archives.resolve(fileName(name)));
    }

    public String export(String name, byte[] contents) throws IOException {
        prepareDirectory(archives);
        Path target = archives.resolve(fileName(name));
        checkNotLink(target);
        if (Files.exists(target)) throw new IOException("同名数据包已存在，请换一个文件名");
        Path staging = Files.createTempFile(archives, ".export-", ".tmp");
        try {
            Files.write(staging, contents);
            // No REPLACE_EXISTING: exporting never destroys a previous backup.
            Files.move(staging, target);
        } finally {
            Files.deleteIfExists(staging);
        }
        return target.getFileName().toString();
    }

    /** Roll back an interrupted import before either repository loads its active configuration. */
    public void recover() throws IOException {
        checkNotLink(pending);
        Path marker = pending.resolve("ready");
        checkNotLink(marker);
        if (!Files.exists(marker)) return;
        for (String name : CONFIG_NAMES) {
            Path target = root.resolve(name);
            checkNotLink(target);
            Path absent = pending.resolve(name + ".absent");
            checkNotLink(absent);
            if (Files.exists(absent)) Files.deleteIfExists(target);
            else atomicWrite(target, readBounded(pending.resolve(name + ".before")));
        }
        Files.delete(marker);
    }

    public void replace(CatalogArchive archive) throws IOException {
        replace(archive, (index, path) -> {});
    }

    // Hook exercises real write failure and interruption recovery without mocking the filesystem.
    void replace(CatalogArchive archive, WriteObserver observer) throws IOException {
        prepareDirectory(root);
        recover();
        prepareDirectory(pending);
        for (String name : CONFIG_NAMES) {
            Path target = root.resolve(name);
            checkNotLink(target);
            Path absent = pending.resolve(name + ".absent");
            checkNotLink(absent);
            Files.deleteIfExists(absent);
            if (Files.exists(target)) atomicWrite(pending.resolve(name + ".before"), readBounded(target));
            else Files.writeString(absent, "absent", StandardOpenOption.CREATE_NEW);
        }
        atomicWrite(pending.resolve("ready"), "rollback".getBytes(StandardCharsets.UTF_8));
        try {
            atomicWrite(root.resolve("formations.json"),
                    CatalogArchive.GSON.toJson(archive.formations()).getBytes(StandardCharsets.UTF_8));
            observer.written(0, root.resolve("formations.json"));
            atomicWrite(root.resolve("loadouts.json"),
                    CatalogArchive.GSON.toJson(archive.loadouts()).getBytes(StandardCharsets.UTF_8));
            observer.written(1, root.resolve("loadouts.json"));
            Files.delete(pending.resolve("ready"));
        } catch (IOException | RuntimeException exception) {
            try { recover(); }
            catch (IOException rollback) {
                exception.addSuppressed(rollback);
                throw new IOException("导入写入及回滚失败；请停止服务并检查 .catalog-import 恢复记录", exception);
            }
            throw new IOException("写入失败，原配置已回滚", exception);
        }
    }

    private static byte[] readBounded(Path path) throws IOException {
        checkNotLink(path);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw new IOException("文件不存在：" + path.getFileName());
        try (var input = Files.newInputStream(path)) {
            byte[] bytes = input.readNBytes(CatalogArchive.MAX_BYTES + 1);
            if (bytes.length > CatalogArchive.MAX_BYTES) throw new IOException("文件超过 8 MiB 上限");
            return bytes;
        }
    }

    private static void atomicWrite(Path target, byte[] contents) throws IOException {
        checkNotLink(target);
        Path staging = Files.createTempFile(target.getParent(), ".catalog-", ".tmp");
        try {
            Files.write(staging, contents);
            try {
                Files.move(staging, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(staging, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(staging); }
    }

    private static void prepareDirectory(Path directory) throws IOException {
        checkNotLink(directory);
        Files.createDirectories(directory);
    }

    private static void checkNotLink(Path path) throws IOException {
        for (Path current = path; current != null; current = current.getParent()) {
            if (Files.isSymbolicLink(current) || Files.exists(current, LinkOption.NOFOLLOW_LINKS)
                    && Files.readAttributes(current, java.nio.file.attribute.BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS).isOther()) {
                throw new IOException("不能通过符号链接或重解析点访问配置文件");
            }
        }
    }

    public static String digest(byte[] contents) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contents)); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }

    @FunctionalInterface
    interface WriteObserver { void written(int index, Path path) throws IOException; }
}
