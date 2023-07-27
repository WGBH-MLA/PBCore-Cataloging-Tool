package digitalbedrock.software.pbcore.components.editor;

import static digitalbedrock.software.pbcore.components.editor.AceEditor.FILE_PROTOCOL;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;
import org.w3c.dom.Document;

import digitalbedrock.software.pbcore.utils.Registry;

@ExtendWith(ApplicationExtension.class)
class AceEditorTest {

    private AceEditor aceEditor;

    @Start
    private void start(Stage stage) throws IOException {

        aceEditor = new AceEditor();
        VBox parent = new VBox();
        parent.getChildren().addAll(aceEditor);
        stage.setScene(new Scene(parent, 1080, 920));
        stage.show();
    }

    @Test
    void aceEditorInXmlModeAndWebViewInRunningState_textUpdated_expectedTextShownInWebView(FxRobot robot)
            throws TransformerException, TimeoutException {

        var webView = robot.lookup("#webView").queryAs(WebView.class);

        var expectedString = "axxx";
        WaitForAsyncUtils
                .waitFor(2, TimeUnit.SECONDS,
                         () -> !getElementContent(webView.getEngine().getDocument()).contains(expectedString));

        String newValue = "<axxx></axxx>";
        robot.interact(() -> {
            webView.getEngine().load(FILE_PROTOCOL + Registry.verifyAndRetrieveAceEditorHtmlResourceFile());
            assertEquals(Worker.State.RUNNING, webView.getEngine().getLoadWorker().getState());
            aceEditor.textProperty().setValue(newValue);
        });
        WaitForAsyncUtils
                .waitFor(2, TimeUnit.SECONDS,
                         () -> getElementContent(webView.getEngine().getDocument()).contains(expectedString));

    }

    @Test
    void aceEditorInXmlModeAndWebViewInSucceededState_textUpdated_expectedTextShownInWebView(FxRobot robot)
            throws TimeoutException {

        var webView = robot.lookup("#webView").queryAs(WebView.class);

        var expectedString = "axxx";
        WaitForAsyncUtils
                .waitFor(2, TimeUnit.SECONDS,
                         () -> !getElementContent(webView.getEngine().getDocument()).contains(expectedString));

        String newValue = "<axxx></axxx>";
        robot.interact(() -> {
            assertEquals(Worker.State.SUCCEEDED, webView.getEngine().getLoadWorker().getState());
            aceEditor.textProperty().setValue(newValue);
        });
        WaitForAsyncUtils
                .waitFor(2, TimeUnit.SECONDS,
                         () -> getElementContent(webView.getEngine().getDocument()).contains(expectedString));

    }

    private String getElementContent(Document element) throws TransformerException {

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(element), new StreamResult(writer));
        return writer.getBuffer().toString();
    }
}
