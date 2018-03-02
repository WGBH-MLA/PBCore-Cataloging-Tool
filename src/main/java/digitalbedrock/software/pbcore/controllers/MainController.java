package digitalbedrock.software.pbcore.controllers;

import digitalbedrock.software.pbcore.MainApp;
import digitalbedrock.software.pbcore.core.PBcoreValidator;
import digitalbedrock.software.pbcore.core.models.NewDocumentType;
import digitalbedrock.software.pbcore.core.models.entity.PBCoreElement;
import digitalbedrock.software.pbcore.core.models.entity.PBCoreStructure;
import digitalbedrock.software.pbcore.listeners.*;
import digitalbedrock.software.pbcore.lucene.HitDocument;
import digitalbedrock.software.pbcore.lucene.LuceneEngineSearchFilter;
import digitalbedrock.software.pbcore.parsers.CSVPBCoreParser;
import digitalbedrock.software.pbcore.utils.Registry;
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
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static digitalbedrock.software.pbcore.listeners.MenuListener.MenuOption.EXPORT_TO_CSV;

public class MainController extends AbsController implements FileChangedListener, SearchResultListener, SavedSearchedUpdated {

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

    public MainController() {
        try {
            validator = new PBcoreValidator();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void menuOptionSelected(MenuOption menuOption, Object... objects) {
        switch (menuOption) {
            case OPEN_FILE:
                Object object = objects[0];
                openDocument((File) object);
                break;
            case BATCH_EDIT:
                batchAdd();
                break;
            case IMPORT_FROM_CSV:
                object = objects[0];
                try {
                    showTab(null, null, CSVPBCoreParser.parseFile(((File) object).getAbsolutePath()));
                } catch (Exception e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Invalid file to import");
                    alert.setHeaderText(null);
                    alert.setContentText("The selected file is not compliant with the PBCore csv template.");
                    alert.showAndWait();
                }
                break;
            case EXPORT_TO_CSV:
                if (currentSavableTabListener != null) {
                    currentSavableTabListener.exportToCsv();
                }
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
            case EXPORT_OPEN_FILES_TO_ZIP:
                exportOpenFiles();
                break;
            default:
                super.menuOptionSelected(menuOption, objects);
                break;
        }
    }

    private void exportOpenFiles() {
        spinnerLayer.setVisible(true);
        FileChooser fileChooser = new FileChooser();

        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Zip files (*.zip)", "*.zip");
        fileChooser.getExtensionFilters().add(extFilter);

        File file = fileChooser.showSaveDialog(tabPane.getScene().getWindow());
        if (file == null) {
            spinnerLayer.setVisible(false);
            return;
        }
        Registry registry = MainApp.getInstance().getRegistry();
        Map<String, PBCoreElement> map = new HashMap<>();
        registry.getPbCoreElements().forEach((key, value) -> {
            String s1 = registry.getCurrentWorkPages().get(key);
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
            PBCoreStructure.getInstance().saveFilesToZip(map, file);
        } catch (ParserConfigurationException | TransformerException | IOException ex) {
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
        } catch (SAXException | IOException e) {
            try {
                PBCoreElement pbCoreElement = PBCoreStructure.getInstance().parseFile(file);
                showTab(null, file, pbCoreElement.copy(), true);
                showWarningMessage();
            } catch (JAXBException | IllegalAccessException ex) {
                showErrorMessage();
                return;
            }
        }
        try {
            PBCoreElement pbCoreElement = PBCoreStructure.getInstance().parseFile(file);
            showTab(null, file, pbCoreElement.copy());
        } catch (JAXBException | IllegalAccessException e) {
            showErrorMessage();
        }

    }

    private void showWarningMessage() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("File modified");
        alert.setHeaderText(null);
        alert.setContentText("The selected file wasn't fully compliant with the PBCore Schema. Some information may have been lost in order to open the file's contents.");
        alert.showAndWait();
    }

    private void showErrorMessage() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Invalid File");
        alert.setHeaderText(null);
        alert.setContentText("The selected file is not a valid PBCore file");
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

    private void showTab(String token, File file, PBCoreElement pbCoreElement) {
        showTab(token, file, pbCoreElement, false);
    }

    private void showTab(String token, File file, PBCoreElement pbCoreElement, boolean changesDetected) {
        if (file != null) {
            File finalFile = file;
            Tab tab1 = tabPane.getTabs().stream().filter(tab -> tab.getId().equalsIgnoreCase(finalFile.getAbsolutePath())).findFirst().orElse(null);
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
            } else {
                title = file.getName();
                id = file.getName();
                untitledTabsCount.addAndGet(1);
                file = null;
            }
        } else {
            title = UNTITLED + String.format("%04d", untitledTabsCount.addAndGet(1));
            id = title;
        }
        Tab tab = new Tab(title);
        tab.setId(id);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/document.fxml"));
            Node node = loader.load();
            DocumentController controller = loader.getController();
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
                MainApp.getInstance().getRegistry().removePBCoreElement(s);
                openedTabs.remove(controller);
            });
            tab.setOnCloseRequest(event -> {
                controller.saveDocument(true);
                event.consume();
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
        } catch (IOException e) {
            e.printStackTrace();
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
        Tab tab = new Tab("batch edit");
        tab.getStyleClass().add("batchTab");
        tab.setId(BATCH_EDIT_ID);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/document_batch_add.fxml"));
            Node node = loader.load();
            batchAddController = loader.getController();
            batchAddController.initializeDocument(pbCoreElement -> {
                spinnerLayer.setVisible(true);
                openedTabs.forEach((openedTab) -> {
                    openedTab.addBatchUpdate(pbCoreElement);
                });
                tabPane.getTabs().remove(tab);
                if (tabPane.getTabs().isEmpty()) {
                    splash.setVisible(true);
                }
                MainApp.getInstance().getRegistry().clearBatchEditPBCoreElement();
                batchAddTab = null;
                batchAddController = null;
                spinnerLayer.setVisible(false);
            });
            tab.setOnClosed(t -> {
                batchAddController.saveDocument();
                if (tabPane.getTabs().isEmpty()) {
                    splash.setVisible(true);
                }
                MainApp.getInstance().getRegistry().clearBatchEditPBCoreElement();
                batchAddTab = null;
                batchAddController = null;
            });
            tab.setContent(node);
            batchAddTab = tab;
        } catch (IOException e) {
            e.printStackTrace();
        }
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
        splash.setVisible(false);
    }

    @Override
    public void onFileChanged(String currentId, File file, boolean close) {
        if (!currentId.equalsIgnoreCase(file.getAbsolutePath())) {
            Tab tabToRemove = tabPane.getTabs().stream().filter(tab -> tab.getId().equalsIgnoreCase(file.getAbsolutePath())).findFirst().orElse(null);
            if (tabToRemove != null) {
                tabPane.getTabs().remove(tabToRemove);
                if (tabPane.getTabs().isEmpty()) {
                    splash.setVisible(true);
                }
            }
        }
        Tab tab1 = tabPane.getTabs().stream().filter(tab -> tab.getId().equalsIgnoreCase(currentId)).findFirst().orElse(null);
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
        Tab tab1 = tabPane.getTabs().stream().filter(tab -> tab.getId().equalsIgnoreCase(currentId)).findFirst().orElse(null);
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
        final Menu file = new Menu("File");
        final MenuItem open = new MenuItem("Open...");
        open.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.META_DOWN));
        open.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open Document");
            File selectedFile = fileChooser.showOpenDialog(tabPane.getScene().getWindow());
            if (selectedFile == null) {
                return;
            }
            menuOptionSelected(MenuListener.MenuOption.OPEN_FILE, selectedFile);
        });

        final MenuItem newd = new MenuItem("New Description Document");
        newd.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.META_DOWN));
        newd.setOnAction(e -> menuOptionSelected(MenuListener.MenuOption.NEW_DESCRIPTION_DOCUMENT));

        final MenuItem newi = new MenuItem("New Instantiation Document");
        newi.setAccelerator(new KeyCodeCombination(KeyCode.I, KeyCombination.META_DOWN));
        newi.setOnAction(e -> menuOptionSelected(MenuListener.MenuOption.NEW_INSTANTIATION_DOCUMENT));

        final MenuItem newc = new MenuItem("New Collection");
        newc.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.SHIFT_DOWN, KeyCombination.META_DOWN));
        newc.setOnAction(e -> menuOptionSelected(MenuListener.MenuOption.NEW_COLLECTION));

        final MenuItem batch = new MenuItem("Batch edit open documents");
        batch.setAccelerator(new KeyCodeCombination(KeyCode.B, KeyCombination.META_DOWN));
        batch.setOnAction(e -> menuOptionSelected(MenuOption.BATCH_EDIT));

        final MenuItem importFromCsv = new MenuItem("Import from CSV file");
        importFromCsv.setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCombination.META_DOWN));
        importFromCsv.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open Document To Import");
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv");
            fileChooser.getExtensionFilters().add(extFilter);
            File selectedFile = fileChooser.showOpenDialog(tabPane.getScene().getWindow());
            if (selectedFile == null) {
                return;
            }
            menuOptionSelected(MenuOption.IMPORT_FROM_CSV, selectedFile);
        });

        final MenuItem exportToCsv = new MenuItem("Export to CSV file");
        exportToCsv.setAccelerator(new KeyCodeCombination(KeyCode.T, KeyCombination.META_DOWN));
        exportToCsv.setOnAction(e -> {
            menuOptionSelected(EXPORT_TO_CSV);
        });
        exportToCsv.disableProperty().bind(nonExportableProperty);

        final MenuItem export = new MenuItem("Export open files to ZIP");
        export.setAccelerator(new KeyCodeCombination(KeyCode.E, KeyCombination.META_DOWN));
        export.setOnAction(e -> menuOptionSelected(MenuOption.EXPORT_OPEN_FILES_TO_ZIP));

        final MenuItem save = new MenuItem("Save");
        save.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.META_DOWN));

        final MenuItem saveas = new MenuItem("Save as...");
        save.setOnAction(e -> menuOptionSelected(MenuListener.MenuOption.SAVE));
        saveas.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHIFT_DOWN, KeyCombination.META_DOWN));
        saveas.setOnAction(e -> menuOptionSelected(MenuListener.MenuOption.SAVE_AS));

        final MenuItem quit = new MenuItem("Quit");
        quit.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.META_DOWN));
        quit.setOnAction(e -> menuOptionSelected(MenuOption.QUIT));
        file.getItems().addAll(open, new SeparatorMenuItem(), newd, newi, newc, new SeparatorMenuItem(), importFromCsv, exportToCsv, new SeparatorMenuItem(), batch, export, new SeparatorMenuItem(), save, saveas, new SeparatorMenuItem(), quit);

        search = new Menu("Search");
        onSavedSearchesUpdated();
        Registry registry = MainApp.getInstance().getRegistry();
        registry.addSavedSearchesListener(this);

        final Menu settings = new Menu("Settings");
        final MenuItem cv = new MenuItem("Controlled Vocabularies");
        final MenuItem folders = new MenuItem("Directory Crawling");
        cv.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.SHIFT_DOWN, KeyCombination.META_DOWN));
        cv.setOnAction(e -> menuOptionSelected(MenuOption.CONTROLLED_VOCABULARIES));
        folders.setOnAction(e -> menuOptionSelected(MenuOption.DIRECTORY_CRAWLING));
        folders.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.SHIFT_DOWN, KeyCombination.META_DOWN));
        settings.getItems().addAll(cv, folders);

        final Menu help = new Menu("Help");
        help.setDisable(true);

        menuBar.getMenus().addAll(file, search, settings, help);
        if (MainApp.getInstance().getRegistry().isMac()) {
            menuBar.setUseSystemMenuBar(true);
        }
        return menuBar;
    }

    @Override
    public void onShown() {
        splash.setVisible(true);
        Timeline timeline = new Timeline(new KeyFrame(
                Duration.millis(1000),
                ae -> Platform.runLater(() -> {
                    Registry registry = MainApp.getInstance().getRegistry();
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
    }

    @Override
    public void searchResultSelected(HitDocument hitDocument) {
        showTab(null, new File(hitDocument.getFilepath()), hitDocument.getPbCoreElement().copy());
    }

    @Override
    public void onSavedSearchesUpdated() {
        search.getItems().clear();
        final MenuItem newSearch = new MenuItem("New Search");
        newSearch.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.META_DOWN));
        newSearch.setOnAction(e -> menuOptionSelected(MenuOption.NEW_SEARCH));
        search.getItems().addAll(newSearch, new SeparatorMenuItem());
        Registry registry = MainApp.getInstance().getRegistry();
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
            menuItem.setAccelerator(new KeyCodeCombination(keyCode, KeyCombination.META_DOWN, KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN));
            menuItem.setOnAction(e -> menuOptionSelected(MenuOption.SAVED_SEARCH, luceneEngineSearchFilters));
            return menuItem;
        }).forEachOrdered(menuItem -> search.getItems().add(menuItem));
    }
}
