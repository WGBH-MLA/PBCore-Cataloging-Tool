package digitalbedrock.software.pbcore.components;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.TreeCell;

import digitalbedrock.software.pbcore.controllers.DocumentAttributeItemController;
import digitalbedrock.software.pbcore.core.models.entity.PBCoreAttribute;
import digitalbedrock.software.pbcore.utils.LanguageManager;

public class PBCoreAttributeTreeCell extends TreeCell<PBCoreAttribute> {

    private final AttributeTreeCellListener attributeTreeCellListener;
    private final DocumentAttributeItemController.DocumentAttributeSelectCVListener listener;

    public PBCoreAttributeTreeCell(AttributeTreeCellListener attributeTreeCellListener,
                                   DocumentAttributeItemController.DocumentAttributeSelectCVListener listener) {

        this.attributeTreeCellListener = attributeTreeCellListener;
        this.listener = listener;

    }

    @Override
    protected void updateItem(PBCoreAttribute attribute, boolean empty) {

        super.updateItem(attribute, empty);
        if (empty) {
            setGraphic(null);
        }
        else {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/document_attribute_list_item.fxml"),
                        LanguageManager.INSTANCE.getBundle());
                Node graphic = loader.load();
                DocumentAttributeItemController controller = loader.getController();
                controller.bind(attribute, attributeTreeCellListener, listener);
                setGraphic(graphic);
            }
            catch (IOException exc) {
                throw new RuntimeException(exc);
            }
        }
    }

    public interface AttributeTreeCellListener {

        void onRemoveAttribute(PBCoreAttribute pbCoreAttribute);
    }
}
