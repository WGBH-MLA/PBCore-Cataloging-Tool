package digitalbedrock.software.pbcore.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class LanguageManagerTest {

    @ParameterizedTest
    @EnumSource(I18nKey.class)
    void i18nKey_getString_expectedStringReturned(I18nKey key) {

        LanguageManager languageManager = LanguageManager.INSTANCE;
        var expectedString = languageManager.getBundle().getString(key.getKey());
        assertEquals(expectedString, languageManager.getString(key));
    }
}
