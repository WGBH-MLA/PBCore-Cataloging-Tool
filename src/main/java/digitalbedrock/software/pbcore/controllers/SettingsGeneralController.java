package digitalbedrock.software.pbcore.controllers;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

import digitalbedrock.software.pbcore.utils.Language;
import digitalbedrock.software.pbcore.utils.LanguageManager;

public class SettingsGeneralController implements Initializable {

    private LanguageChangeListener listener;

    public static final Logger LOGGER = Logger.getLogger(SettingsGeneralController.class.getName());
    @FXML
    private Button changeLanguageButton;

    @FXML
    private ComboBox<Language> languagesCombobox;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        LanguageManager languageManager = LanguageManager.INSTANCE;

        languagesCombobox.getItems().addAll(Language.values());

        var selectedLocale = languagesCombobox
                .getItems()
                .stream()
                .filter(language -> Objects.equals(language.getLocale(), languageManager.getLocale()))
                .findFirst()
                .orElseGet(() -> languagesCombobox.getItems().get(0));

        languagesCombobox.getSelectionModel().select(selectedLocale);

        Language initialLocale = languagesCombobox.getSelectionModel().getSelectedItem();

        languagesCombobox
                .getSelectionModel()
                .selectedItemProperty()
                .addListener((observableValue, oldValue, newValue) -> changeLanguageButton
                        .setDisable(Objects.equals(initialLocale, newValue)));

        languagesCombobox.converterProperty().set(new StringConverter<Language>() {

            @Override
            public String toString(Language language) {

                return languageManager.getString(language.getDisplayKey());
            }

            @Override
            public Language fromString(String value) {

                return languagesCombobox
                        .getItems()
                        .stream()
                        .filter(language -> languageManager.getString(language.getDisplayKey()).equals(value))
                        .findFirst()
                        .orElse(null);
            }
        });
    }

    public void changeLanguage() {

        Language language = languagesCombobox.getSelectionModel().getSelectedItem();
        LanguageManager.INSTANCE.updateLanguage(language);
        listener.onLanguageChanged(language);
    }

    public void setLanguageChangeListener(LanguageChangeListener listener) {

        this.listener = listener;
    }
}
