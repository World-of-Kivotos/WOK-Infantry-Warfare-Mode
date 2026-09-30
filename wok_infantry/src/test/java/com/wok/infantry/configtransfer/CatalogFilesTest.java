package com.wok.infantry.configtransfer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CatalogFilesTest {
    @TempDir Path root;

    @Test void exportReadListAndDuplicateNameDoNotOverwrite() throws Exception {
        var files = new CatalogFiles(root);
        byte[] bytes = CatalogArchiveTest.fixture().encode();
        assertEquals("学院军.json", files.export("学院军", bytes));
        assertArrayEquals(bytes, files.read("学院军.json"));
        assertEquals(java.util.List.of("学院军.json"), files.list());
        assertThrows(IOException.class, () -> files.export("学院军", new byte[]{0}));
        assertArrayEquals(bytes, files.read("学院军"));
    }

    @Test void rejectsPathsReservedWindowsNamesAndOversizedFiles() throws Exception {
        for (String name : new String[]{"../escape", "..", "C:\\test", "a/b", "a\\b", "CON", "LPT1.txt", "a.", "", "a".repeat(65), "\uD801\uDC00".repeat(33)}) {
            assertThrows(IllegalArgumentException.class, () -> CatalogFiles.fileName(name), name);
        }
        var files = new CatalogFiles(root);
        Files.createDirectories(files.directory());
        try (var stream = Files.newOutputStream(files.directory().resolve("large.json"))) {
            stream.write(new byte[CatalogArchive.MAX_BYTES + 1]);
        }
        assertThrows(IOException.class, () -> files.read("large"));
    }

    @Test void successfulImportReplacesBothFilesAndLeavesPlayerRecordsUntouched() throws Exception {
        Files.writeString(root.resolve("player_loadouts.json"), "private-player-records");
        var archive = CatalogArchiveTest.fixture();
        new CatalogFiles(root).replace(archive);
        assertEquals(CatalogArchive.GSON.toJson(archive.formations()), Files.readString(root.resolve("formations.json")));
        assertEquals(CatalogArchive.GSON.toJson(archive.loadouts()), Files.readString(root.resolve("loadouts.json")));
        assertEquals("private-player-records", Files.readString(root.resolve("player_loadouts.json")));
        assertFalse(Files.exists(root.resolve(".catalog-import/ready")));
    }

    @Test void midWriteFailureRestoresExactOriginalBytesForBothFiles() throws Exception {
        Files.writeString(root.resolve("formations.json"), "original formations\r\n");
        Files.writeString(root.resolve("loadouts.json"), "original loadouts\r\n");
        var files = new CatalogFiles(root);
        assertThrows(IOException.class, () -> files.replace(CatalogArchiveTest.fixture(),
                (index, path) -> { throw new IOException("disk failure"); }));
        assertEquals("original formations\r\n", Files.readString(root.resolve("formations.json")));
        assertEquals("original loadouts\r\n", Files.readString(root.resolve("loadouts.json")));
        assertFalse(Files.exists(root.resolve(".catalog-import/ready")));
    }

    @Test void restartRecoversInterruptedTransactionIncludingOriginallyAbsentFiles() throws Exception {
        Files.writeString(root.resolve("formations.json"), "original");
        assertThrows(SimulatedCrash.class, () -> new CatalogFiles(root).replace(CatalogArchiveTest.fixture(),
                (index, path) -> { throw new SimulatedCrash(); }));
        assertTrue(Files.exists(root.resolve(".catalog-import/ready")));
        new CatalogFiles(root).recover();
        assertEquals("original", Files.readString(root.resolve("formations.json")));
        assertFalse(Files.exists(root.resolve("loadouts.json")));
        new CatalogFiles(root).recover();
        assertEquals("original", Files.readString(root.resolve("formations.json")));
    }

    @Test void junctionAboveConfigDirectoryDoesNotBlockLoadingOrImport() throws Exception {
        // PCL version isolation reaches the game directory through versions\<name> junctions.
        Path real = Files.createDirectories(root.resolve("real"));
        Path linked = root.resolve("linked");
        link(linked, real);
        Path config = Files.createDirectories(linked.resolve("config").resolve("wok_infantry"));
        var files = new CatalogFiles(config);
        files.recover();
        files.replace(CatalogArchiveTest.fixture());
        assertTrue(Files.exists(real.resolve("config/wok_infantry/loadouts.json")));
        assertEquals("备份.json", files.export("备份", new byte[]{1}));
    }

    @Test void linkInsideConfigDirectoryIsStillRejected() throws Exception {
        Path outside = Files.createDirectories(root.resolve("outside"));
        Path config = Files.createDirectories(root.resolve("config"));
        link(config.resolve(".catalog-import"), outside);
        assertThrows(IOException.class, () -> new CatalogFiles(config).recover());
        link(config.resolve("catalogs"), outside);
        assertThrows(IOException.class, () -> new CatalogFiles(config).list());
    }

    private static void link(Path link, Path target) throws Exception {
        if (System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("win")) {
            Process process = new ProcessBuilder("cmd", "/c", "mklink", "/J", link.toString(), target.toString())
                    .redirectErrorStream(true).start();
            process.getInputStream().readAllBytes();
            org.junit.jupiter.api.Assumptions.assumeTrue(process.waitFor() == 0, "无法创建目录联接");
        } else {
            Files.createSymbolicLink(link, target);
        }
    }

    @Test void digestDetectsAnyFileChange() {
        assertNotEquals(CatalogFiles.digest(new byte[]{1}), CatalogFiles.digest(new byte[]{2}));
        assertEquals(64, CatalogFiles.digest(new byte[0]).length());
    }

    private static class SimulatedCrash extends Error {}
}
