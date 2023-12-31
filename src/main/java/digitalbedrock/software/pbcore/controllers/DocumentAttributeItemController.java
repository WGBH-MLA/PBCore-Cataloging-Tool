package digitalbedrock.software.pbcore.controllers;

import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

import org.kordamp.ikonli.javafx.FontIcon;

import digitalbedrock.software.pbcore.MainApp;
import digitalbedrock.software.pbcore.components.PBCoreAttributeTreeCell;
import digitalbedrock.software.pbcore.core.models.CVTerm;
import digitalbedrock.software.pbcore.core.models.entity.PBCoreAttribute;
import digitalbedrock.software.pbcore.listeners.CVSelectionListener;
import digitalbedrock.software.pbcore.utils.I18nKey;
import digitalbedrock.software.pbcore.utils.LanguageManager;
import digitalbedrock.software.pbcore.utils.Registry;
import np.com.ngopal.control.AutoFillTextBox;

public class DocumentAttributeItemController {

    @FXML
    private AutoFillTextBox<CVTerm> autoCompleteTF;
    @FXML
    private Label attributeNameLbl;
    @FXML
    private Button removeButton;
    @FXML
    private Button btnSelectCV;
    @FXML
    private FontIcon valueMissingIcon;
    private ChangeListener<String> tChangeListener;
    private DocumentAttributeSelectCVListener documentAttributeSelectCVListener;
    private PBCoreAttribute pbCoreAttribute;

    public DocumentAttributeItemController() {

    }

    public void bind(PBCoreAttribute pbCoreAttribute,
                     PBCoreAttributeTreeCell.AttributeTreeCellListener attributeTreeCellListener,
                     DocumentAttributeSelectCVListener documentAttributeSelectCVListener) {

        this.documentAttributeSelectCVListener = documentAttributeSelectCVListener;
        this.pbCoreAttribute = pbCoreAttribute;
        if (tChangeListener != null) {
            autoCompleteTF.getTextbox().textProperty().removeListener(tChangeListener);
        }
        tChangeListener = (observable, oldValue, newValue) -> pbCoreAttribute.setValue(newValue);
        Registry registry = MainApp.getInstance().getRegistry();
        if (registry.getControlledVocabularies().containsKey(pbCoreAttribute.getName())) {
            registry
                    .getControlledVocabularies()
                    .get(pbCoreAttribute.getName())
                    .getTerms()
                    .forEach(autoCompleteTF::addData);
        }
        autoCompleteTF.getTextbox().setText(pbCoreAttribute.getValue() == null ? "" : pbCoreAttribute.getValue());
        pbCoreAttribute.valueProperty
                .addListener((observable, oldValue, newValue) -> valueMissingIcon
                        .setVisible(pbCoreAttribute.isRequired() && (newValue == null || newValue.trim().isEmpty())));
        attributeNameLbl.setText(pbCoreAttribute.getScreenName());
        attributeNameLbl.setTooltip(new Tooltip(pbCoreAttribute.getScreenName()));
        removeButton.setVisible(!pbCoreAttribute.isRequired() && !pbCoreAttribute.isReadOnly());
        removeButton.setOnAction(event -> {
            if (attributeTreeCellListener != null) {
                attributeTreeCellListener.onRemoveAttribute(pbCoreAttribute);
            }
        });
        autoCompleteTF.setDisable(pbCoreAttribute.isReadOnly());
        autoCompleteTF.getTextbox().textProperty().addListener(tChangeListener);

        valueMissingIcon
                .setVisible(pbCoreAttribute.isRequired()
                        && (pbCoreAttribute.getValue() == null || pbCoreAttribute.getValue().trim().isEmpty()));

        if (valueMissingIcon.isVisible()) {
            Tooltip tooltip = new Tooltip(LanguageManager.INSTANCE.getString(I18nKey.MISSING_VALUE));
            valueMissingIcon.setOnMouseEntered(event -> {
                Point2D p = valueMissingIcon
                        .localToScreen(valueMissingIcon.getLayoutBounds().getMaxX(),
                                       valueMissingIcon.getLayoutBounds().getMaxY());
                tooltip.show(valueMissingIcon, p.getX(), p.getY() + 2);
            });
            valueMissingIcon.setOnMouseExited(event -> tooltip.hide());
        }
        else {
            valueMissingIcon.setOnMouseEntered(null);
            valueMissingIcon.setOnMouseEntered(null);
        }
        btnSelectCV
                .setVisible(MainApp
                        .getInstance()
                        .getRegistry()
                        .getControlledVocabularies()
                        .containsKey(pbCoreAttribute.getName()));
    }

    public void selectCV(ActionEvent actionEvent) {

        if (documentAttributeSelectCVListener != null) {
            documentAttributeSelectCVListener.selectCV(pbCoreAttribute, (key, cvTerm, attr) -> {
                if (attr && pbCoreAttribute.getName().equals(key)) {
                    pbCoreAttribute.setValue(cvTerm.getTerm());
                    autoCompleteTF
                            .getTextbox()
                            .setText(pbCoreAttribute.getValue() == null ? "" : pbCoreAttribute.getValue());
                    valueMissingIcon
                            .setVisible(pbCoreAttribute.isRequired() && (pbCoreAttribute.getValue() == null
                                    || pbCoreAttribute.getValue().trim().isEmpty()));
                }
            });
        }
    }

    public interface DocumentAttributeSelectCVListener {

        void selectCV(PBCoreAttribute pbCoreAttribute, CVSelectionListener cvSelectionListener);
    }
}
