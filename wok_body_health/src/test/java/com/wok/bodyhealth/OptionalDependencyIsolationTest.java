package com.wok.bodyhealth;

import com.wok.bodyhealth.health.BodyPart;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The module must load without TaCZ, SBW or TAA: only the TaCZ compat package and the TaCZ mixin
 * may link against TaCZ types, SBW and TAA are reached by reflection only, and ordinary code
 * never links against a mixin class. Checks the compiled type references, not strings.
 */
final class OptionalDependencyIsolationTest {
    private static final String ROOT = "com/wok/bodyhealth/";

    @Test
    void optionalModTypesStayInTheirGatedPackages() throws IOException, URISyntaxException {
        Path classes = Path.of(BodyPart.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        assumeTrue(Files.isDirectory(classes.resolve(ROOT)), "compiled main classes directory");

        List<String> problems = new ArrayList<>();
        int allowedTaczLinks = 0;
        try (Stream<Path> files = Files.walk(classes)) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".class"))::iterator) {
                String name = classes.relativize(file).toString().replace('\\', '/');
                if (!name.startsWith(ROOT)) {
                    continue;
                }
                boolean taczAllowed = name.startsWith(ROOT + "compat/tacz/") || name.startsWith(ROOT + "mixin/tacz/");
                boolean mixinPackage = name.startsWith(ROOT + "mixin/");
                for (String type : referencedTypes(file)) {
                    if (type.startsWith("com/tacz/") && taczAllowed) {
                        allowedTaczLinks++;
                    } else if (type.startsWith("com/tacz/")) {
                        problems.add(name + " links TaCZ type " + type);
                    } else if (type.startsWith(ROOT + "compat/tacz/") && !taczAllowed) {
                        problems.add(name + " links TaCZ-dependent class " + type);
                    } else if (type.startsWith("com/atsuishio/") || type.startsWith("com/locknar/")) {
                        problems.add(name + " links optional mod type " + type);
                    } else if (type.startsWith(ROOT + "mixin/") && !mixinPackage) {
                        problems.add(name + " links mixin class " + type);
                    }
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
        // The scanner must actually see type references: the TaCZ compat classes do link TaCZ.
        assertTrue(allowedTaczLinks > 0, "no TaCZ references found in compat/tacz; scanner broken?");
    }

    private static TreeSet<String> referencedTypes(Path file) throws IOException {
        TreeSet<String> types = new TreeSet<>();
        Remapper collector = new Remapper(Opcodes.ASM9) {
            @Override
            public String map(String internalName) {
                types.add(internalName);
                return internalName;
            }
        };
        new ClassReader(Files.readAllBytes(file)).accept(new ClassRemapper(new ClassWriter(0), collector), 0);
        return types;
    }
}
