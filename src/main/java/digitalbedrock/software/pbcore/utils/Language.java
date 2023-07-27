package digitalbedrock.software.pbcore.utils;

import java.util.Locale;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Language {

    EN(Locale.US, I18nKey.ENGLISH, "/structure/structure_en.json"),
    ES(Locale.forLanguageTag("es"), I18nKey.SPANISH, "/structure/structure_es.json");

    private final Locale locale;
    private final I18nKey displayKey;
    private final String structureFile;
}
