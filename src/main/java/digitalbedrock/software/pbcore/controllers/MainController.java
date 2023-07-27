package digitalbedrock.software.pbcore.controllers;

import static digitalbedrock.software.pbcore.listeners.MenuListener.MenuOption.EXPORT_OPEN_FILES_TO_CSV;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import digitalbedrock.software.pbcore.core.PBcoreValidator;
import digitalbedrock.software.pbcore.core.models.NewDocumentType;
import digitalbedrock.software.pbcore.core.models.entity.PBCoreElement;
import digitalbedrock.software.pbcore.core.models.entity.PBCoreStructure;
import digitalbedrock.software.pbcore.listeners.FileChangedListener;
import digitalbedrock.software.pbcore.listeners.MenuListener.MenuOption;
import digitalbedrock.software.pbcore.listeners.SavableTabListener;
import digitalbedrock.software.pbcore.listeners.SavedSearchedUpdated;
import digitalbedrock.software.pbcore.listeners.SearchResultListener;
import digitalbedrock.software.pbcore.lucene.HitDocument;
import digitalbedrock.software.pbcore.lucene.LuceneEngineSearchFilter;
import digitalbedrock.software.pbcore.parsers.CSVPBCoreParser;
import digitalbedrock.software.pbcore.utils.I18nKey;
import digitalbedrock.software.pbcore.utils.LanguageManager;
import digitalbedrock.software.pbcore.utils.Registry;
import lombok.Setter;

public class MainController extends AbsController
        implements FileChangedListener, SearchResultListener, SavedSearchedUpdated {

    public static final Logger LOGGER = Logger.getLogger(MainController.class.getName());
    private static final String UNTITLED = "untitled";
    public static final String BATCH_EDIT_ID = "batch-edit";
    @FXML
    private AnchorPane splash;
    @FXML
    private AnchorPane spinnerLayer;
    @FXML
    private TabPane tabPane;

    private BooleanProperty nonExportableProperty = new SimpleBooleanProperty(true);

    private final AtomicInteger untitledTabsCount = new AtomicInteger(0);

    private final List<SavableTabListener> openedTabs = new ArrayList<>();
    private SavableTabListener currentSavableTabListener;
    private DocumentBatchAddController batchAddController;
    private Tab batchAddTab;

    private Menu search;
    private PBcoreValidator validator;
    @Setter
    private Registry registry;

    public MainController() {

        try {
            validator = new PBcoreValidator();
        }
        catch (SAXException e) {
            LOGGER.log(Level.SEVERE, "could not instantiate PBCoreValidator", e);
        }
    }

    public void menuOptionSelected(MenuOption menuOption, Object... objects) {

        switch (menuOption) {
            case OPEN_FILE:
                openDocument((File) objects[0]);
                break;
            case BATCH_EDIT:
                batchAdd();
                break;
            case CONVERT_FROM_CSV:
                convertFromCsv(objects[0]);
                break;
            case EXPORT_OPEN_FILES_TO_CSV:
                exportOpenFiles(false);
                break;
            case NEW_DESCRIPTION_DOCUMENT:
                newDocument(NewDocumentType.DESCRIPTION_DOCUMENT);
                break;
            case NEW_INSTANTIATION_DOCUMENT:
                newDocument(NewDocumentType.INSTANTIATION_DOCUMENT);
                break;
            case NEW_COLLECTION:
                newDocument(NewDocumentType.COLLECTION);
                break;
            case SAVE:
                saveDocument();
                break;
            case SAVE_AS:
                saveDocumentAs();
                break;
            case SAVE_AS_TEMPLATE:
                saveDocumentAsTemplate();
                break;
            case EXPORT_OPEN_FILES_TO_ZIP:
                exportOpenFiles(true);
                break;
            default:
                menuListener.menuOptionSelected(menuOption, objects);
                break;
        }
    }

    private void convertFromCsv(Object object) {

        try {
            spinnerLayer.setVisible(true);
            DirectoryChooser directoryChooser = new DirectoryChooser();
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(LanguageManager.INSTANCE.getString(I18nKey.SELECT_DESTINATION_FOLDER));
            alert.setHeaderText(null);
            alert.setContentText(LanguageManager.INSTANCE.getString(I18nKey.SELECT_DESTINATION_FOLDER_DESCRIPTION));
            Optional<ButtonType> buttonType = alert.showAndWait();
            if (!buttonType.isPresent() || buttonType.get().getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
                spinnerLayer.setVisible(false);
                return;
            }
            File file = directoryChooser.showDialog(tabPane.getScene().getWindow());
            if (file == null) {
                spinnerLayer.setVisible(false);
                return;
            }
            int i = 1;
            for (PBCoreElement pbCoreElement : CSVPBCoreParser.parseFile(((File) object).getAbsolutePath())) {
                pbCoreElement.clearAllEmptyElementsAndAttributes();
                PBCoreStructure
                        .getInstance()
                        .saveFile(pbCoreElement,
                                  new File(file.getAbsolutePath() + "/imported_from_csv_" + i++ + ".xml"));
            }
            alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(LanguageManager.INSTANCE.getString(I18nKey.FILE_CONVERTED));
            alert.setHeaderText(null);
            alert.setContentText(LanguageManager.INSTANCE.getString(I18nKey.FILE_CONVERTED_DESCRIPTION));
            alert.showAndWait();
            spinnerLayer.setVisible(false);
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "The selected file is not compliant with the PBCore csv template", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(LanguageManager.INSTANCE.getString(I18nKey.INVALID_FILE_TO_IMPORT));
            alert.setHeaderText(null);
            alert.setContentText(LanguageManager.INSTANCE.getString(I18nKey.INVALID_FILE_TO_IMPORT_DESCRIPTION));
            alert.showAndWait();
            spinnerLayer.setVisible(false);
        }
    }

    private void exportOpenFiles(boolean isZip) {

        spinnerLayer.setVisible(true);
        FileChooser fileChooser = new FileChooser();

        FileChooser.ExtensionFilter extFilter;
        if (isZip) {
            extFilter = new FileChooser.ExtensionFilter(LanguageManager.INSTANCE.getString(I18nKey.ZIP_FILES), "*.zip");
        }
        else {
            extFilter = new FileChooser.ExtensionFilter(LanguageManager.INSTANCE.getString(I18nKey.CSV_FILES), "*.csv");
        }
        fileChooser.getExtensionFilters().add(extFilter);

        File file = fileChooser.showSaveDialog(tabPane.getScene().getWindow());
        if (file == null) {
            spinnerLayer.setVisible(false);
            return;
        }
        Map<String, PBCoreElement> map = new HashMap<>();
        registry.getPbCoreElements().forEach((key, value) -> {
            String s1 = registry.getCurrentWorkPages().get(key);
            if (s1 == null) {
                return;
            }
            if (!isZip && !value.getFullPath().startsWith("pbcoreDescriptionDocument")) {
                return;
            }
            int c = 1;
            String filename = s1;
            if (!filename.endsWith(".xml")) {
                filename += ".xml";
                s1 += ".xml";
            }
            while (map.containsKey(s1)) {
                String[] split = filename.split(".xml");
                s1 = split[0] + "_" + c++ + ".xml";
            }
            map.put(s1, value);
        });
        try {
            if (isZip) {
                PBCoreStructure.getInstance().saveFilesToZip(map, file);
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle(LanguageManager.INSTANCE.getString(I18nKey.DOCUMENT_EXPORTED));
                alert.setContentText(LanguageManager.INSTANCE.getString(I18nKey.DOCUMENT_EXPORTED_DESCRIPTION_ZIP));
                alert.setHeaderText(null);
                alert.getButtonTypes().setAll(new ButtonType(LanguageManager.INSTANCE.getString(I18nKey.OK)));
                alert.showAndWait();
            }
            else {
                CSVPBCoreParser.writeFile(map, file.getAbsolutePath());
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle(LanguageManager.INSTANCE.getString(I18nKey.DOCUMENT_EXPORTED));
                alert.setContentText(LanguageManager.INSTANCE.getString(I18nKey.DOCUMENT_EXPORTED_DESCRIPTION_CSV));
                alert.setHeaderText(null);
                alert.getButtonTypes().setAll(new ButtonType(LanguageManager.INSTANCE.getString(I18nKey.OK)));
                alert.showAndWait();
            }
        }
        catch (ParserConfigurationException | TransformerException | IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex.getMessage());
        }
        spinnerLayer.setVisible(false);
    }

    private void batchAdd() {

        showBatchTab();
    }

    private void newDocument(NewDocumentType newDocumentType) {

        PBCoreElement rootElement = PBCoreStructure.getInstance().getRootElement(newDocumentType);
        rootElement.clearOptionalSubElements();
        showTab(null, null, rootElement);
    }

    private void openDocument(File file) {

        try {
            validator.validate(file);
        }
        catch (SAXException | IOException e) {
            try {
                PBCoreElement pbCoreElement = PBCoreStructure.getInstance().parseFile(file);
                showTab(null, file, pbCoreElement.copy(), true);
                showWarningMessage();
            }
            catch (JAXBException | IllegalAccessException ex) {
                showErrorMessage();
                return;
            }
        }
        try {
            PBCoreElement pbCoreElement = PBCoreStructure.getInstance().parseFile(file);
            showTab(null, file, pbCoreElement.copy());
        }
        catch (JAXBException | IllegalAccessException e) {
            showErrorMessage();
        }

    }

    private void showWarningMessage() {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(LanguageManager.INSTANCE.getString(I18nKey.FILE_MODIFIED));
        alert.setHeaderText(null);
        alert.setContentText(LanguageManager.INSTANCE.getString(I18nKey.FILE_MODIFIED_DESCRIPTION));
        alert.showAndWait();
    }

    private void showErrorMessage() {

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(LanguageManager.INSTANCE.getString(I18nKey.INVALID_FILE));
        alert.setHeaderText(null);
        alert.setContentText(LanguageManager.INSTANCE.getString(I18nKey.INVALID_FILE_DESCRIPTION));
        alert.showAndWait();
    }

    private void saveDocument() {

        if (currentSavableTabListener != null) {
            currentSavableTabListener.saveDocument();
        }
    }

    private void saveDocumentAs() {

        if (currentSavableTabListener != null) {
            currentSavableTabListener.saveDocumentAs();
        }
    }

    private void saveDocumentAsTemplate() {

        if (currentSavableTabListener != null) {
            currentSavableTabListener.saveDocumentAsTemplate();
        }
    }

    private void showTab(String token, File file, PBCoreElement pbCoreElement) {

        showTab(token, file, pbCoreElement, false);
    }

    private void showTab(String token, File file, PBCoreElement pbCoreElement, boolean changesDetected) {

        if (file != null) {
            File finalFile = file;
            Tab tab1 = tabPane
                    .getTabs()
                    .stream()
                    .filter(tab -> tab.getId().equalsIgnoreCase(finalFile.getAbsolutePath()))
                    .findFirst()
                    .orElse(null);
            if (tab1 != null) {
                tabPane.getSelectionModel().select(tab1);
                return;
            }
        }
        String title;
        String id;
        if (file != null) {
            if (file.exists()) {
                title = file.getName();
                id = file.getAbsolutePath();
            }
            else {
                title = file.getName();
                id = file.getName();
                untitledTabsCount.addAndGet(1);
                file = null;
            }
        }
        else {
            title = UNTITLED + String.format("%04d", untitledTabsCount.addAndGet(1));
            id = title;
        }
        Tab tab = new Tab(title);
        tab.setId(id);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/document.fxml"),
                    LanguageManager.INSTANCE.getBundle());
            Node node = loader.load();
            DocumentController controller = loader.getController();
            controller.setMenuListener(menuListener);
            String s = token != null ? token : UUID.randomUUID().toString() + System.currentTimeMillis();
            controller.initializeDocument(s, id, file, pbCoreElement, this, changesDetected);
            tab.setOnClosed(t -> {
                if (tabPane.getTabs().isEmpty()) {
                    splash.setVisible(true);
                    nonExportableProperty.setValue(true);
                }
                if (tab.getText().startsWith(UNTITLED)) {
                    untitledTabsCount.set(untitledTabsCount.get() - 1);
                }
                registry.removePBCoreElement(s);
                openedTabs.remove(controller);
            });
            tab.setOnCloseRequest(event -> {
                controller.saveDocument(true);
                if (event != null) {
                    event.consume();
                }
            });
            tab.setContent(node);
            tab.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null && newValue) {
                    currentSavableTabListener = controller;
                    controller.onShow();
                    nonExportableProperty.setValue(!currentSavableTabListener.isExportable());
                }
            });
            openedTabs.add(controller);
        }
        catch (IOException e) {
            LOGGER.log(Level.SEVERE, "could not show tab", e);
        }
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
        splash.setVisible(false);
    }

    private void showBatchTab() {

        if (batchAddController != null) {
            tabPane.getSelectionModel().select(batchAddTab);
            return;
        }
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        Tab tab = new Tab(LanguageManager.INSTANCE.getString(I18nKey.BATCH_EDIT));
        tab.getStyleClass().add("batchTab");
        tab.setId(BATCH_EDIT_ID);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/document_batch_add.fxml"),
                    LanguageManager.INSTANCE.getBundle());
            Node node = loader.load();
            batchAddController = loader.getController();
            batchAddController.setMenuListener(menuListener);
            batchAddController.initializeDocument(pbCoreElement -> {
                spinnerLayer.setVisible(true);
                openedTabs.forEach((openedTab) -> {
                    openedTab.addBatchUpdate(pbCoreElement);
                });
                tabPane.getTabs().remove(tab);
                if (tabPane.getTabs().isEmpty()) {
                    splash.setVisible(true);
                }
                registry.clearBatchEditPBCoreElement();
                batchAddTab = null;
                batchAddController = null;
                spinnerLayer.setVisible(false);
            });
            tab.setOnClosed(t -> {
                batchAddController.saveDocument();
                if (tabPane.getTabs().isEmpty()) {
                    splash.setVisible(true);
                }
                registry.clearBatchEditPBCoreElement();
                batchAddTab = null;
                batchAddController = null;
            });
            tab.setContent(node);
            batchAddTab = tab;
        }
        catch (IOException e) {
            LOGGER.log(Level.SEVERE, "could not show batch tab", e);
        }
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
        splash.setVisible(false);
    }

    @Override
    public void onFileChanged(String currentId, File file, boolean close) {

        if (!currentId.equalsIgnoreCase(file.getAbsolutePath())) {
            Tab tabToRemove = tabPane
                    .getTabs()
                    .stream()
                    .filter(tab -> tab.getId().equalsIgnoreCase(file.getAbsolutePath()))
                    .findFirst()
                    .orElse(null);
            if (tabToRemove != null) {
                tabPane.getTabs().remove(tabToRemove);
                if (tabPane.getTabs().isEmpty()) {
                    splash.setVisible(true);
                }
            }
        }
        Tab tab1 = tabPane
                .getTabs()
                .stream()
                .filter(tab -> tab.getId().equalsIgnoreCase(currentId))
                .findFirst()
                .orElse(null);
        if (tab1 != null) {
            tab1.setId(file.getAbsolutePath());
            tab1.setText(file.getName());
            if (close) {
                tabPane.getTabs().remove(tab1);
                EventHandler<Event> handler = tab1.getOnClosed();
                if (handler != null) {
                    handler.handle(null);
                }
            }
        }
    }

    @Override
    public void discardChanges(String currentId, File file) {

        Tab tab1 = tabPane
                .getTabs()
                .stream()
                .filter(tab -> tab.getId().equalsIgnoreCase(currentId))
                .findFirst()
                .orElse(null);
        if (tab1 != null) {
            tabPane.getTabs().remove(tab1);
            EventHandler<Event> handler = tab1.getOnClosed();
            if (handler != null) {
                handler.handle(null);
            }
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    @Override
    public MenuBar createMenu() {

        final MenuBar menuBar = new MenuBar();
        menuBar.setId("menuBar");
        final Menu file = new Menu(LanguageManager.INSTANCE.getString(I18nKey.FILE));
        final MenuItem open = new MenuItem(LanguageManager.INSTANCE.getString(I18nKey.OPEN));
        open.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        open.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(LanguageManager.INSTANCE.getString(I18nKey.OPEN_DOCUMENT));
            File selectedFile = fileChooser.showOpenDialog(tabPane.getScene().getWindow());
            if (selectedFile == null) {
                return;
            }
            menuOptionSelected(MenuOption.OPEN_FILE, selectedFile);
        });

        final MenuItem newd = new MenuItem(LanguageManager.INSTANCE.getString(I18nKey.NEW_DESCRIPTION_DOCUMENT));
        newd.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
        newd.setOnAction(e -> menuOptionSelected(MenuOption.NEW_DESCRIPTION_DOCUMENT));

        final MenuItem newi = new MenuItem(LanguageManager.INSTANCE.getString(I18nKey.NEW_INSTANTIATION_DOCUMENT));
        newi.setAccelerator(new KeyCodeCombination(KeyCode.I, KeyCombination.CONTROL_DOWN));
        newi.setOnAction(e -> menuOptionSelected(MenuOption.NEW_INSTANTIATION_DOCUMENT));

        final MenuItem newc = new MenuItem(LanguageManager.INSTANCE.getString(I18nKey.NEW_COLLECTION));
        newc.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.SHIFT_DOWN, KeyCombination.CONTROL_DOWN));
        newc.setOnAction(e -> menuOptionSelected(MenuOption.NEW_COLLECTION));

        final MenuItem batch = new MenuItem(LanguageManager.INSTANCE.getString(I18nKey.BATCH_EDIT_OPEN_DOCUMENTS));
        batch.setAccelerator(new KeyCodeCombination(KeyCode.B, KeyCombination.CONTROL_DOWN));
        batch.setOnAction(e -> menuOptionSelected(MenuOption.BATCH_EDIT));

        final MenuItem importFromCsv = new MenuItem(
                LanguageManager.INSTANCE.getString(I18nKey.CONVERT_CSV_FILE_TO_XML));
        importFromCsv.setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN));
        importFromCsv.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(LanguageManager.INSTANCE.getString(I18nKey.OPEN_DOCUMENT_TO_IMPORT));
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                    LanguageManager.INSTANCE.getString(I18nKey.CSV_FILES));
            fileChooser.getExtensionFilters().add(extFilter);
            File selectedFile = fileChooser.showOpenDialog(tabPane.getScene().getWindow());
            if (selectedFile == null) {
                return;
            }
            menuOptionSelected(MenuOption.CONVERT_FROM_CSV, selectedFile);
        });

        final MenuItem exportToCsv = new MenuItem(
                LanguageManager.INSTANCE.getString(I18nKey.CONVERT_OPEN_FILES_TO_CSV_FILE));
        exportToCsv.setAccelerator(new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN));
        exportToCsv.setOnAction(e -> {
            menuOptionSelected(EXPORT_OPEN_FILES_TO_CSV);
        });
        exportToCsv.disableProperty().bind(nonExportableProperty);

        final MenuItem export = new MenuItem(LanguageManager.INSTANCE.getString(I18nKey.EXPORT_OPEN_FILES_TO_ZIP));
        export.setAccelerator(new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN));
        export.setOnAction(e -> menuOptionSelected(MenuOption.EXPORT_OPEN_FILES_TO_ZIP));

        final MenuItem save = new MenuItem(LanguageManager.INSTANCE.getString(I18nKey.SAVE));
        save.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        save.setOnAction(e -> menuOptionSelected(MenuOption.SAVE));

        final MenuItem saveas = new MenuItem(LanguageManager.INSTANCE.getString(I18nKey.SAVE_AS));
        saveas
                .setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHIFT_DOWN,
                        KeyCombination.CONTROL_DOWN));
        saveas.setOnAction(e -> menuOptionSelected(MenuOption.SAVE_AS));

        final MenuItem saveAsTemplate = new MenuItem(LanguageManager.INSTANCE.getString(I18nKey.SAVE_AS_TEMPLATE));
        saveAsTemplate
                .setAccelerator(new KeyCodeCombination(KeyCode.T, KeyCombination.SHIFT_DOWN,
                        KeyCombination.CONTROL_DOWN));
        saveAsTemplate.setOnAction(e -> menuOptionSelected(MenuOption.SAVE_AS_TEMPLATE));

        final MenuItem quit = new MenuItem(LanguageManager.INSTANCE.getString(I18nKey.QUIT));
        quit.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));
        quit.setOnAction(e -> menuOptionSelected(MenuOption.QUIT));
        file
                .getItems()
                .addAll(open, new SeparatorMenuItem(), newd, newi, newc, new SeparatorMenuItem(), importFromCsv,
                        exportToCsv, new SeparatorMenuItem(), batch, export, new SeparatorMenuItem(), save, saveas,
                        saveAsTemplate, new SeparatorMenuItem(), quit);

        search = new Menu(LanguageManager.INSTANCE.getString(I18nKey.SEARCH));
        onSavedSearchesUpdated();
        registry.addSavedSearchesListener(this);

        final Menu settings = new Menu(LanguageManager.INSTANCE.getString(I18nKey.SETTINGS));
        final MenuItem cv = new MenuItem(LanguageManager.INSTANCE.getString(I18nKey.CONTROLLED_VOCABULARIES));
        final MenuItem folders = new MenuItem(LanguageManager.INSTANCE.getString(I18nKey.DIRECTORY_CRAWLING));
        cv.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.SHIFT_DOWN, KeyCombination.CONTROL_DOWN));
        cv.setOnAction(e -> menuOptionSelected(MenuOption.CONTROLLED_VOCABULARIES));
        folders.setOnAction(e -> menuOptionSelected(MenuOption.DIRECTORY_CRAWLING));
        folders
                .setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.SHIFT_DOWN,
                        KeyCombination.CONTROL_DOWN));
        final MenuItem general = new MenuItem(LanguageManager.INSTANCE.getString(I18nKey.GENERAL_SETTINGS));
        general.setOnAction(e -> menuOptionSelected(MenuOption.GENERAL_SETTINGS));
        general
                .setAccelerator(new KeyCodeCombination(KeyCode.G, KeyCombination.SHIFT_DOWN,
                        KeyCombination.CONTROL_DOWN));
        settings.getItems().addAll(cv, folders, general);

        final Menu help = new Menu(LanguageManager.INSTANCE.getString(I18nKey.HELP));
        final MenuItem helpItem = new MenuItem(LanguageManager.INSTANCE.getString(I18nKey.HELP));
        helpItem.setDisable(true);
        final MenuItem aboutItem = new MenuItem(LanguageManager.INSTANCE.getString(I18nKey.ABOUT_PBCORE));
        aboutItem.setOnAction(e -> menuOptionSelected(MenuOption.ABOUT));
        help.getItems().addAll(aboutItem, helpItem);

        menuBar.getMenus().addAll(file, search, settings, help);
        if (registry.isMac()) {
            menuBar.setUseSystemMenuBar(true);
        }
        return menuBar;
    }

    @Override
    public void onShown() {

        splash.setVisible(true);
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(1000), ae -> Platform.runLater(() -> {
            registry.getPbCoreElements().forEach((key, value) -> {
                String s = registry.getCurrentWorkPagesFilenames().get(key);
                String s1 = registry.getCurrentWorkPages().get(key);
                showTab(key, new File(s == null ? s1 : s), value);
            });
            if (registry.getBatchEditPBCoreElement() != null) {
                showBatchTab();
            }
            spinnerLayer.setVisible(false);
        })));
        timeline.play();
        final KeyCombination keyCombSingle = new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN);
        final KeyCombination keyCombAll = new KeyCodeCombination(KeyCode.W, KeyCombination.SHIFT_DOWN,
                KeyCombination.CONTROL_DOWN);
        tabPane.getScene().addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (keyCombSingle.match(event)) {
                if (tabPane.selectionModelProperty().getValue().getSelectedItem().getOnCloseRequest() != null) {
                    tabPane.selectionModelProperty().getValue().getSelectedItem().getOnCloseRequest().handle(null);
                }
                else {
                    batchAddController.saveFile(null);
                }
            }
            else if (keyCombAll.match(event)) {
                ArrayList<Tab> tabs = new ArrayList<>(tabPane.getTabs());
                for (int i = tabs.size() - 1; i >= 0; i--) {
                    Tab tab = tabs.get(i);
                    tabPane.getSelectionModel().select(tab);
                    if (tab.getOnCloseRequest() != null) {
                        tab.getOnCloseRequest().handle(null);
                    }
                    else {
                        batchAddController.saveFile(null);
                    }
                }

            }
        });
    }

    @Override
    public void searchResultSelected(List<HitDocument> hitDocuments) {

        for (HitDocument hitDocument : hitDocuments) {
            showTab(null, new File(hitDocument.getFilepath()), hitDocument.getPbCoreElement().copy());
        }
    }

    @Override
    public void onSavedSearchesUpdated() {

        search.getItems().clear();
        final MenuItem newSearch = new MenuItem(LanguageManager.INSTANCE.getString(I18nKey.NEW_SEARCH));
        newSearch.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN));
        newSearch.setOnAction(e -> menuOptionSelected(MenuOption.NEW_SEARCH));
        search.getItems().addAll(newSearch, new SeparatorMenuItem());
        AtomicInteger counter = new AtomicInteger();
        registry.getSavedSearches().stream().map(luceneEngineSearchFilters -> {
            StringBuilder terms = new StringBuilder();
            int c = 1;
            for (LuceneEngineSearchFilter luceneEngineSearchFilter : luceneEngineSearchFilters) {
                terms.append(luceneEngineSearchFilter.getTerm());
                if (c++ != luceneEngineSearchFilters.size()) {
                    terms.append(", ");
                }
            }
            MenuItem menuItem = new MenuItem(terms.toString());
            KeyCode keyCode = KeyCode.valueOf("DIGIT" + String.valueOf(counter.getAndIncrement()));
            menuItem
                    .setAccelerator(new KeyCodeCombination(keyCode, KeyCombination.CONTROL_DOWN,
                            KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN));
            menuItem.setOnAction(e -> menuOptionSelected(MenuOption.SAVED_SEARCH, luceneEngineSearchFilters));
            return menuItem;
        }).forEachOrdered(menuItem -> search.getItems().add(menuItem));
    }
}
