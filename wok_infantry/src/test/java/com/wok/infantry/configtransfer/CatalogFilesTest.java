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

    @Test void digestDetectsAnyFileChange() {
        assertNotEquals(CatalogFiles.digest(new byte[]{1}), CatalogFiles.digest(new byte[]{2}));
        assertEquals(64, CatalogFiles.digest(new byte[0]).length());
    }

    private static class SimulatedCrash extends Error {}
}
