package com.wok.infantry.server;

import com.wok.infantry.formation.FormationConfigData;
import com.wok.infantry.formation.FormationLoadoutEditAction;
import com.wok.infantry.formation.FormationLoadoutRuleEditor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormationRepositoryTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void missingFileCreatesTheSafeDefaultCatalog() throws Exception {
        Path path = temporaryDirectory.resolve("config/wok_infantry/formations.json");
        FormationRepository repository = new FormationRepository(path);

        FormationRepository.LoadResult result = repository.load();

        assertTrue(result.success());
        assertTrue(Files.isRegularFile(path));
        assertEquals(2, result.config().factions().size());
        assertTrue(result.config().findFaction("academy").isPresent());
        assertTrue(result.config().findFaction("caesar").isPresent());
    }

    @Test
    void malformedReloadPreservesBothTheActiveCatalogAndOriginalBytes() throws Exception {
        Path path = temporaryDirectory.resolve("formations.json");
        FormationRepository repository = new FormationRepository(path);
        assertTrue(repository.load().success());
        int activeFactionCount = repository.config().factions().size();
        String malformed = "{\"version\":1,\"factions\":[";
        Files.writeString(path, malformed, StandardCharsets.UTF_8);

        FormationRepository.LoadResult result = repository.load();

        assertFalse(result.success());
        assertEquals(activeFactionCount, result.config().factions().size());
        assertEquals(activeFactionCount, repository.config().factions().size());
        assertEquals(malformed, Files.readString(path, StandardCharsets.UTF_8));
    }

    @Test
    void catalogWithoutAnyValidFactionIsRejectedWithoutRewritingIt() throws Exception {
        Path path = temporaryDirectory.resolve("formations.json");
        String emptyCatalog = "{\"version\":1,\"factions\":[]}";
        Files.writeString(path, emptyCatalog, StandardCharsets.UTF_8);
        FormationRepository repository = new FormationRepository(path);

        FormationRepository.LoadResult result = repository.load();

        assertFalse(result.success());
        assertTrue(result.config().factions().isEmpty());
        assertEquals(emptyCatalog, Files.readString(path, StandardCharsets.UTF_8));
    }

    @Test
    void administratorWhitelistEditIsPersistedAndReloadable() {
        Path path = temporaryDirectory.resolve("formations.json");
        FormationRepository repository = new FormationRepository(path);
        FormationConfigData original = repository.load().config();
        FormationConfigData edited = FormationLoadoutRuleEditor.apply(
                original,
                "academy",
                "default",
                "assault",
                "primary",
                "captured_m4a1",
                FormationLoadoutEditAction.INCLUDE_CAPTURED);

        assertTrue(repository.replace(edited));

        FormationRepository reloadedRepository = new FormationRepository(path);
        FormationConfigData reloaded = reloadedRepository.load().config();
        assertEquals(
                java.util.List.of("captured_m4a1"),
                reloaded.findFormation("academy", "default").orElseThrow()
                        .findClass("assault").orElseThrow()
                        .allowedEntriesFor("primary"));
    }
}
