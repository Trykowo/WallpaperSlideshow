package wallpaperslideshow;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


@State(name = "SlideshowStorage", storages = @Storage(value = "$APP_CONFIG$/SlideshowWallpaper.xml"))
public class SlideshowStorage implements PersistentStateComponent<SlideshowStorage> {
    public Integer interval = 5;
    public String folder = "";
    public boolean random = false;
    public boolean dirWalk = false;

    @Override
    public @Nullable SlideshowStorage getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull SlideshowStorage state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public static SlideshowStorage getInstance() {
        return ApplicationManager.getApplication().getService(SlideshowStorage.class).getState();
    }
}
