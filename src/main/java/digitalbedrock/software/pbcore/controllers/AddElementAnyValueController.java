package digitalbedrock.software.pbcore.controllers;

import java.io.StringReader;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.InputSource;

import digitalbedrock.software.pbcore.core.models.entity.PBCoreElementAnyValue;
import digitalbedrock.software.pbcore.listeners.AddElementAnyValueListener;

public class AddElementAnyValueController extends AbsController {

    @FXML
    private TextArea taValue;
    @FXML
    private Button btnCancel;
    @FXML
    private Button btnAdd;
    @FXML
    private Label lblAttributeAlreadyAdded;

    private PBCoreElementAnyValue pbCoreElementAnyValue;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        pbCoreElementAnyValue = new PBCoreElementAnyValue();
        lblAttributeAlreadyAdded.setVisible(false);
    }

    public void setAttributeSelectionListener(AddElementAnyValueListener addAnyValueListener) {

        btnCancel.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> addAnyValueListener.onValueAdded(null));
        btnAdd.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            String text = taValue.getText();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder;
            try {
                builder = factory.newDocumentBuilder();
                InputSource inputSource = new InputSource(new StringReader(text));
                inputSource.setEncoding("UTF-8");
                builder.parse(inputSource);
                pbCoreElementAnyValue.setValue(text);
                lblAttributeAlreadyAdded.setVisible(false);
                addAnyValueListener.onValueAdded(pbCoreElementAnyValue);
            }
            catch (Exception e) {
                lblAttributeAlreadyAdded.setVisible(true);
            }
        });
        Platform.runLater(() -> btnAdd.requestFocus());
    }

    @Override
    public MenuBar createMenu() {

        return new MenuBar();
    }

}
