package digitalbedrock.software.pbcore.controllers;

import static digitalbedrock.software.pbcore.listeners.MenuListener.MenuOption;

import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.*;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

import digitalbedrock.software.pbcore.MainApp;
import digitalbedrock.software.pbcore.components.AutoFillTextAreaBox;
import digitalbedrock.software.pbcore.components.PBCoreAnyValueListCell;
import digitalbedrock.software.pbcore.components.PBCoreAttributeTreeCell;
import digitalbedrock.software.pbcore.components.PBCoreTreeCell;
import digitalbedrock.software.pbcore.core.models.CV;
import digitalbedrock.software.pbcore.core.models.CVTerm;
import digitalbedrock.software.pbcore.core.models.ElementType;
import digitalbedrock.software.pbcore.core.models.NewDocumentType;
import digitalbedrock.software.pbcore.core.models.entity.*;
import digitalbedrock.software.pbcore.listeners.*;
import digitalbedrock.software.pbcore.utils.I18nKey;
import digitalbedrock.software.pbcore.utils.LanguageManager;
import digitalbedrock.software.pbcore.utils.Registry;
import digitalbedrock.software.pbcore.utils.StringUtils;

public class DocumentBatchAddController extends AbsController implements ElementSelectionListener,
        AttributeSelectionListener, PBCoreAttributeTreeCell.AttributeTreeCellListener, CVSelectionListener {

    @FXML
    private ListView<PBCoreElementAnyValue> lvAnyValues;
    @FXML
    private TreeView<PBCoreElement> rootDocumentTreeView;
    @FXML
    private TreeView<PBCoreElement> requiredElementsListView;
    @FXML
    private TreeView<PBCoreElement> optionalElementsTreeView;
    @FXML
    private TreeView<PBCoreAttribute> attributesTreeView;
    @FXML
    private AutoFillTextAreaBox<CVTerm> taElementValue;
    @FXML
    private ComboBox<String> cbElementValue;
    @FXML
    private Button addAttributeButton;
    @FXML
    private FontIcon invalidValueIcon;
    @FXML
    private Label documentValidationLbl;
    @FXML
    private FontIcon invalidDocumentIcon;
    @FXML
    private Button buttonSave;
    @FXML
    private Button addAnyValueButton;
    @FXML
    private Button btnSelectCV;

    private final ObjectProperty<PBCoreElement> selectedPBCoreElementProperty = new SimpleObjectProperty<>();
    private PBCoreElement rootElement;

    private ChangeListener<TreeItem<PBCoreElement>> rootListener = null;
    private ChangeListener<TreeItem<PBCoreElement>> requiredListener = null;
    private ChangeListener<TreeItem<PBCoreElement>> optionalListener = null;

    private BatchFinishedListener batchFinishedListener;

    private void onRemove(int index, PBCoreElement pbCoreElement, TreeView<PBCoreElement> treeView) {

        TreeItem<PBCoreElement> selectedItem;
        selectedItem = treeView.getTreeItem(index);
        selectedItem.getParent().getValue().removeSubElement(pbCoreElement);
        TreeItem<PBCoreElement> pbCoreElementToRemove = null;
        for (TreeItem<PBCoreElement> pbCoreElementTreeItem : selectedItem.getParent().getChildren()) {
            if (pbCoreElementTreeItem.getValue().getId() == pbCoreElement.getId()
                    && StringUtils.compare(pbCoreElementTreeItem.getValue().getValue(), pbCoreElement.getValue())) {
                pbCoreElementToRemove = pbCoreElementTreeItem;
                break;
            }
        }
        if (pbCoreElementToRemove != null) {
            selectedItem.getParent().getChildren().remove(pbCoreElementToRemove);
        }
        selectedPBCoreElementProperty.setValue(null);
        updateXmlPreview();
        lvAnyValues.setVisible(false);
        taElementValue.setDisable(true);
        taElementValue.getTextbox().setPromptText(LanguageManager.INSTANCE.getString(I18nKey.NO_INPUT_REQUIRED));
    }

    private void onAdd(String treeViewId, int index, PBCoreElement pbCoreElement) {

        menuListener
                .menuOptionSelected(MenuOption.SELECT_ELEMENT, treeViewId, index, pbCoreElement,
                                    DocumentBatchAddController.this);
    }

    private void onDuplicate(int index, PBCoreElement pbCoreElement, TreeView<PBCoreElement> treeView) {

        TreeItem<PBCoreElement> selectedItem = treeView.getTreeItem(index);
        PBCoreElement copy = pbCoreElement.copy(true, false);
        copy.valueProperty.addListener((observable, oldValue, newValue) -> updateXmlPreview());
        selectedItem.getParent().getValue().addSubElement(copy);
        TreeItem<PBCoreElement> itemToAdd;
        if (treeView.equals(requiredElementsListView)) {
            itemToAdd = getRequiredTreeItem(copy, pbCoreElement.getElementType() == PBCoreElementType.ROOT_ELEMENT);
        }
        else {
            itemToAdd = getOptionalTreeItem(copy, true);
        }
        int i = selectedItem.getParent().getChildren().indexOf(selectedItem);
        selectedItem.getParent().getChildren().add(i + 1, itemToAdd);

        selectedPBCoreElementProperty.setValue(copy);
        updateXmlPreview();
    }

    private void loadRequiredTreeData(PBCoreElement rootElement) {

        TreeItem<PBCoreElement> requiredTreeItem = getRequiredTreeItem(rootElement, true);
        requiredElementsListView.setRoot(requiredTreeItem);
    }

    private void loadOptionalTreeData(PBCoreElement rootElement) {

        TreeItem<PBCoreElement> optionalTreeItem = getOptionalTreeItem(rootElement, false);
        optionalElementsTreeView.setRoot(optionalTreeItem);
    }

    private TreeItem<PBCoreAttribute> getAttributesTreeItem(PBCoreElement rootElement) {

        TreeItem<PBCoreAttribute> pbCoreAttributeTreeItem = new TreeItem<>();
        if (rootElement == null) {
            return pbCoreAttributeTreeItem;
        }
        List<PBCoreAttribute> attributes = rootElement.getAttributes();
        attributes.stream().map((attribute) -> {
            pbCoreAttributeTreeItem.getChildren().add(new TreeItem<>(attribute));
            return attribute;
        }).forEachOrdered((attribute) -> attribute.valueProperty.addListener((observable, oldValue, newValue) -> {
            buttonSave.setVisible(buttonSave.isVisible() || !Objects.equals(oldValue, newValue));
            updateXmlPreview();
        }));
        return pbCoreAttributeTreeItem;
    }

    private TreeItem<PBCoreElement> getRequiredTreeItem(PBCoreElement rootElement, boolean root) {

        TreeItem<PBCoreElement> pbCoreElementTreeItem = new TreeItem<>(rootElement);
        pbCoreElementTreeItem.setExpanded(true);
        rootElement.updateStatus();
        rootElement.valueProperty.addListener((observable, oldValue, newValue) -> updateXmlPreview());
        rootElement.validAttributesProperty.addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                updateStatusBarLabel(false, false);
            }
            else {
                updateStatusBarLabel();
            }
        });
        rootElement.validProperty.addListener((observable, oldValue, newValue) -> updateStatusBarLabel());
        rootElement.fatalErrorProperty.addListener((observable, oldValue, newValue) -> updateStatusBarLabel());
        List<PBCoreElement> subElements = rootElement.getOrderedSubElements();
        subElements
                .stream()
                .filter(pbCoreElement -> pbCoreElement.isRequired() || !root)
                .forEach(pbCoreElement -> pbCoreElementTreeItem
                        .getChildren()
                        .add(Math.min(pbCoreElement.getSequence(), pbCoreElementTreeItem.getChildren().size()),
                             getRequiredTreeItem(pbCoreElement, false)));
        return pbCoreElementTreeItem;
    }

    private TreeItem<PBCoreElement> getOptionalTreeItem(PBCoreElement rootElement, boolean hasNonRootParent) {

        TreeItem<PBCoreElement> pbCoreElementTreeItem = new TreeItem<>(rootElement);
        pbCoreElementTreeItem.setExpanded(true);
        rootElement.updateStatus();
        rootElement.valueProperty.addListener((observable, oldValue, newValue) -> updateXmlPreview());
        rootElement.validProperty.addListener((observable, oldValue, newValue) -> updateStatusBarLabel());
        rootElement.fatalErrorProperty.addListener((observable, oldValue, newValue) -> updateStatusBarLabel());
        List<PBCoreElement> subElements = rootElement.getOrderedSubElements();
        subElements
                .stream()
                .filter(pbCoreElement -> hasNonRootParent || !pbCoreElement.isRequired())
                .forEach(pbCoreElement -> pbCoreElementTreeItem
                        .getChildren()
                        .add(Math.min(pbCoreElement.getSequence(), pbCoreElementTreeItem.getChildren().size()),
                             getOptionalTreeItem(pbCoreElement, true)));
        return pbCoreElementTreeItem;
    }

    private void initListSelectionListeners() {

        rootListener = (observable, oldValue, newValue) -> {
            optionalElementsTreeView.getSelectionModel().selectedItemProperty().removeListener(optionalListener);
            requiredElementsListView.getSelectionModel().selectedItemProperty().removeListener(requiredListener);
            selectedPBCoreElementProperty.setValue(newValue == null ? null : newValue.getValue());
            taElementValue
                    .setDisable(selectedPBCoreElementProperty.getValue() == null
                            || selectedPBCoreElementProperty.getValue().isSupportsChildElements());
            if (selectedPBCoreElementProperty.getValue() == null
                    || selectedPBCoreElementProperty.getValue().isSupportsChildElements()) {
                taElementValue
                        .getTextbox()
                        .setPromptText(LanguageManager.INSTANCE.getString(I18nKey.NO_INPUT_REQUIRED));
            }
            else {
                taElementValue.getTextbox().setPromptText(LanguageManager.INSTANCE.getString(I18nKey.ENTER_HERE));
            }
            setElementValueText(selectedPBCoreElementProperty.getValue());
            optionalElementsTreeView.getSelectionModel().clearSelection();
            optionalElementsTreeView.getSelectionModel().selectedItemProperty().addListener(optionalListener);
            requiredElementsListView.getSelectionModel().clearSelection();
            requiredElementsListView.getSelectionModel().selectedItemProperty().addListener(requiredListener);
        };
        requiredListener = (observable, oldValue, newValue) -> {
            optionalElementsTreeView.getSelectionModel().selectedItemProperty().removeListener(optionalListener);
            rootDocumentTreeView.getSelectionModel().selectedItemProperty().removeListener(rootListener);
            selectedPBCoreElementProperty.setValue(newValue == null ? null : newValue.getValue());
            taElementValue
                    .setDisable(selectedPBCoreElementProperty.getValue() == null
                            || selectedPBCoreElementProperty.getValue().isSupportsChildElements());
            if (selectedPBCoreElementProperty.getValue() == null
                    || selectedPBCoreElementProperty.getValue().isSupportsChildElements()) {
                taElementValue
                        .getTextbox()
                        .setPromptText(LanguageManager.INSTANCE.getString(I18nKey.NO_INPUT_REQUIRED));
            }
            else {
                taElementValue.getTextbox().setPromptText(LanguageManager.INSTANCE.getString(I18nKey.ENTER_HERE));
            }
            setElementValueText(selectedPBCoreElementProperty.getValue());
            optionalElementsTreeView.getSelectionModel().clearSelection();
            optionalElementsTreeView.getSelectionModel().selectedItemProperty().addListener(optionalListener);
            rootDocumentTreeView.getSelectionModel().clearSelection();
            rootDocumentTreeView.getSelectionModel().selectedItemProperty().addListener(rootListener);
        };
        optionalListener = (observable, oldValue, newValue) -> {
            requiredElementsListView.getSelectionModel().selectedItemProperty().removeListener(requiredListener);
            rootDocumentTreeView.getSelectionModel().selectedItemProperty().removeListener(rootListener);
            selectedPBCoreElementProperty.setValue(newValue == null ? null : newValue.getValue());
            taElementValue
                    .setDisable(selectedPBCoreElementProperty.getValue() == null
                            || selectedPBCoreElementProperty.getValue().isSupportsChildElements());
            if (selectedPBCoreElementProperty.getValue() == null
                    || selectedPBCoreElementProperty.getValue().isSupportsChildElements()) {
                taElementValue
                        .getTextbox()
                        .setPromptText(LanguageManager.INSTANCE.getString(I18nKey.NO_INPUT_REQUIRED));
            }
            else {
                taElementValue.getTextbox().setPromptText(LanguageManager.INSTANCE.getString(I18nKey.ENTER_HERE));
            }
            setElementValueText(selectedPBCoreElementProperty.getValue());
            requiredElementsListView.getSelectionModel().clearSelection();
            requiredElementsListView.getSelectionModel().selectedItemProperty().addListener(requiredListener);
            rootDocumentTreeView.getSelectionModel().clearSelection();
            rootDocumentTreeView.getSelectionModel().selectedItemProperty().addListener(rootListener);
        };
    }

    private void setElementValueText(PBCoreElement pbCoreElement) {

        if (pbCoreElement != null && pbCoreElement.isAnyElement()) {
            lvAnyValues.setItems(FXCollections.observableArrayList(pbCoreElement.getAnyValues()));
            updateInvalidIcon(false, false);
            addAnyValueButton.setVisible(true);
        }
        else {
            taElementValue.getData().clear();
            taElementValue
                    .getTextbox()
                    .setText(pbCoreElement == null || pbCoreElement.getValue() == null ? null
                            : pbCoreElement.getValue());
            if (pbCoreElement == null) {
                taElementValue.setFilterMode(false);
                invalidValueIcon.setVisible(false);
                return;
            }
            Registry registry = MainApp.getInstance().getRegistry();
            boolean b = registry.getControlledVocabularies().containsKey(pbCoreElement.getName());
            taElementValue.setFilterMode(b);
            if (b) {
                List<CVTerm> suggestions = new ArrayList<>();
                CV cv = registry.getControlledVocabularies().get(pbCoreElement.getName());
                if (cv.isHasSubs()) {
                    cv.getSubs().entrySet().forEach((stringCVBaseEntry) -> {
                        suggestions.addAll(stringCVBaseEntry.getValue().getTerms());
                    });
                }
                else {
                    suggestions.addAll(cv.getTerms());
                }
                taElementValue.getData().addAll(suggestions);
            }
            updateInvalidIcon(!pbCoreElement.isValid()
                    && pbCoreElement.getElementType() != PBCoreElementType.ROOT_ELEMENT, pbCoreElement.isFatalError());
            addAnyValueButton.setVisible(false);
        }
    }

    private void updateInvalidIcon(boolean isInvalid, boolean fatalError) {

        invalidValueIcon.setVisible(isInvalid);
        if (invalidValueIcon.isVisible()) {
            invalidValueIcon.getStyleClass().remove("panicIcon");
            invalidValueIcon.getStyleClass().remove("warningIcon");
            if (fatalError) {
                invalidValueIcon.getStyleClass().add("panicIcon");
                invalidValueIcon.setIconCode(MaterialDesign.MDI_ALERT_CIRCLE);
            }
            else {
                invalidValueIcon.getStyleClass().add("warningIcon");
                invalidValueIcon.setIconCode(MaterialDesign.MDI_ALERT);
            }
            Tooltip tooltip = new Tooltip(getErrorMessage());
            invalidValueIcon.setOnMouseEntered(event -> {
                Point2D p = invalidValueIcon
                        .localToScreen(invalidValueIcon.getLayoutBounds().getMaxX(),
                                       invalidValueIcon.getLayoutBounds().getMaxY());
                tooltip.show(invalidValueIcon, p.getX(), p.getY() + 2);
            });
            invalidValueIcon.setOnMouseExited(event -> tooltip.hide());
        }
        else {
            invalidValueIcon.getStyleClass().add("warningIcon");
            invalidValueIcon.setIconCode(MaterialDesign.MDI_ALERT);
            invalidValueIcon.setOnMouseEntered(null);
            invalidValueIcon.setOnMouseEntered(null);
        }
    }

    private String getErrorMessage() {

        if (selectedPBCoreElementProperty.getValue().isHasChildElements()
                && !selectedPBCoreElementProperty.getValue().isValid()) {
            if (selectedPBCoreElementProperty.getValue().isFatalError()) {
                invalidDocumentIcon.getStyleClass().add("panicIcon");
                invalidDocumentIcon.setIconCode(MaterialDesign.MDI_ALERT_CIRCLE);
                if (selectedPBCoreElementProperty.getValue().getSubElements().size() > 1) {
                    return "At least one child node has an invalid value";
                }
                else {
                    return "Child node has an invalid value";
                }
            }
            else {
                invalidDocumentIcon.getStyleClass().add("warningIcon");
                invalidDocumentIcon.setIconCode(MaterialDesign.MDI_ALERT);
                if (selectedPBCoreElementProperty.getValue().getSubElements().size() > 1) {
                    return "At least one child node is missing is respective value";
                }
                else {
                    return "Child node is missing is respective value";
                }
            }
        }
        switch (selectedPBCoreElementProperty.getValue().getElementValueRestrictionType()) {
            case ENUMERATION:
                for (String enumerationValue : selectedPBCoreElementProperty.getValue().getEnumerationValues()) {
                    if (selectedPBCoreElementProperty.getValue().getValue().equals(enumerationValue)) {
                        return "";
                    }
                }
                invalidDocumentIcon.getStyleClass().add("panicIcon");
                invalidDocumentIcon.setIconCode(MaterialDesign.MDI_ALERT_CIRCLE);
                return "Invalid value. Only one of " + selectedPBCoreElementProperty.getValue().getEnumerationValues()
                        + " is allowed";
            case PATTERN:
                Pattern pattern = Pattern.compile(selectedPBCoreElementProperty.getValue().getPatternToFollow());
                String value = selectedPBCoreElementProperty.getValue().getValue();
                Matcher matcher = pattern.matcher(value == null ? "" : value);
                if (matcher.matches()) {
                    return "";
                }
                invalidDocumentIcon.getStyleClass().add("panicIcon");
                invalidDocumentIcon.setIconCode(MaterialDesign.MDI_ALERT_CIRCLE);
                return "Value doesn't match required pattern";
            case SIMPLE:
                if (selectedPBCoreElementProperty.getValue().isAnyElement()
                        && selectedPBCoreElementProperty.getValue().getAnyValues().isEmpty()) {
                    return "This element should have at least one value associated";
                }
                else if (selectedPBCoreElementProperty.getValue().getValue() == null
                        || selectedPBCoreElementProperty.getValue().getValue().trim().isEmpty()) {
                    return "Value missing";
                }
                else {
                    invalidDocumentIcon.getStyleClass().add("warningIcon");
                    invalidDocumentIcon.setIconCode(MaterialDesign.MDI_ALERT);
                    return "Value not matching any of the controlled vocabularies defined for this element";
                }
        }
        return "";
    }

    @FXML
    public void onAddAttribute(ActionEvent event) {

        MainApp
                .getInstance()
                .showSelectAttribute(selectedPBCoreElementProperty.getValue(), DocumentBatchAddController.this);
    }

    @Override
    public void onElementSelected(String treeViewId, int index, PBCoreElement element, boolean close) {

        if (element == null) {
            return;
        }
        ElementType elementType;
        TreeItem<PBCoreElement> selectedItem;
        TreeItem<PBCoreElement> pbCoreElementTreeItem;
        if (selectedPBCoreElementProperty.getValue() == null
                || selectedPBCoreElementProperty.getValue().getId() == rootElement.getId()) {
            if (element.isRequired()) {
                selectedItem = requiredElementsListView.getRoot();
                pbCoreElementTreeItem = getRequiredTreeItem(element,
                                                            element.getElementType() == PBCoreElementType.ROOT_ELEMENT);
                elementType = ElementType.REQUIRED;
            }
            else {
                selectedItem = optionalElementsTreeView.getRoot();
                pbCoreElementTreeItem = getOptionalTreeItem(element, true);
                elementType = ElementType.OPTIONAL;
            }
        }
        else if (treeViewId.equals(requiredElementsListView.getId())) {
            selectedItem = requiredElementsListView.getTreeItem(index);
            pbCoreElementTreeItem = getRequiredTreeItem(element,
                                                        element.getElementType() == PBCoreElementType.ROOT_ELEMENT);
            elementType = ElementType.REQUIRED;
        }
        else {
            selectedItem = optionalElementsTreeView.getTreeItem(index);
            if (selectedItem == null) {
                selectedItem = requiredElementsListView.getTreeItem(index);
                pbCoreElementTreeItem = getRequiredTreeItem(element,
                                                            element.getElementType() == PBCoreElementType.ROOT_ELEMENT);
                elementType = ElementType.REQUIRED;
            }
            else {
                pbCoreElementTreeItem = getOptionalTreeItem(element, true);
                elementType = ElementType.OPTIONAL;
            }
        }
        selectedItem.getValue().addSubElement(element);

        pbCoreElementTreeItem.setExpanded(true);
        selectedItem.setExpanded(true);
        selectedItem
                .getChildren()
                .add(Math
                        .min(element.getSequence(),
                             selectedItem.getChildren().isEmpty() ? 0 : selectedItem.getChildren().size() - 1),
                     pbCoreElementTreeItem);
        if (close) {
            selectedPBCoreElementProperty.setValue(element);
            setElementValueText(element);
            switch (elementType) {
                case REQUIRED:
                    requiredElementsListView.getSelectionModel().select(0);
                    rootDocumentTreeView.getSelectionModel().clearSelection();
                    optionalElementsTreeView.getSelectionModel().clearSelection();
                    break;
                case OPTIONAL:
                    optionalElementsTreeView.getSelectionModel().select(0);
                    requiredElementsListView.getSelectionModel().clearSelection();
                    rootDocumentTreeView.getSelectionModel().clearSelection();
                    break;
            }
        }
        updateXmlPreview();
        updateStatusBarLabel();
    }

    @Override
    public void onAttributeSelected(PBCoreAttribute pbCoreAttribute, boolean close) {

        if (pbCoreAttribute == null) {
            return;
        }
        selectedPBCoreElementProperty.getValue().addAttribute(pbCoreAttribute.copy());
        attributesTreeView.setRoot(getAttributesTreeItem(selectedPBCoreElementProperty.getValue()));
        updateXmlPreview();
    }

    @Override
    public void onRemoveAttribute(PBCoreAttribute pbCoreAttribute) {

        selectedPBCoreElementProperty.getValue().removeAttribute(pbCoreAttribute);
        attributesTreeView.setRoot(getAttributesTreeItem(selectedPBCoreElementProperty.getValue()));
        updateXmlPreview();
    }

    private void updateXmlPreview() {

        PBCoreElement value = requiredElementsListView.getRoot().getValue();
        MainApp.getInstance().getRegistry().saveBatchEditPBCoreElement(value);
    }

    private void updateStatusBarLabel() {

        boolean valid = rootDocumentTreeView.getRoot().getValue().isValid()
                && rootDocumentTreeView.getRoot().getValue().isValidAttributes()
                && requiredElementsListView.getRoot().getValue().isValid()
                && requiredElementsListView.getRoot().getValue().isValidAttributes()
                && optionalElementsTreeView.getRoot().getValue().isValid()
                && optionalElementsTreeView.getRoot().getValue().isValidAttributes();
        boolean fatalError = rootDocumentTreeView.getRoot().getValue().isFatalError()
                && requiredElementsListView.getRoot().getValue().isFatalError()
                && optionalElementsTreeView.getRoot().getValue().isFatalError();
        updateStatusBarLabel(valid, fatalError);
    }

    private void updateStatusBarLabel(boolean valid, boolean fatalError) {

        DocumentController.updateStatusBarLabel(valid, fatalError, invalidDocumentIcon, documentValidationLbl);
    }

    public void initializeDocument(BatchFinishedListener batchFinishedListener) {

        this.batchFinishedListener = batchFinishedListener;
        if (MainApp.getInstance().getRegistry().getBatchEditPBCoreElement() == null) {
            rootElement = PBCoreStructure.getInstance().getRootElement(NewDocumentType.DESCRIPTION_DOCUMENT).copy(true);
            rootElement.clearOptionalSubElements();
            rootElement.getSubElements().clear();
            MainApp.getInstance().getRegistry().saveBatchEditPBCoreElement(rootElement);
        }
        else {
            this.rootElement = MainApp.getInstance().getRegistry().getBatchEditPBCoreElement().copy(true);
        }
        initListSelectionListeners();

        selectedPBCoreElementProperty.addListener((observable, oldValue, newValue) -> {
            btnSelectCV
                    .setVisible(MainApp
                            .getInstance()
                            .getRegistry()
                            .getControlledVocabularies()
                            .containsKey(selectedPBCoreElementProperty.getValue().getName()));
            attributesTreeView.setRoot(getAttributesTreeItem(newValue));
            addAttributeButton.setDisable(newValue == null || !newValue.isSupportsAttributes());
            if (newValue == null) {
                taElementValue.setVisible(true);
                cbElementValue.setVisible(false);
                invalidValueIcon.setVisible(false);
            }
            else {
                switch (newValue.getElementValueRestrictionType()) {
                    case ENUMERATION:
                        taElementValue.setVisible(false);
                        cbElementValue.setVisible(true);
                        lvAnyValues.setVisible(false);
                        cbElementValue.setItems(FXCollections.observableArrayList(newValue.getEnumerationValues()));
                        int i = newValue.getEnumerationValues().indexOf(newValue.getValue());
                        cbElementValue.getSelectionModel().select(i < 0 ? 0 : i);
                        break;
                    default:
                        if (newValue.isAnyElement()) {
                            lvAnyValues.setVisible(true);
                            taElementValue.setVisible(false);
                            cbElementValue.setVisible(false);
                        }
                        else {
                            lvAnyValues.setVisible(false);
                            taElementValue.setVisible(true);
                            cbElementValue.setVisible(false);
                        }
                        cbElementValue.getItems().clear();
                }
            }
        });
        addAttributeButton.setDisable(true);

        TreeItem<PBCoreElement> elementTreeItem = new TreeItem<>(rootElement);
        rootDocumentTreeView.setRoot(elementTreeItem);
        rootDocumentTreeView
                .setCellFactory(lv -> new PBCoreTreeCell(false, false,
                        new DocumentElementItemController.DocumentElementInteractionListener() {

                            @Override
                            public void onRemove(int index, PBCoreElement pbCoreElement) {

                            }

                            @Override
                            public void onAdd(int index, PBCoreElement pbCoreElement) {

                                rootDocumentTreeView.getSelectionModel().selectFirst();
                                DocumentBatchAddController.this
                                        .onAdd(rootDocumentTreeView.getId(), index, pbCoreElement);
                            }

                            @Override
                            public void onDuplicate(int index, PBCoreElement pbCoreElement) {

                            }
                        }));

        requiredElementsListView.setShowRoot(false);
        requiredElementsListView
                .setCellFactory(lv -> new PBCoreTreeCell(true, true,
                        new DocumentElementItemController.DocumentElementInteractionListener() {

                            @Override
                            public void onRemove(int index, PBCoreElement pbCoreElement) {

                                DocumentBatchAddController.this
                                        .onRemove(index, pbCoreElement, requiredElementsListView);
                                requiredElementsListView.getSelectionModel().clearSelection();
                            }

                            @Override
                            public void onAdd(int index, PBCoreElement pbCoreElement) {

                                requiredElementsListView.getSelectionModel().select(index);
                                DocumentBatchAddController.this
                                        .onAdd(requiredElementsListView.getId(), index, pbCoreElement);
                            }

                            @Override
                            public void onDuplicate(int index, PBCoreElement pbCoreElement) {

                                DocumentBatchAddController.this
                                        .onDuplicate(index, pbCoreElement, requiredElementsListView);
                                requiredElementsListView.getSelectionModel().select(index + 1);
                            }
                        }));
        loadRequiredTreeData(rootElement);

        optionalElementsTreeView.setShowRoot(false);
        optionalElementsTreeView
                .setCellFactory(lv -> new PBCoreTreeCell(true, true,
                        new DocumentElementItemController.DocumentElementInteractionListener() {

                            @Override
                            public void onRemove(int index, PBCoreElement pbCoreElement) {

                                DocumentBatchAddController.this
                                        .onRemove(index, pbCoreElement, optionalElementsTreeView);
                                optionalElementsTreeView.getSelectionModel().clearSelection();
                            }

                            @Override
                            public void onAdd(int index, PBCoreElement pbCoreElement) {

                                optionalElementsTreeView.getSelectionModel().select(index);
                                DocumentBatchAddController.this
                                        .onAdd(optionalElementsTreeView.getId(), index, pbCoreElement);
                            }

                            @Override
                            public void onDuplicate(int index, PBCoreElement pbCoreElement) {

                                DocumentBatchAddController.this
                                        .onDuplicate(index, pbCoreElement, optionalElementsTreeView);
                                optionalElementsTreeView.getSelectionModel().select(index + 1);
                            }
                        }));
        loadOptionalTreeData(rootElement);

        attributesTreeView.setShowRoot(false);
        attributesTreeView
                .setCellFactory(lv -> new PBCoreAttributeTreeCell(DocumentBatchAddController.this,
                        (pbCoreAttribute, listener) -> menuListener
                                .menuOptionSelected(MenuOption.SELECT_CV_ATTRIBUTE, pbCoreAttribute, listener)));

        requiredElementsListView.getSelectionModel().selectedItemProperty().addListener(requiredListener);
        optionalElementsTreeView.getSelectionModel().selectedItemProperty().addListener(optionalListener);

        taElementValue.getTextbox().textProperty().addListener((observable, oldValue, newValue) -> {
            PBCoreElement value = selectedPBCoreElementProperty.getValue();
            if (value == null) {
                return;
            }
            buttonSave.setVisible(buttonSave.isVisible() || !Objects.equals(value.getValue(), newValue));
            value.setValue(taElementValue.getText());
            Registry registry = MainApp.getInstance().getRegistry();
            if (registry.getControlledVocabularies().containsKey(value.getName())) {
                List<CVTerm> suggestions = new ArrayList<>();
                CV cv = registry.getControlledVocabularies().get(value.getName());
                if (cv.isHasSubs()) {
                    cv.getSubs().forEach((key, value1) -> suggestions.addAll(value1.getTerms()));
                }
                else {
                    suggestions.addAll(cv.getTerms());
                }
                value
                        .setValid(suggestions
                                .stream()
                                .anyMatch(cvTerm -> cvTerm.getTerm().equalsIgnoreCase(taElementValue.getText())));
                PBCoreStructure.getInstance().updateSourceAttributeOnElement(value, taElementValue.getItem());
                attributesTreeView.setRoot(getAttributesTreeItem(value));
            }
            else {
                DocumentController.validateChildElements(value, taElementValue);
            }
            updateInvalidIcon(!value.isValid(), value.isFatalError());
        });
        cbElementValue.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (selectedPBCoreElementProperty.getValue() == null || newValue == null) {
                return;
            }
            selectedPBCoreElementProperty.getValue().setValue(newValue);
            selectedPBCoreElementProperty.getValue().setValid(true);
        });
        taElementValue.setDisable(true);
        taElementValue.getTextbox().setPromptText(LanguageManager.INSTANCE.getString(I18nKey.NO_INPUT_REQUIRED));
        cbElementValue.setVisible(false);
        setElementValueText(selectedPBCoreElementProperty.getValue());
        updateXmlPreview();

        invalidValueIcon.setVisible(false);
        selectedPBCoreElementProperty.setValue(rootElement);
        rootDocumentTreeView.getSelectionModel().select(0);
        updateStatusBarLabel();
        buttonSave.setVisible(false);

        lvAnyValues.setCellFactory(lv -> new PBCoreAnyValueListCell(pbCoreElementAnyValue -> {
            if (pbCoreElementAnyValue == null) {
                return;
            }
            selectedPBCoreElementProperty.getValue().removeAnyValue(pbCoreElementAnyValue);
            lvAnyValues.getItems().remove(pbCoreElementAnyValue);
            updateXmlPreview();
            updateInvalidIcon(false, false);
        }));
        addAnyValueButton.setOnAction(event -> {
            menuListener
                    .menuOptionSelected(MenuOption.ADD_ELEMENT_ANY_VALUE, selectedPBCoreElementProperty.getValue(),
                                        (AddElementAnyValueListener) pbCoreElementAnyValue -> {
                                            if (pbCoreElementAnyValue == null) {
                                                return;
                                            }
                                            selectedPBCoreElementProperty
                                                    .getValue()
                                                    .addAnyElement(pbCoreElementAnyValue);
                                            lvAnyValues.getItems().add(pbCoreElementAnyValue);
                                            updateXmlPreview();
                                            updateInvalidIcon(false, false);
                                        });
        });
    }

    @FXML
    public void saveFile(ActionEvent event) {

        if (event == null && !buttonSave.isVisible()) {
            batchFinishedListener.onSaveBatch(requiredElementsListView.getRoot().getValue());
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(LanguageManager.INSTANCE.getString(I18nKey.ADD_ELEMENTS_TO_OPEN_FILES));
        alert.setContentText(LanguageManager.INSTANCE.getString(I18nKey.ADD_ELEMENTS_TO_OPEN_FILES_DESCRIPTION));
        alert.setHeaderText(null);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (batchFinishedListener != null) {
                batchFinishedListener.onSaveBatch(requiredElementsListView.getRoot().getValue());
            }
        }
    }

    public void saveDocument() {

        MainApp.getInstance().getRegistry().saveBatchEditPBCoreElement(rootElement);
    }

    @Override
    public MenuBar createMenu() {

        return new MenuBar();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    @FXML
    public void selectCV(ActionEvent actionEvent) {

        if (MainApp
                .getInstance()
                .getRegistry()
                .getControlledVocabularies()
                .containsKey(selectedPBCoreElementProperty.getValue().getName())) {
            menuListener
                    .menuOptionSelected(MenuOption.SELECT_CV_ELEMENT, selectedPBCoreElementProperty.getValue(),
                                        DocumentBatchAddController.this);
        }
    }

    @Override
    public void onCVSelected(String key, CVTerm cvTerm, boolean attr) {

        PBCoreElement value = selectedPBCoreElementProperty.getValue();
        if (cvTerm != null && !attr && value != null && value.getName().equals(key)) {
            value.setValue(cvTerm.getTerm());
            PBCoreStructure.getInstance().updateSourceAttributeOnElement(value, cvTerm);
            attributesTreeView.setRoot(getAttributesTreeItem(value));
            setElementValueText(value);
        }
    }
}
