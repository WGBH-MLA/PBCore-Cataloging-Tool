package digitalbedrock.software.pbcore.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.testfx.assertions.api.Assertions.assertThat;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import digitalbedrock.software.pbcore.core.models.entity.PBCoreElement;
import digitalbedrock.software.pbcore.utils.I18nKey;
import digitalbedrock.software.pbcore.utils.Language;
import digitalbedrock.software.pbcore.utils.LanguageManager;

@ExtendWith(ApplicationExtension.class)
class ElementSelectorControllerTest {

    public static final String LBL_DESCRIPTION_ID = "#lblDescription";
    public static final String LBL_OPTIONAL_ID = "#lblOptional";
    public static final String LBL_REPEATABLE_ID = "#lblRepeatable";
    public static final String LBL_ELEMENT_ALREADY_ADDED_ID = "#lblElementAlreadyAdded";
    public static final String BTN_ADD_ID = "#btnAdd";
    public static final String BTN_ADD_AND_CLOSE_ID = "#btnAddAndClose";
    private ElementSelectorController elementSelectorController;

    @Start
    private void start(Stage stage) throws IOException {

        LanguageManager.INSTANCE.updateLanguage(Language.EN);

        System.setProperty("user.home", System.getProperty("java.io.tmpdir"));

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/element_selector.fxml"),
                LanguageManager.INSTANCE.getBundle());
        Node node = loader.load();
        elementSelectorController = loader.getController();
        final BorderPane borderPane = new BorderPane();
        borderPane.setCenter(node);
        stage.setScene(new Scene(borderPane));
        stage.show();
    }

    @Test
    void existingElementAndNullParent_updateElementUI_expectedComponentsUpdated(FxRobot robot) {

        PBCoreElement element = PBCoreElement
                .builder()
                .fullPath("pbcoreDescriptionDocument")
                .description("description")
                .build();
        robot.interact(() -> {
            elementSelectorController.setPbCoreElement(element);
            elementSelectorController.updateElementUI(element, null);
        });

        LanguageManager languageManager = LanguageManager.INSTANCE;

        assertThat(robot.lookup(LBL_DESCRIPTION_ID).queryLabeled()).hasText(element.getDescription());
        assertThat(robot.lookup(LBL_OPTIONAL_ID).queryText()).hasText(languageManager.getString(I18nKey.OPTIONAL));
        assertThat(robot.lookup(LBL_REPEATABLE_ID).queryText()).hasText("");
        assertThat(robot.lookup(LBL_ELEMENT_ALREADY_ADDED_ID).queryLabeled())
                .hasText(languageManager.getString(I18nKey.ELEMENT_ALREADY_ADDED));

        assertFalse(robot.lookup(BTN_ADD_ID).queryButton().isDisabled());
        assertFalse(robot.lookup(BTN_ADD_AND_CLOSE_ID).queryButton().isDisabled());
        assertFalse(robot.lookup(LBL_ELEMENT_ALREADY_ADDED_ID).queryLabeled().isVisible());
    }

}
