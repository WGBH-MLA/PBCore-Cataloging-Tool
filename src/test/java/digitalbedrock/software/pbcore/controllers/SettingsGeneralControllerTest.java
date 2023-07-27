package digitalbedrock.software.pbcore.controllers;

import static digitalbedrock.software.pbcore.utils.I18nKey.CHANGE_LANGUAGE_INFO_MESSAGE;
import static org.junit.jupiter.api.Assertions.*;
import static org.testfx.assertions.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import digitalbedrock.software.pbcore.utils.Language;
import digitalbedrock.software.pbcore.utils.LanguageManager;

@ExtendWith(ApplicationExtension.class)
class SettingsGeneralControllerTest {

    public static final String CHANGE_LANGUAGE_BUTTON_ID = "#changeLanguageButton";
    public static final String LANGUAGES_COMBOBOX_ID = "#languagesCombobox";

    private LanguageChangeListener languageChangeListener = Mockito.mock(LanguageChangeListener.class);

    @Start
    private void start(Stage stage) throws IOException {

        LanguageManager.INSTANCE.updateLanguage(Language.EN);

        System.setProperty("user.home", System.getProperty("java.io.tmpdir"));

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings_general.fxml"),
                LanguageManager.INSTANCE.getBundle());
        Node node = loader.load();
        SettingsGeneralController settingsGeneralController = loader.getController();
        settingsGeneralController.setLanguageChangeListener(languageChangeListener);
        final BorderPane borderPane = new BorderPane();
        borderPane.setCenter(node);
        stage.setScene(new Scene(borderPane));
        stage.show();
    }

    @Test
    void englishLanguageSelected_componentLoaded_expectedLanguageSelectedAndApplyButtonDisabled(FxRobot robot) {

        LanguageManager languageManager = LanguageManager.INSTANCE;

        languageManager.updateLanguage(Language.EN);

        var changeLanguageButton = robot.lookup(CHANGE_LANGUAGE_BUTTON_ID).queryButton();
        assertTrue(changeLanguageButton.isDisable());

        assertThat(robot.lookup("#changeLanguageInfoMessageLabel").queryAs(Label.class))
                .hasText(languageManager.getString(CHANGE_LANGUAGE_INFO_MESSAGE));
    }

    @Test
    void englishLanguageSelected_selectToDifferentLanguage_expectedLanguageSelectedAndApplyButtonEnabled(FxRobot robot) {

        LanguageManager languageManager = LanguageManager.INSTANCE;

        languageManager.updateLanguage(Language.EN);

        var combobox = (ComboBox<Language>) robot.lookup(LANGUAGES_COMBOBOX_ID).queryAs(ComboBox.class);
        assertEquals(languageManager.getLocale(), combobox.getValue().getLocale());
        robot.interact(() -> combobox.getSelectionModel().select(Language.ES));

        var changeLanguageButton = robot.lookup(CHANGE_LANGUAGE_BUTTON_ID).queryButton();
        assertFalse(changeLanguageButton.isDisable());
    }

    @Test
    void differentLanguageSelected_apply_languageManagerLanguageUpdated(FxRobot robot) throws TimeoutException {

        LanguageManager languageManager = LanguageManager.INSTANCE;

        languageManager.updateLanguage(Language.EN);

        var combobox = (ComboBox<Language>) robot.lookup(LANGUAGES_COMBOBOX_ID).queryAs(ComboBox.class);
        assertEquals(languageManager.getLocale(), combobox.getValue().getLocale());
        robot.interact(() -> combobox.getSelectionModel().select(Language.ES));

        var changeLanguageButton = robot.lookup(CHANGE_LANGUAGE_BUTTON_ID).queryButton();
        assertFalse(changeLanguageButton.isDisable());

        robot.clickOn(changeLanguageButton);

        WaitForAsyncUtils
                .waitFor(2, TimeUnit.SECONDS, () -> Objects.equals(Language.ES, languageManager.getLanguage()));
        Mockito.verify(languageChangeListener).onLanguageChanged(Language.ES);
    }
}
