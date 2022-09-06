package wallpaperslideshow;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.wm.impl.IdeBackgroundUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;


public class SettingsPage implements StartupActivity, Configurable, ActionListener {
    public static Worker worker;
    private JPanel panel;
    private TextFieldWithBrowseButton fileTextBox;
    private final JFileChooser fileChooser = new JFileChooser();
    private JSpinner intervalSpinner;
    private JCheckBox randomCheckBox;
    private JButton reloadFolderButton;
    private JCheckBox directoryWalkCheckBox;
    private final SlideshowStorage slideshowStorage = SlideshowStorage.getInstance();
    FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();

    public SettingsPage(){
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        reloadFolderButton.addActionListener(this);
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "Slideshow Wallpaper Plugin";
    }

    /**
     * Renters the settings page.
     * @return the settings page.
     */
    @Override
    public @Nullable JComponent createComponent() {
        fileTextBox.setText(slideshowStorage.folder);
        intervalSpinner.setValue(slideshowStorage.interval);
        randomCheckBox.setSelected(slideshowStorage.random);
        directoryWalkCheckBox.setSelected(slideshowStorage.dirWalk);

        // Creates a listener for the text box.
        TextBrowseFolderListener textBrowseFolderListener = new TextBrowseFolderListener(descriptor) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!slideshowStorage.folder.isEmpty()){
                    fileChooser.setCurrentDirectory(new File(slideshowStorage.folder));
                }
                fileChooser.showOpenDialog(panel);
                File file = fileChooser.getSelectedFile();
                String path = file == null ? "" : file.getAbsolutePath();
                fileTextBox.setText(path);
            }
        };

        fileTextBox.addBrowseFolderListener(textBrowseFolderListener);
        return panel;
    }

    /**
     * Called repeatedly when the settings panel is open.
     * Checks whether any setting is different from the stored settings.
     * Returning true activates the apply button so users can save the new settings.
     * @return Whether the settings have been modified.
     */
    @Override
    public boolean isModified() {
        return !fileTextBox.getText().equals(slideshowStorage.folder) ||
                slideshowStorage.dirWalk != directoryWalkCheckBox.isSelected() ||
                slideshowStorage.random != randomCheckBox.isSelected() ||
                slideshowStorage.interval != intervalSpinner.getValue();
    }

    /**
     * Makes sure the currently running thread has stopped and starts another.
     */
    private void createNewThread(){
        if (worker != null){ worker.interrupt(); }

        PropertiesComponent prop = PropertiesComponent.getInstance();
        String activeImage = prop.getValue(IdeBackgroundUtil.FRAME_PROP);
        if (activeImage == null){ activeImage = ""; }

        if (slideshowStorage.folder.isEmpty()){ return; }
        worker = new Worker(
                slideshowStorage.interval,
                slideshowStorage.folder,
                slideshowStorage.random,
                slideshowStorage.dirWalk,
                activeImage
        );
        worker.start();
    }

    /**
     * The apply button has been pressed on the Settings panel.
     * Saves all new values.
     * @throws ConfigurationException No idea.
     */
    @Override
    public void apply() throws ConfigurationException {
        slideshowStorage.folder = fileTextBox.getText();
        slideshowStorage.dirWalk = directoryWalkCheckBox.isSelected();
        slideshowStorage.random = randomCheckBox.isSelected();
        slideshowStorage.interval = (Integer) intervalSpinner.getValue();
        createNewThread();
    }

    /**
     * Called when the Application has opened and loaded all dependencies it needs.
     * This should start the background process to automatically change the wallpaper.
     * @param project The current project that's open. `Not used.`
     */
    @Override
    public void runActivity(@NotNull Project project) {
        if (slideshowStorage.folder.isEmpty()){ return; }
        if (worker != null){ return; }
        createNewThread();
    }

    /**
     * A listener for the refresh button, should reload directory here.
     * @param e The refresh button pressed event.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        createNewThread();
    }
}

