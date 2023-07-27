package digitalbedrock.software.pbcore.components.editor;

import java.util.Objects;
import javafx.fxml.FXML;
import javafx.scene.text.Text;

import digitalbedrock.software.pbcore.core.models.entity.IPBCoreLayoutType;
import digitalbedrock.software.pbcore.core.models.entity.PBCoreElement;
import digitalbedrock.software.pbcore.utils.I18nKey;
import digitalbedrock.software.pbcore.utils.LanguageManager;

public class ElementInstantiationVisualLayoutItemController {

    @FXML
    private Text previewElementLabel;
    @FXML
    private Text previewElementMediaType;
    @FXML
    private Text previewElementFormat;

    public void bind(PBCoreElement element) {

        if (element == null || element.getTypeForLayout() != IPBCoreLayoutType.INSTANTIATION) {
            return;
        }
        String pbcoreInstantiationMediaType = element
                .getOrderedSubElements()
                .stream()
                .filter(pbCoreElement -> Objects.equals(pbCoreElement.getName(), "instantiationMediaType"))
                .map(PBCoreElement::getValue)
                .findFirst()
                .orElse(null);
        String pbcoreInstantiationDigital = element
                .getOrderedSubElements()
                .stream()
                .filter(pbCoreElement -> Objects.equals(pbCoreElement.getName(), "instantiationDigital"))
                .map(PBCoreElement::getValue)
                .findFirst()
                .orElse(null);

        String pbcoreInstantiationPhysical = element
                .getOrderedSubElements()
                .stream()
                .filter(pbCoreElement -> Objects.equals(pbCoreElement.getName(), "instantiationPhysical"))
                .map(PBCoreElement::getValue)
                .findFirst()
                .orElse(null);

        previewElementMediaType
                .setText(" " + (pbcoreInstantiationMediaType == null ? "" : pbcoreInstantiationMediaType));
        if (pbcoreInstantiationDigital != null) {
            previewElementLabel.setText(LanguageManager.INSTANCE.getString(I18nKey.DIGITAL));
            previewElementFormat.setText(" (" + pbcoreInstantiationDigital + ")");
        }
        else if (pbcoreInstantiationPhysical != null) {
            previewElementLabel.setText(LanguageManager.INSTANCE.getString(I18nKey.PHYSICAL));
            previewElementFormat.setText(" (" + pbcoreInstantiationPhysical + ")");
        }
        else {
            previewElementLabel.setText(null);
            previewElementFormat.setText("()");
        }
    }
}
