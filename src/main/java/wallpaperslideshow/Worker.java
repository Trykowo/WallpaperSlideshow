package wallpaperslideshow;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.wm.impl.IdeBackgroundUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.diagnostic.Logger;


public class Worker extends Thread{

    private static final Logger log = Logger.getInstance("WallpaperWorker");
    public boolean dirWalk;
    public String folder;
    public Integer interval;
    public Boolean random;

    public boolean stop = false;
    private ArrayList<String> pictureSet;
    private String activeImageName;

    private boolean colonNotify = false;

    public Worker(Integer interval, String folder, Boolean random, Boolean dirWalk, String activeImageName){
        this.dirWalk = dirWalk;
        this.folder = folder;
        this.interval = (interval * 1000) * 60; // Converts to minutes.
        this.random = random;
        this.activeImageName = activeImageName;
    }

    /**
     * Called if there were no images found.
     */
    private void sendNoImagesMessage(){
        String msg = "No images were found.";

        Notification n = new Notification(
                "Wallpaper Slideshow Notifications",
                "Wallpaper slideshow",
                msg,
                NotificationType.WARNING);
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater( () -> Notifications.Bus.notify(n));
        }
    }

    /**
     * Called if a comma was found in an image name.
     * Warns user.
     */
    private void sendCommaMessage(){
        String msg = "An image with a comma in the name was found during the search. " +
                "Jetbrains applications cannot use images with commas.";

        Notification n = new Notification(
                "Wallpaper Slideshow Notifications",
                "Wallpaper slideshow",
                msg,
                NotificationType.WARNING);

        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater( () -> Notifications.Bus.notify(n));
        }
    }

    /**
     * Builds a list of all files. In directory and nested directories if dirWalk.
     * @param rootDir The directory to search.
     * @return A list of all files in the directory.
     */
    private ArrayList<String> buildFileList(String rootDir){
        ArrayList<String> fileList = new ArrayList<String>();
        File fileDir = new File(rootDir);

        File[] files = fileDir.listFiles();

        if (files == null){ return fileList; }

        for (File file : files){
            if (file.isDirectory() && dirWalk){
                fileList.addAll(buildFileList(file.getAbsolutePath()));
            }
            else if (file.isFile()){
                fileList.add(file.getAbsolutePath());
            }
        }
        return fileList;
    }

    /**
     * Checks that the image dir given is a genuine image.
     * @param image The image to check.
     * @return Whether the image is genuine.
     */
    private boolean verifyImage(String image){
        try {
            FileInputStream fileInputStream = new FileInputStream(image);
            if (ImageIO.read(fileInputStream) == null){ return false; }
        } catch (IOException e){
            return false;
        }
        if (image.contains(",")){
            if (!colonNotify){
                sendCommaMessage();
                colonNotify = true;
            }
            return false;
        }
        return true;
    }

    /**
     * Sets the wallpaper as the chosen image.
     * @param newWallpaper The image to be set as the wallpaper.
     */
    public void setNewWallpaper(String newWallpaper) {
        PropertiesComponent prop = PropertiesComponent.getInstance();
        prop.setValue(IdeBackgroundUtil.FRAME_PROP, newWallpaper);
        prop.setValue(IdeBackgroundUtil.EDITOR_PROP, newWallpaper);
        prop.setValue(IdeBackgroundUtil.TARGET_PROP, newWallpaper);

        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(
                    IdeBackgroundUtil::repaintAllWindows
            );
        }
        else{
            IdeBackgroundUtil.repaintAllWindows();
        }
    }

    /**
     * Called by both image selection methods.
     * @param chosenImage The image to check then set as wallpaper if passes.
     * @param removed Whether the active image was removed from the list.
     * @return Whether the selected image has been set as wallpaper.
     */
    private boolean selectImageVerify(String chosenImage, boolean removed){
        if(verifyImage(chosenImage)){
            setNewWallpaper(chosenImage);
            if (removed){ pictureSet.add(activeImageName); }
            activeImageName = chosenImage;
            return true;
        }
        return false;
    }

    /**
     * Selects a random image from the list.
     * @return If the thread should keep running.
     */
    private boolean selectRandomImage(){
        Random rand = new Random();
        boolean removed = pictureSet.remove(activeImageName);
        int chosenIndex;

        while (pictureSet.size() > 0){
            chosenIndex = rand.nextInt(pictureSet.size());
            String chosenImage = pictureSet.get(chosenIndex);

            // Checks if image is valid. Adds old image back to list if removed.
            if(selectImageVerify(chosenImage, removed)){ return true; }
            pictureSet.remove(chosenIndex);
        }
        // Failed to find a suitable image with no images remaining to choose from.
        if(!removed){ sendNoImagesMessage(); }
        return false;
    }

    /**
     * Selects the next usable image in the ArrayList.
     * @return If the thread should keep running.
     */
    private boolean selectSequenceImage(){

        int imageIndex = pictureSet.indexOf(activeImageName);
        boolean removed = pictureSet.remove(activeImageName);
        if (imageIndex == -1){ imageIndex ++; }

        while (pictureSet.size() > 0) {

            // For some irritating reason I can't have the duplicate code outside the try/catch block.
            try {
                String chosenImage = pictureSet.get(imageIndex);

                // Checks if image is valid. Adds old image back to list if removed.
                if(selectImageVerify(chosenImage, removed)){ return true; }
                pictureSet.remove(imageIndex);
                imageIndex ++;
            } catch (IndexOutOfBoundsException ignore){
                imageIndex = 0;
                String chosenImage = pictureSet.get(imageIndex);

                // Checks if image is valid. Adds old image back to list if removed.
                if(selectImageVerify(chosenImage, removed)){ return true; }
                pictureSet.remove(imageIndex);
                imageIndex ++;
            }
        }

        // Failed to find a suitable image with no images remaining to choose from.
        if(!removed){ sendNoImagesMessage(); }
        return false;
    }

    @Override
    public void run() {
        pictureSet = buildFileList(folder);

        while (!stop) {

            if (pictureSet.size() < 1){
                sendNoImagesMessage();
                return;
            }

            if (random) {
                if (!selectRandomImage()){ return; }
            }
            else {
                if(!selectSequenceImage()){ return; }
            }

            try{
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}
