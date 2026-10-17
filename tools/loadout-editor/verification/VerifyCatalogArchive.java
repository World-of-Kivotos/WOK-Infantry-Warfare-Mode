import com.wok.infantry.configtransfer.CatalogArchive;
import java.nio.file.Files;
import java.nio.file.Path;

/** Offline compatibility check against the compiled production CatalogArchive decoder. */
public final class VerifyCatalogArchive {
    public static void main(String[] args) throws Exception {
        int failures = 0;
        for (String filename : args) {
            try {
                CatalogArchive archive = CatalogArchive.decode(Files.readAllBytes(Path.of(filename)));
                System.out.println("PASS " + filename + " | " + archive.summary().replace('\n', ' '));
            } catch (Exception | LinkageError error) {
                failures++;
                System.out.println("FAIL " + filename + " | " + error.getClass().getSimpleName()
                        + ": " + error.getMessage());
                error.printStackTrace(System.out);
            }
        }
        if (failures > 0) System.exit(1);
    }
}
