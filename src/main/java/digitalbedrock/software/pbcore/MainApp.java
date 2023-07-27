package digitalbedrock.software.pbcore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import digitalbedrock.software.pbcore.controllers.*;
import digitalbedrock.software.pbcore.core.Settings;
import digitalbedrock.software.pbcore.core.models.FolderModel;
import digitalbedrock.software.pbcore.core.models.entity.PBCoreAttribute;
import digitalbedrock.software.pbcore.core.models.entity.PBCoreElement;
import digitalbedrock.software.pbcore.core.models.entity.PBCoreStructure;
import digitalbedrock.software.pbcore.listeners.*;
import digitalbedrock.software.pbcore.lucene.HitDocument;
import digitalbedrock.software.pbcore.lucene.LuceneEngineSearchFilter;
import digitalbedrock.software.pbcore.lucene.LuceneIndexer;
import digitalbedrock.software.pbcore.utils.I18nKey;
import digitalbedrock.software.pbcore.utils.Language;
import digitalbedrock.software.pbcore.utils.LanguageManager;
import digitalbedrock.software.pbcore.utils.Registry;

public class MainApp extends Application implements MenuListener, LanguageChangeListener {

    public static final Logger LOGGER = Logger.getLogger(MainApp.class.getName());
    private Stage stage;

    private static MainApp instance;
    private SearchResultListener searchResultListener;
    private final Registry registry;

    private Stage searchStage;
    private Stage settingsStage;

    public MainApp() throws IOException {

        instance = this;
        this.registry = new Registry();
    }

    public static MainApp getInstance() {

        return instance;
    }

    /**
     * @param args
     *            the command line arguments
     */
    public static void main(String[] args) {

        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        try {
            stage = primaryStage;
            stage.setTitle(LanguageManager.INSTANCE.getString(I18nKey.APP_TITLE));

            PBCoreStructure.getInstance();
            loadInitialScreen();
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        Settings settings = registry.getSettings();
        try {
            new Task<Boolean>() {

                @Override
                protected Boolean call() throws Exception {

                    settings.getFolders().stream().filter(FolderModel::isIndexing).forEach(folderModel -> {
                        folderModel.setTotalValidFiles(0);
                        LuceneIndexer.getInstance().startFolderIndexing(folderModel.getFolderPath());
                        settings.updateFolder(folderModel);
                    });
                    settings
                            .getFolders()
                            .stream()
                            .filter(FolderModel::isScheduled)
                            .forEach(folderModel -> LuceneIndexer
                                    .getInstance()
                                    .startFolderIndexing(folderModel.getFolderPath()));
                    return null;
                }
            }.call();
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "exception indexing folders", e);
        }
    }

    private void loadInitialScreen() {

        goToMainScreen();
        stage.show();
        if (registry.getSettings().getFolders().isEmpty()) {
            showSettings(true, 1);
        }
    }

    public Registry getRegistry() {

        return registry;
    }

    private void showSettings(boolean block, int tab) {

        if (settingsStage != null) {
            settingsStage.toFront();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings.fxml"),
                    LanguageManager.INSTANCE.getBundle());
            Parent tabs = loader.load();
            ((TabPane) tabs.lookup("#tabs")).getSelectionModel().select(tab);

            final BorderPane borderPane = new BorderPane();
            SettingsController controller = loader.getController();
            controller.setLanguageChangeListener(this);
            controller.setMenuListener(this);
            borderPane.setTop(controller.createMenu());
            borderPane.setCenter(tabs);

            Scene settingsScene = new Scene(borderPane);
            Stage settingsWindow = new Stage();
            settingsWindow.initOwner(settingsScene.getWindow());
            settingsWindow.initModality(block ? Modality.APPLICATION_MODAL : Modality.WINDOW_MODAL);
            settingsWindow.setTitle(LanguageManager.INSTANCE.getString(I18nKey.SETTINGS));
            settingsWindow.setScene(settingsScene);
            settingsWindow.show();
            settingsWindow.setOnCloseRequest(event -> settingsStage = null);
            settingsStage = settingsWindow;
            settingsStage.toFront();
        }
        catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private void showSearch() {

        showSearch(null);
    }

    private void showSearch(List<LuceneEngineSearchFilter> filters) {

        if (searchStage != null) {
            searchStage.toFront();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/search.fxml"),
                    LanguageManager.INSTANCE.getBundle());
            Parent tabs = loader.load();

            final BorderPane borderPane = new BorderPane();
            SearchController controller = loader.getController();
            if (filters != null) {
                controller.setFilters(filters);
            }
            controller.setMenuListener(this);
            borderPane.setTop(controller.createMenu());
            borderPane.setCenter(tabs);

            Scene settingsScene = new Scene(borderPane);
            Stage settingsWindow = new Stage();
            settingsWindow.initOwner(settingsScene.getWindow());
            settingsWindow.initModality(Modality.WINDOW_MODAL);
            settingsWindow.setTitle(LanguageManager.INSTANCE.getString(I18nKey.SEARCH));
            settingsWindow.setScene(settingsScene);
            settingsWindow.show();
            settingsWindow.setOnCloseRequest(event -> searchStage = null);
            searchStage = settingsWindow;
        }
        catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private void showAbout() {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/about.fxml"),
                    LanguageManager.INSTANCE.getBundle());
            Parent tabs = loader.load();

            final BorderPane borderPane = new BorderPane();
            loader.getController();
            borderPane.setCenter(tabs);

            Scene settingsScene = new Scene(borderPane);
            Stage settingsWindow = new Stage();
            settingsWindow.initOwner(settingsScene.getWindow());
            settingsWindow.initModality(Modality.APPLICATION_MODAL);
            settingsWindow.setTitle(LanguageManager.INSTANCE.getString(I18nKey.ABOUT));
            settingsWindow.setScene(settingsScene);
            settingsWindow.show();
        }
        catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    public void showSelectAttribute(PBCoreElement pbCoreElement,
                                    AttributeSelectionListener attributeSelectionListener) {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/attribute_selector.fxml"),
                    LanguageManager.INSTANCE.getBundle());
            Parent parent = loader.load();
            AttributeSelectorController controller = loader.getController();
            controller.setPbCoreElement(pbCoreElement);
            Scene searchScene = new Scene(parent);
            Stage searchWindow = new Stage();
            searchWindow.initOwner(searchScene.getWindow());
            searchWindow.initModality(Modality.APPLICATION_MODAL);
            searchWindow.setTitle(LanguageManager.INSTANCE.getString(I18nKey.ADD_NEW_ATTRIBUTE));
            searchWindow.setScene(searchScene);
            searchWindow.show();
            controller.setAttributeSelectionListener((element, close) -> {
                if (attributeSelectionListener != null) {
                    attributeSelectionListener.onAttributeSelected(element, close);
                }
                if (close) {
                    searchWindow.close();
                }
            });
        }
        catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    public void showAddElementAnyValue(PBCoreElement pbCoreElement,
                                       AddElementAnyValueListener addElementAnyValueListener) {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/embedded_dialog.fxml"),
                    LanguageManager.INSTANCE.getBundle());
            Parent parent = loader.load();
            AddElementAnyValueController controller = loader.getController();
            Scene searchScene = new Scene(parent);
            Stage searchWindow = new Stage();
            searchWindow.initOwner(searchScene.getWindow());
            searchWindow.initModality(Modality.APPLICATION_MODAL);
            searchWindow.setTitle(LanguageManager.INSTANCE.getString(I18nKey.ADD_NEW_EMBEDDED_VALUE));
            searchWindow.setScene(searchScene);
            searchWindow.show();
            controller.setAttributeSelectionListener(element -> {
                if (addElementAnyValueListener != null) {
                    addElementAnyValueListener.onValueAdded(element);
                }
                searchWindow.close();
            });
        }
        catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private void showSelectElement(String treeViewId, int index, PBCoreElement pbCoreElement,
                                   ElementSelectionListener elementSelectionListener) {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/element_selector.fxml"),
                    LanguageManager.INSTANCE.getBundle());
            Parent parent = loader.load();
            ElementSelectorController controller = loader.getController();
            controller.setPbCoreElement(pbCoreElement);
            Scene searchScene = new Scene(parent);
            Stage selectElementWindow = new Stage();
            selectElementWindow.initOwner(stage);
            selectElementWindow.initOwner(searchScene.getWindow());
            selectElementWindow.initModality(Modality.APPLICATION_MODAL);
            selectElementWindow.setTitle(LanguageManager.INSTANCE.getString(I18nKey.ADD_NEW_ELEMENT));
            selectElementWindow.setScene(searchScene);
            selectElementWindow.show();
            controller.setElementSelectionListener(treeViewId, index, (treeViewId1, index1, element, close) -> {
                if (elementSelectionListener != null) {
                    elementSelectionListener.onElementSelected(treeViewId, index, element, close);
                }
                if (close) {
                    selectElementWindow.close();
                }
            });
        }
        catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private void showSelectCV(boolean attr, String key, CVSelectionListener cvSelectionListener) {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/cv_selector.fxml"),
                    LanguageManager.INSTANCE.getBundle());
            Parent parent = loader.load();
            CVSelectorController controller = loader.getController();
            controller.setKey(attr, key);
            Scene searchScene = new Scene(parent);
            Stage selectElementWindow = new Stage();
            selectElementWindow.initOwner(stage);
            selectElementWindow.initOwner(searchScene.getWindow());
            selectElementWindow.initModality(Modality.APPLICATION_MODAL);
            selectElementWindow.setTitle(LanguageManager.INSTANCE.getString(I18nKey.SELECT_CONTROLLED_VOCABULARY_TERM));
            selectElementWindow.setScene(searchScene);
            selectElementWindow.show();
            controller.setCVSelectionListener((key1, cvTerm, attr1) -> {
                if (cvSelectionListener != null) {
                    cvSelectionListener.onCVSelected(key1, cvTerm, attr1);
                }
                selectElementWindow.close();

            });
        }
        catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private void showSelectSearchFieldElements(int index, LuceneEngineSearchFilter luceneEngineSearchFilter,
                                               SearchFilterElementsSelectionListener elementSelectionListener) {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/search_filter_elements_selector.fxml"),
                    LanguageManager.INSTANCE.getBundle());
            Parent parent = loader.load();
            SearchFilterElementsSelectorController controller = loader.getController();
            controller.setSearchFilter(luceneEngineSearchFilter);
            Scene searchScene = new Scene(parent);
            Stage selectElementWindow = new Stage();
            selectElementWindow.initOwner(stage);
            selectElementWindow.initOwner(searchScene.getWindow());
            selectElementWindow.initModality(Modality.APPLICATION_MODAL);
            selectElementWindow.setTitle(LanguageManager.INSTANCE.getString(I18nKey.SELECT_FIELDS_TO_SEARCH));
            selectElementWindow.setScene(searchScene);
            selectElementWindow.show();
            controller.setElementSelectionListener(index, (index1, element) -> {
                if (elementSelectionListener != null) {
                    elementSelectionListener.onFiltersDefined(index1, element);
                }
                selectElementWindow.close();
            });
        }
        catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    public void goTo(MenuListener.MenuOption menuOption, Object... objects) {

        switch (menuOption) {
            case OPEN_FILE:
            case NEW_DESCRIPTION_DOCUMENT:
            case NEW_INSTANTIATION_DOCUMENT:
            case SAVE:
            case SAVE_AS:
            case SAVE_AS_TEMPLATE:
            case CONVERT_FROM_CSV:
            case EXPORT_SEARCHED_FILES_TO_CSV:
            case NEW_COLLECTION:
            case BATCH_EDIT:
            case EXPORT_OPEN_FILES_TO_ZIP:
                goToMainScreen();
                break;
            case QUIT:
                quit();
                break;
            case NEW_SEARCH:
                showSearch();
                break;
            case ABOUT:
                showAbout();
                break;
            case SAVED_SEARCH:
                List<LuceneEngineSearchFilter> filters = new ArrayList<>();
                if (objects[0] != null && objects[0] instanceof Collection) {
                    ((Collection) objects[0])
                            .stream()
                            .filter((o) -> (o instanceof LuceneEngineSearchFilter))
                            .forEachOrdered((o) -> {
                                filters.add((LuceneEngineSearchFilter) o);
                            });
                }
                showSearch(filters);
                break;
            case CONTROLLED_VOCABULARIES:
                showSettings(false, 0);
                break;
            case DIRECTORY_CRAWLING:
                showSettings(false, 1);
                break;
            case GENERAL_SETTINGS:
                showSettings(false, 2);
                break;
            case HELP:
                break;
            case SELECT_SEARCH_FILTER_ELEMENTS:
                showSelectSearchFieldElements((int) objects[0], (LuceneEngineSearchFilter) objects[1],
                                              (SearchFilterElementsSelectionListener) objects[2]);
                break;
            case SELECT_ELEMENT:
                showSelectElement((String) objects[0], (int) objects[1], (PBCoreElement) objects[2],
                                  (ElementSelectionListener) objects[3]);
                break;
            case SELECT_CV_ELEMENT:
                showSelectCV(false, ((PBCoreElement) objects[0]).getName(), (CVSelectionListener) objects[1]);
                break;
            case SELECT_CV_ATTRIBUTE:
                showSelectCV(true, ((PBCoreAttribute) objects[0]).getName(), (CVSelectionListener) objects[1]);
                break;
            case SELECT_ATTRIBUTE:
                showSelectAttribute((PBCoreElement) objects[0], (AttributeSelectionListener) objects[1]);
                break;
            case ADD_ELEMENT_ANY_VALUE:
                showAddElementAnyValue((PBCoreElement) objects[0], (AddElementAnyValueListener) objects[1]);
                break;
            case SEARCH_RESULT_SELECTED:
                if (searchResultListener != null) {
                    searchResultListener.searchResultSelected((List<HitDocument>) objects[0]);
                }
                break;
        }
    }

    private void goToMainScreen() {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"),
                    LanguageManager.INSTANCE.getBundle());
            Parent page = null;
            try {
                page = loader.load();
                MainController mainController = loader.getController();
                mainController.setMenuListener(this);
                mainController.setRegistry(getRegistry());
            }
            catch (IOException ioe) {
                LOGGER.log(Level.SEVERE, "could not load layout", ioe);
            }
            Scene scene = stage.getScene();

            final BorderPane borderPane = new BorderPane();
            AbsController controller = loader.getController();
            if (controller instanceof SearchResultListener) {
                searchResultListener = (SearchResultListener) controller;
            }
            controller.setMenuListener(this);
            borderPane.setTop(controller.createMenu());
            borderPane.setCenter(page);

            if (scene == null) {
                scene = new Scene(borderPane);
                stage.setMinWidth(800);
                stage.setMinHeight(600);
                stage.setScene(scene);
                stage.setTitle(LanguageManager.INSTANCE.getString(I18nKey.APP_TITLE));
                stage.setOnShown(event -> controller.onShown());
                stage.show();
            }
            else {
                stage.getScene().setRoot(page);
            }
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private void quit() {

        Platform.exit();
    }

    @Override
    public void onLanguageChanged(Language language) {

        PBCoreStructure.getInstance().reloadStructure();
        registry.updateLanguage(language);
        if (settingsStage != null) {
            settingsStage.close();
            settingsStage = null;
        }
        if (searchStage != null) {
            searchStage.close();
            searchStage = null;
        }
        if (stage != null) {
            stage.close();
            stage.setScene(null);
        }
        loadInitialScreen();
    }

    @Override
    public void menuOptionSelected(MenuOption menuOption, Object... objects) {

        goTo(menuOption, objects);
    }
}
