package digitalbedrock.software.pbcore.utils;

import static digitalbedrock.software.pbcore.utils.Utils.deleteDirectoryStream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class RegistryTest {

    public static final String PB_CORE_FOLDER = System.getProperty("java.io.tmpdir") + File.separator + ".pbcore";

    @BeforeAll
    static void beforeAll() throws IOException {

        File pbCoreFolder = new File(PB_CORE_FOLDER);
        assertTrue(pbCoreFolder.mkdir());
    }

    @AfterAll
    static void afterAll() throws IOException {

        deleteDirectoryStream(Path.of(PB_CORE_FOLDER));
    }

    @ParameterizedTest
    @EnumSource(Language.class)
    void settingsWithLanguage_instantiateRegistry_LanguageManagerLanguageIsExpectedOne(Language language)
            throws URISyntaxException, IOException {

        System.setProperty("user.home", System.getProperty("java.io.tmpdir").replaceAll("/$", ""));

        var fileToBeCopied = new File(
                RegistryTest.class.getResource(String.format("/settings_%s.json", language)).toURI());
        Files
                .copy(fileToBeCopied.toPath(), Path.of(PB_CORE_FOLDER + File.separator + "settings.json"),
                      StandardCopyOption.REPLACE_EXISTING);

        var registry = new Registry();
        assertEquals(language, LanguageManager.INSTANCE.getLanguage());
        assertEquals(registry.getSettings().getLanguage(), LanguageManager.INSTANCE.getLanguage());
    }
}
