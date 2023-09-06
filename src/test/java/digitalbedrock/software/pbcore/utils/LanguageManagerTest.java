package digitalbedrock.software.pbcore.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class LanguageManagerTest {

    private static Stream<Arguments> provideColumnsToTestAndExpectedExtension() {

        return Stream
                .of(Arguments.of(Language.ES, I18nKey.PROCESSING, "PROCESANDO"),
                    Arguments.of(Language.ES, I18nKey.SCHEDULED, "PROGRAMADO"),
                    Arguments.of(Language.ES, I18nKey.FINISHED, "-"),
                    Arguments.of(Language.EN, I18nKey.PROCESSING, "PROCESSING"),
                    Arguments.of(Language.EN, I18nKey.SCHEDULED, "SCHEDULED"),
                    Arguments.of(Language.EN, I18nKey.FINISHED, "-"));
    }

    @ParameterizedTest
    @MethodSource("provideColumnsToTestAndExpectedExtension")
    void selectedLanguageAndCrawlingStateKeys_getString_expectedTranslationReturned(Language language, I18nKey i18nKey,
                                                                                    String expectedTranslation) {

        LanguageManager languageManager = LanguageManager.INSTANCE;
        languageManager.updateLanguage(language);
        assertEquals(expectedTranslation, languageManager.getString(i18nKey));
    }

    @ParameterizedTest
    @EnumSource(I18nKey.class)
    void i18nKey_getString_expectedStringReturned(I18nKey key) {

        LanguageManager languageManager = LanguageManager.INSTANCE;
        var expectedString = languageManager.getBundle().getString(key.getKey());
        assertEquals(expectedString, languageManager.getString(key));
    }
}
