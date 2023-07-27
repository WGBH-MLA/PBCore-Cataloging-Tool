package digitalbedrock.software.pbcore.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Utils {

    public static void deleteDirectoryStream(Path path) throws IOException {

        var fileToDelete = path.toFile();
        assertTrue(fileToDelete.setWritable(true));
        assertTrue(fileToDelete.setReadable(true));
        try (var walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(file -> {
                assertTrue(file.setWritable(true));
                assertTrue(file.setReadable(true));
                assertTrue(file.delete());
            });
        }
    }
}
