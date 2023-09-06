package digitalbedrock.software.pbcore.controllers;

import static digitalbedrock.software.pbcore.controllers.DocumentController.CSV_EXTENSION;
import static digitalbedrock.software.pbcore.controllers.SettingsGeneralControllerTest.CHANGE_LANGUAGE_BUTTON_ID;
import static digitalbedrock.software.pbcore.controllers.SettingsGeneralControllerTest.LANGUAGES_COMBOBOX_ID;
import static digitalbedrock.software.pbcore.utils.RegistryTest.PB_CORE_FOLDER;
import static digitalbedrock.software.pbcore.utils.Utils.deleteDirectoryStream;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import digitalbedrock.software.pbcore.MainApp;
import digitalbedrock.software.pbcore.core.models.entity.PBCoreElement;
import digitalbedrock.software.pbcore.core.models.entity.PBCoreStructure;
import digitalbedrock.software.pbcore.listeners.MenuListener;
import digitalbedrock.software.pbcore.utils.I18nKey;
import digitalbedrock.software.pbcore.utils.Language;
import digitalbedrock.software.pbcore.utils.LanguageManager;
import digitalbedrock.software.pbcore.utils.Registry;

@ExtendWith(ApplicationExtension.class)
class MainControllerTest {

    public static final String SEARCH_CANCEL_BUTTON = "#cancelButton";
    public static final String SEARCH_TERM_TEXT_FIELD = "#textFieldTerm";
    public static final String TITLE_LABEL_ID = "#titleLabel";
    public static final String SPINNER_LAYER_ID = "#spinnerLayer";
    public static final String MENU_BAR_ID = "#menuBar";
    public static final String ADD_ATTRIBUTE_BUTTON_ID = "#addAttributeButton";
    public static final String BTN_CANCEL_ID = "#btnCancel";
    public static final String BTN_ADD_AND_CLOSE_ID = "#btnAddAndClose";
    public static final String ADD_BUTTON_ID = "#addButton";
    private static Registry registry;
    private MainApp mainApp;
    private MainController controller;

    @Start
    private void start(Stage stage) throws IOException, URISyntaxException {

        var fileToBeCopied = new File(
                MainControllerTest.class.getResource("/settings_with_folder_defined.json").toURI());
        Files
                .copy(fileToBeCopied.toPath(), Path.of(PB_CORE_FOLDER + File.separator + "settings.json"),
                      StandardCopyOption.REPLACE_EXISTING);

        stage.setScene(null);
        LanguageManager.INSTANCE.updateLanguage(Language.EN);

        mainApp = new MainApp();
        mainApp.start(stage);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"),
                LanguageManager.INSTANCE.getBundle());
        Node node = loader.load();
        final BorderPane borderPane = new BorderPane();
        controller = loader.getController();
        controller.setRegistry(mainApp.getRegistry());
        controller.setMenuListener(mainApp);
        borderPane.setTop(controller.createMenu());
        borderPane.setCenter(node);
        stage.setScene(new Scene(borderPane));
        stage.show();
    }

    @BeforeAll
    static void beforeAll() throws URISyntaxException, IOException {

        System.setProperty("user.home", System.getProperty("java.io.tmpdir").replaceAll("/$", ""));

        File pbCoreFolder = new File(PB_CORE_FOLDER);
        assertTrue(pbCoreFolder.mkdir());
    }

    @AfterAll
    static void afterAll() throws IOException {

        deleteDirectoryStream(Path.of(PB_CORE_FOLDER));
    }

    @Test
    void componentLoaded_menuBarLoaded_generalSettingsMenuItemInExpectedMenuPosition(FxRobot robot) {

        var menuBar = robot.lookup(MENU_BAR_ID).queryAs(MenuBar.class);
        assertNotNull(menuBar);

        Menu settingsMenu = menuBar.getMenus().get(2);
        assertEquals(LanguageManager.INSTANCE.getString(I18nKey.SETTINGS), settingsMenu.getText());
        var generalSettingsMenu = settingsMenu.getItems().get(2);
        assertEquals(LanguageManager.INSTANCE.getString(I18nKey.GENERAL_SETTINGS), generalSettingsMenu.getText());
    }

    @Test
    void menuBarLoaded_selectGeneralSettingsMenuItem_generalSettingsTabShown(FxRobot robot) throws TimeoutException {

        WaitForAsyncUtils
                .waitFor(2, TimeUnit.SECONDS,
                         () -> robot.lookup(LanguageManager.INSTANCE.getString(I18nKey.GENERAL)).tryQuery().isEmpty());

        robot.press(KeyCode.SHIFT, KeyCode.CONTROL, KeyCode.G);
        robot.release(KeyCode.SHIFT, KeyCode.CONTROL, KeyCode.G);

        assertTrue(robot.lookup(LanguageManager.INSTANCE.getString(I18nKey.GENERAL)).tryQuery().isPresent());
    }

    @Test
    void mainControllerLoaded_getFileChooser_fileChooserContainsExpectedExtension(FxRobot robot)
            throws TimeoutException {

        var fileChooser = controller.getFileChooser();

        ObservableList<FileChooser.ExtensionFilter> extensionFilters = fileChooser.getExtensionFilters();
        assertEquals(1, extensionFilters.size());
        var filter = extensionFilters.get(0);
        assertEquals(LanguageManager.INSTANCE.getString(I18nKey.CSV_FILES), filter.getDescription());
        assertEquals(Collections.singletonList(CSV_EXTENSION), filter.getExtensions());

    }

    @Test
    void settingsDialogOpened_changeLanguage_languageChanged(FxRobot robot) throws TimeoutException {

        LanguageManager languageManager = LanguageManager.INSTANCE;

        assertFalse(robot
                .lookup(languageManager.getBundle(Language.ES.getLocale()).getString(I18nKey.FILE.getKey()))
                .tryQuery()
                .isPresent());

        robot.press(KeyCode.SHIFT, KeyCode.CONTROL, KeyCode.G);
        robot.release(KeyCode.SHIFT, KeyCode.CONTROL, KeyCode.G);

        var combobox = (ComboBox<Language>) robot.lookup(LANGUAGES_COMBOBOX_ID).queryAs(ComboBox.class);
        assertEquals(languageManager.getLocale(), combobox.getValue().getLocale());
        robot.interact(() -> combobox.getSelectionModel().select(Language.ES));

        robot.clickOn(robot.lookup(CHANGE_LANGUAGE_BUTTON_ID).queryButton());

        WaitForAsyncUtils
                .waitFor(2, TimeUnit.SECONDS, () -> Objects.equals(Language.ES, languageManager.getLanguage()));

        var menuBar = robot.lookup(MENU_BAR_ID).queryAs(MenuBar.class);
        assertNotNull(menuBar);

        assertEquals(languageManager.getBundle(Language.ES.getLocale()).getString(I18nKey.FILE.getKey()),
                     menuBar.getMenus().get(0).getText());
    }

    @Test
    void pageLoaded_selectNewSearchOption_dialogOpened(FxRobot robot) throws TimeoutException {

        robot.press(KeyCode.CONTROL, KeyCode.F);
        robot.release(KeyCode.CONTROL, KeyCode.F);

        WaitForAsyncUtils
                .waitFor(2, TimeUnit.SECONDS, () -> robot.lookup(SEARCH_TERM_TEXT_FIELD).tryQuery().isPresent());
        robot.clickOn(robot.lookup(SEARCH_CANCEL_BUTTON).queryButton());
    }

    @Test
    void pageLoaded_selectNewElementOption_dialogOpened(FxRobot robot) throws TimeoutException {

        robot.interact(() -> {
            controller.onShown();
            controller.menuOptionSelected(MenuListener.MenuOption.NEW_DESCRIPTION_DOCUMENT);
        });
        WaitForAsyncUtils
                .waitFor(15, TimeUnit.SECONDS, () -> !robot.lookup(SPINNER_LAYER_ID).queryAs(Node.class).isVisible());

        robot.clickOn(robot.lookup(ADD_BUTTON_ID).queryButton());
        robot.clickOn(robot.lookup(BTN_ADD_AND_CLOSE_ID).queryButton());
    }

    @Test
    void pageLoaded_selectNewAttributeOption_dialogOpened(FxRobot robot) throws TimeoutException {

        robot.interact(() -> {
            controller.onShown();
            controller.menuOptionSelected(MenuListener.MenuOption.NEW_DESCRIPTION_DOCUMENT);
        });
        WaitForAsyncUtils
                .waitFor(15, TimeUnit.SECONDS, () -> !robot.lookup(SPINNER_LAYER_ID).queryAs(Node.class).isVisible());

        robot.clickOn(robot.lookup(ADD_ATTRIBUTE_BUTTON_ID).queryButton());
        robot.clickOn(robot.lookup(BTN_CANCEL_ID).queryButton());
    }

    @Test
    void settingsDialogOpened_changeLanguage_pbcoreElementsAppearInSelectedLanguage(FxRobot robot)
            throws TimeoutException, IOException {

        robot.press(KeyCode.CONTROL, KeyCode.N);
        robot.release(KeyCode.CONTROL, KeyCode.N);

        LanguageManager languageManager = LanguageManager.INSTANCE;

        assertFalse(robot
                .lookup(languageManager.getBundle(Language.ES.getLocale()).getString(I18nKey.FILE.getKey()))
                .tryQuery()
                .isPresent());

        robot.press(KeyCode.SHIFT, KeyCode.CONTROL, KeyCode.G);
        robot.release(KeyCode.SHIFT, KeyCode.CONTROL, KeyCode.G);

        var combobox = (ComboBox<Language>) robot.lookup(LANGUAGES_COMBOBOX_ID).queryAs(ComboBox.class);
        assertEquals(languageManager.getLocale(), combobox.getValue().getLocale());
        robot.interact(() -> combobox.getSelectionModel().select(Language.ES));

        robot.clickOn(robot.lookup(CHANGE_LANGUAGE_BUTTON_ID).queryButton());

        WaitForAsyncUtils
                .waitFor(2, TimeUnit.SECONDS, () -> Objects.equals(Language.ES, languageManager.getLanguage()));

        var menuBar = robot.lookup(MENU_BAR_ID).queryAs(MenuBar.class);
        assertNotNull(menuBar);

        ObjectMapper mapper = new ObjectMapper();
        List<PBCoreElement> elements = mapper
                .readValue(PBCoreStructure.class.getResource(languageManager.getLanguage().getStructureFile()),
                           new TypeReference<>() {
                           });

        WaitForAsyncUtils
                .waitFor(15, TimeUnit.SECONDS, () -> !robot.lookup(SPINNER_LAYER_ID).queryAs(Node.class).isVisible());

        WaitForAsyncUtils
                .waitFor(15, TimeUnit.SECONDS,
                         () -> robot
                                 .lookup(TITLE_LABEL_ID)
                                 .lookup(elements.get(0).getScreenName())
                                 .tryQuery()
                                 .isPresent());

        assertTrue(robot.lookup(TITLE_LABEL_ID).lookup(elements.get(0).getScreenName()).queryLabeled().isVisible());
    }

}
