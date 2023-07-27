package digitalbedrock.software.pbcore.components.editor;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.text.StringEscapeUtils;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import digitalbedrock.software.pbcore.core.PBcoreValidator;
import digitalbedrock.software.pbcore.core.models.entity.IPBCore;
import digitalbedrock.software.pbcore.core.models.entity.PBCoreElement;
import digitalbedrock.software.pbcore.core.models.entity.PBCoreElementAnyValue;
import digitalbedrock.software.pbcore.utils.LanguageManager;
import digitalbedrock.software.pbcore.utils.Registry;

public class AceEditor extends AnchorPane {

    public static final Logger LOGGER = Logger.getLogger(AceEditor.class.getName());
    public static final String FILE_PROTOCOL = "file://";
    public static final String ACE_EDITOR_COMMAND = "test('%s')";

    private PBcoreValidator validator;

    private EditorOpenedStateListener editorOpenedStateListener;
    @FXML
    private WebView webView;
    @FXML
    private ListView<IPBCore> treeViewPreview;
    @FXML
    private Button closeButton;
    @FXML
    private FontIcon toggleIcon;
    @FXML
    private GridPane topBar;
    @FXML
    private RadioButton toggleXml;
    @FXML
    private RadioButton toggleVisual;
    @FXML
    private ToggleGroup typeRadio;

    private final SimpleStringProperty text;

    private final AtomicBoolean opened = new AtomicBoolean(true);
    private final BooleanProperty showCloseButton = new SimpleBooleanProperty(true);

    private PBCoreElement pbCoreElement;

    public AceEditor() {

        try {
            validator = new PBcoreValidator();
        }
        catch (SAXException e) {
            LOGGER.log(Level.SEVERE, "error instantiating PBCoreValidator", e);
        }
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/editor.fxml"),
                LanguageManager.INSTANCE.getBundle());
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        }
        catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        toggleXml.getStyleClass().remove("radio-button");
        toggleXml.getStyleClass().add("toggle-button");
        toggleVisual.getStyleClass().remove("radio-button");
        toggleVisual.getStyleClass().add("toggle-button");

        typeRadio.selectedToggleProperty().addListener((observable, oldValue, newValue) -> updateContent(newValue));

        final WebEngine webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);
        text = new SimpleStringProperty("");
        text.addListener((observableValue, oldValue, newValue) -> {
            if (newValue != null && Arrays
                    .asList(Worker.State.SUCCEEDED, Worker.State.RUNNING)
                    .contains(webEngine.getLoadWorker().stateProperty().getValue())) {
                webEngine
                        .executeScript(String.format(ACE_EDITOR_COMMAND, StringEscapeUtils.escapeEcmaScript(newValue)));
            }
        });
        initialize(webView);

        closeButton.setOnAction(event -> {
            if (opened.get()) {
                getStyleClass().add("closed");
                toggleIcon.setIconCode(MaterialDesign.MDI_CHEVRON_UP);
                double heightToHide = getHeight() - topBar.getHeight();
                double v2 = getHeight() - Math.min(heightToHide, 50);
                setTranslateY(getTranslateY() + v2);
                if (editorOpenedStateListener != null) {
                    editorOpenedStateListener.onEditorClosed();
                    opened.set(false);
                }
            }
            else {
                getStyleClass().remove("closed");
                toggleIcon.setIconCode(MaterialDesign.MDI_CHEVRON_DOWN);
                setTranslateY(0);
                if (editorOpenedStateListener != null) {
                    editorOpenedStateListener.onEditorOpened();
                    opened.set(true);
                }
            }
            updateContent(typeRadio.getSelectedToggle());
        });
        showCloseButton
                .addListener((observable, oldValue, newValue) -> closeButton.setVisible(newValue != null && newValue));
        treeViewPreview.setCellFactory(lv -> new IPBCorePreviewItemListCell(null));
    }

    private void updateContent(Toggle newValue) {

        if (newValue != null && opened.get()) {
            if (newValue.equals(toggleXml)) {
                updateXmlPreview();
                webView.setVisible(true);
                treeViewPreview.setVisible(false);
            }
            else if (newValue.equals(toggleVisual)) {
                webView.setVisible(false);
                treeViewPreview.setVisible(true);
                updateVisualLayoutPreview();
            }
        }
        else {
            webView.setVisible(false);
            treeViewPreview.setVisible(false);
        }
    }

    private void initialize(WebView webView) {

        loadContent(webView);
    }

    private void loadContent(WebView webView) {

        WebEngine engine = webView.getEngine();
        engine.load(FILE_PROTOCOL + Registry.verifyAndRetrieveAceEditorHtmlResourceFile());
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.FAILED) {
                LOGGER.log(Level.WARNING, "preview failed to load", engine.getLoadWorker().getException());
            }
            else if (newState == Worker.State.SUCCEEDED) {
                try {
                    String textValue = text.getValue() == null ? "" : text.getValue();
                    engine
                            .executeScript(String
                                    .format(ACE_EDITOR_COMMAND, StringEscapeUtils.escapeEcmaScript(textValue)));
                }
                catch (Exception e) {
                    LOGGER.log(Level.FINE, "error loading preview", e);
                }
            }
        });
    }

    public Node getNode() {

        return webView;
    }

    public StringProperty textProperty() {

        return text;
    }

    public String getText() {

        return text.get();
    }

    public void setEditorOpenedStateListener(EditorOpenedStateListener editorOpenedStateListener) {

        this.editorOpenedStateListener = editorOpenedStateListener;
    }

    public void updatePreview(PBCoreElement value) {

        this.pbCoreElement = value;
        updateContent(typeRadio.getSelectedToggle());
    }

    private void updateVisualLayoutPreview() {

        treeViewPreview.setItems(FXCollections.emptyObservableList());
        List<IPBCore> flatList = new ArrayList<>();
        AtomicInteger index = new AtomicInteger(0);
        getFlatTree(index, flatList, pbCoreElement);
        treeViewPreview.setItems(FXCollections.observableArrayList(flatList));
    }

    private void updateXmlPreview() {

        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            Element element = processElement(doc, pbCoreElement);
            if (element == null) {
                return;
            }

            doc.appendChild(element);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(doc);
            StringWriter stringWriter = new StringWriter();
            StreamResult result = new StreamResult(stringWriter);
            transformer.transform(source, result);

            String textValue = stringWriter.toString();
            textProperty().setValue(textValue);
            try {
                validator.validate(textValue);
            }
            catch (SAXException | IOException e) {
                LOGGER.log(Level.SEVERE, "could not validate xml", e);
            }
        }
        catch (ParserConfigurationException | TransformerException e) {
            LOGGER.log(Level.SEVERE, "could not parse xml", e);
        }
    }

    private void getFlatTree(AtomicInteger index, List<IPBCore> listToAdd, PBCoreElement rootElement) {

        int idx = index.get() + 1;
        if (rootElement == null) {
            return;
        }
        rootElement.setIndex(idx);
        listToAdd.add(rootElement);
        rootElement.getAttributes().forEach(pbCoreAttribute -> pbCoreAttribute.setIndex(idx));
        if (idx != 1) {
            listToAdd.addAll(rootElement.getAttributes());
        }
        index.incrementAndGet();
        rootElement
                .getOrderedSubElements()
                .forEach((coreElement) -> getFlatTree(new AtomicInteger(idx), listToAdd, coreElement));
    }

    private Element processElement(Document doc, PBCoreElement value) {

        if (value == null) {
            return null;
        }
        Element element = doc.createElement(value.getName());
        if (!value.isAnyElement()) {
            element.appendChild(doc.createTextNode(value.getValue() == null ? "" : value.getValue()));
        }
        else {
            for (PBCoreElementAnyValue s : value.getAnyValues()) {
                String valueStr = s == null || s.getValue() == null || s.getValue().trim().isEmpty() ? ""
                        : s.getValue();
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder;
                try {
                    builder = factory.newDocumentBuilder();
                    InputSource inputSource = new InputSource(new StringReader(valueStr));
                    inputSource.setEncoding("UTF-8");
                    Document document = builder.parse(inputSource);
                    element.appendChild(doc.importNode(document.getDocumentElement(), true));
                }
                catch (Exception e) {
                    LOGGER.log(Level.WARNING, "could not process element", e);
                }
            }
        }
        value
                .getAttributes()
                .forEach((pbCoreAttribute) -> element
                        .setAttribute(pbCoreAttribute.getName(), pbCoreAttribute.getValue()));
        value.getOrderedSubElements().forEach((pbElement) -> element.appendChild(processElement(doc, pbElement)));
        return element;
    }

    public void reload() {

        if (webView.getEngine().getLoadWorker().stateProperty().getValue() != Worker.State.SUCCEEDED) {
            loadContent(webView);
        }
    }

    public interface EditorOpenedStateListener {

        void onEditorClosed();

        void onEditorOpened();
    }

    public void open() {

        opened.set(false);
        closeButton.fire();
    }

    public void close() {

        opened.set(true);
        closeButton.fire();
    }

    public boolean isShowCloseButton() {

        return showCloseButton.get();
    }

    public BooleanProperty showCloseButtonProperty() {

        return showCloseButton;
    }

    public void setShowCloseButton(boolean showCloseButton) {

        this.showCloseButton.set(showCloseButton);
    }
}
