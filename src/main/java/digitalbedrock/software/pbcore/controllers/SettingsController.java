package digitalbedrock.software.pbcore.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.scene.control.MenuBar;

public class SettingsController extends AbsController {

    private LanguageChangeListener listener;
    @FXML
    SettingsGeneralController settingsGeneralController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    @Override
    public MenuBar createMenu() {

        return new MenuBar();
    }

    public void setLanguageChangeListener(LanguageChangeListener listener) {

        settingsGeneralController.setLanguageChangeListener(listener);
    }
}
