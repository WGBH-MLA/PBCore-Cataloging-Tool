package digitalbedrock.software.pbcore.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

import digitalbedrock.software.pbcore.core.models.entity.IPBCore;
import digitalbedrock.software.pbcore.utils.I18nKey;
import digitalbedrock.software.pbcore.utils.LanguageManager;

public class SearchFieldItemController {

    @FXML
    private Label lblElementType;
    @FXML
    private Label lblElementName;

    public void bind(IPBCore ipbCore) {

        if (ipbCore.isAttribute()) {
            lblElementType.getStyleClass().remove("elem");
            lblElementType.getStyleClass().add("attr");
            lblElementType.setText(LanguageManager.INSTANCE.getString(I18nKey.ATTRIBUTE_INITIAL));
        }
        else {
            lblElementType.getStyleClass().remove("attr");
            lblElementType.getStyleClass().add("elem");
            lblElementType.setText(LanguageManager.INSTANCE.getString(I18nKey.ELEMENT_INITIAL));
        }
        lblElementName.setText(ipbCore.getScreenName());
    }
}
