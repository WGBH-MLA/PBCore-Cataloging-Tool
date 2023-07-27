package digitalbedrock.software.pbcore.utils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Utility {

    private static final Logger LOGGER = Logger.getLogger(Utility.class.getName());

    public static void showInExplorer(String filePath) {

        if (PBCoreUtils.isWindows()) {
            try {
                Runtime.getRuntime().exec(new String[] { "explorer.exe /select," + filePath });
            }
            catch (IOException e) {
                LOGGER.log(Level.WARNING, "windows - could not show in explorer", e);
            }
        }
        else if (PBCoreUtils.isMac()) {
            try {
                Runtime.getRuntime().exec(new String[] { "open --reveal " + filePath });
            }
            catch (IOException e) {
                LOGGER.log(Level.WARNING, "mac - could not show in explorer", e);
            }
        }
        else {
            final Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    File file = new File(filePath);
                    desktop.open(file.getParentFile());
                }
                catch (IOException e) {
                    LOGGER.log(Level.WARNING, "could not show in explorer", e);
                }
            }
        }
    }
}
