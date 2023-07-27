package digitalbedrock.software.pbcore.controllers;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;

import digitalbedrock.software.pbcore.MainApp;
import digitalbedrock.software.pbcore.core.models.NewDocumentType;
import digitalbedrock.software.pbcore.core.models.entity.PBCoreElement;
import digitalbedrock.software.pbcore.core.models.entity.PBCoreElementType;
import digitalbedrock.software.pbcore.core.models.entity.PBCoreStructure;
import digitalbedrock.software.pbcore.listeners.ElementSelectionListener;
import digitalbedrock.software.pbcore.utils.I18nKey;
import digitalbedrock.software.pbcore.utils.LanguageManager;

public class CVElementSelectorController extends AbsController {

    @FXML
    private Label lblDescription;
    @FXML
    private Text lblOptional;
    @FXML
    private Text lblRepeatable;
    @FXML
    private Button btnCancel;
    @FXML
    private Button btnAdd;
    @FXML
    private Label lblElementAlreadyAdded;
    @FXML
    private Label lblChoice;

    @FXML
    private TreeView<PBCoreElement> treeElements;

    @FXML
    private ToggleGroup typeRadio;
    @FXML
    private RadioButton rbInstantiation;
    @FXML
    private RadioButton rbDescription;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        treeElements.setShowRoot(false);
        treeElements.setCellFactory(lv -> new PBCoreTreeCell());
        ChangeListener<TreeItem<PBCoreElement>> listener = (observable, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }
            PBCoreElement value = newValue.getValue();
            lblDescription.setText(value.getDescription());
            lblOptional
                    .setText(value.isRequired() ? LanguageManager.INSTANCE.getString(I18nKey.REQUIRED)
                            : LanguageManager.INSTANCE.getString(I18nKey.OPTIONAL));
            String repeatable = value.isRepeatable() ? LanguageManager.INSTANCE.getString(I18nKey.REPEATABLE) : "";
            String choice = value.isChoice() ? LanguageManager.INSTANCE.getString(I18nKey.CHOICE) : "";
            lblRepeatable.setText(repeatable + choice);
            lblElementAlreadyAdded.setText(LanguageManager.INSTANCE.getString(I18nKey.ELEMENT_ALREADY_IN_USE));
            lblElementAlreadyAdded.setVisible(false);
            btnAdd.setDisable(false);
            if (MainApp
                    .getInstance()
                    .getRegistry()
                    .getControlledVocabularies()
                    .containsKey(newValue.getValue().getName())) {
                btnAdd.setDisable(true);
                lblElementAlreadyAdded.setVisible(true);
            }
            else if (value.isHasChildElements()) {
                lblElementAlreadyAdded.setText(LanguageManager.INSTANCE.getString(I18nKey.ELEMENT_IS_A_PARENT_ELEMENT));
                btnAdd.setDisable(true);
                lblElementAlreadyAdded.setVisible(true);
            }
            lblChoice.setVisible(false);
            if (value.isChoice()) {
                btnAdd.setDisable(true);
                lblChoice.setVisible(true);
            }
        };
        treeElements.getSelectionModel().selectedItemProperty().addListener(listener);
        typeRadio.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (typeRadio.getSelectedToggle().equals(rbDescription)) {
                loadElements(NewDocumentType.DESCRIPTION_DOCUMENT);
            }
            else if (typeRadio.getSelectedToggle().equals(rbInstantiation)) {
                loadElements(NewDocumentType.INSTANTIATION_DOCUMENT);
            }
        });
        loadElements(NewDocumentType.DESCRIPTION_DOCUMENT);
    }

    public void setElementSelectionListener(ElementSelectionListener elementSelectionListener) {

        btnCancel.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            elementSelectionListener.onElementSelected(null, 0, null, false);
        });
        btnAdd.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            TreeItem<PBCoreElement> selectedItem = treeElements.getSelectionModel().getSelectedItem();
            MainApp
                    .getInstance()
                    .getRegistry()
                    .createNewVocabulariesAggregator(selectedItem.getValue().getName(), false);
            elementSelectionListener.onElementSelected(null, 0, null, false);
        });
    }

    private TreeItem<PBCoreElement> getTreeItem(PBCoreElement rootElement) {

        TreeItem<PBCoreElement> pbCoreElementTreeItem = new TreeItem<>(rootElement);
        rootElement
                .getOrderedSubElements()
                .forEach((coreElement) -> pbCoreElementTreeItem.getChildren().add(getTreeItem(coreElement)));
        return pbCoreElementTreeItem;
    }

    public void loadElements(NewDocumentType newDocumentType) {

        PBCoreElement copy = PBCoreStructure.getInstance().getRootElement(newDocumentType).copy();
        if (copy.getElementType() == PBCoreElementType.ROOT_ELEMENT) {
            copy.getSubElements().forEach(PBCoreElement::unmarkAsRootElement);
        }
        treeElements.setRoot(getTreeItem(copy));
        treeElements.getSelectionModel().select(0);
    }

    @Override
    public MenuBar createMenu() {

        return null;
    }

    private class PBCoreTreeCell extends TreeCell<PBCoreElement> {

        @Override
        protected void updateItem(PBCoreElement item, boolean empty) {

            super.updateItem(item, empty);
            if (!empty) {
                setText(item.getScreenName());
                setTooltip(new Tooltip(item.getTooltip()));
            }
            else {
                setText(null);
                setTooltip(null);
            }
        }
    }
}
