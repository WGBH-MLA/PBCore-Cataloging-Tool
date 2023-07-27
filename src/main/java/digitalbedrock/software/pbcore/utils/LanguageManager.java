package digitalbedrock.software.pbcore.utils;

import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;

import lombok.Getter;

public enum LanguageManager {

    INSTANCE;

    @Getter
    private Language language;

    LanguageManager() {

        this.language = Arrays
                .stream(Language.values())
                .filter(lng -> lng.getLocale() == Locale.getDefault())
                .findFirst()
                .orElse(Language.EN);
    }

    public ResourceBundle getBundle() {

        return getBundle(language.getLocale());
    }

    public ResourceBundle getBundle(Locale locale) {

        return ResourceBundle.getBundle("digitalbedrock/software/strings", locale);
    }

    public String getString(I18nKey key) {

        return getBundle().getString(key.getKey());
    }

    public void updateLanguage(Language newValue) {

        this.language = newValue;
    }

    public Locale getLocale() {

        return language.getLocale();
    }
}
