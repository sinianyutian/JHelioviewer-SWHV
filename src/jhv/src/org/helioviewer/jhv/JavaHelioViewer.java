package org.helioviewer.jhv;

import java.awt.EventQueue;
import java.util.Locale;
import java.util.TimeZone;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.logging.LogSettings;
import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.io.CommandLineProcessor;
import org.helioviewer.jhv.resourceloader.SystemProperties;
import org.helioviewer.viewmodel.view.jp2view.J2KRenderGlobalOptions;
import org.helioviewer.viewmodel.view.jp2view.JP2Image;
import org.helioviewer.viewmodel.view.jp2view.kakadu.JHV_KduException;

/**
 * This class starts the applications.
 *
 * @author caplins
 * @author Benjamin Wamsler
 * @author Markus Langenberg
 * @author Stephan Pagel
 * @author Andre Dau
 * @author Helge Dietert
 *
 */
public class JavaHelioViewer {

    public static void main(String[] args) {
        // Prints the usage message
        if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
            System.out.println(CommandLineProcessor.getUsageMessage());
            return;
        }
        // Uncaught runtime errors are displayed in a dialog box in addition
        JHVUncaughtExceptionHandler.setupHandlerForThread();

        // Save command line arguments
        CommandLineProcessor.setArguments(args);

        // Save current default system timezone in user.timezone
        System.setProperty("user.timezone", TimeZone.getDefault().getID());

        // Per default all times should be given in GMT
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        // Save current default locale to user.locale
        System.setProperty("user.locale", Locale.getDefault().toString());

        // Per default, the us locale should be used
        Locale.setDefault(Locale.US);

        // init log
        LogSettings.init("/settings/log4j.initial.properties", JHVDirectory.SETTINGS.getPath() + "log4j.properties", JHVDirectory.LOGS.getPath(), CommandLineProcessor.isOptionSet("--use-existing-log-time-stamp"));

        // Information log message
        String argString = "";
        for (int i = 0; i < args.length; ++i) {
            argString += " " + args[i];
        }
        Log.info("JHelioviewer started with command-line options:" + argString);

        // This attempts to create the necessary directories for the application
        Log.info("Create directories...");
        JHVGlobals.createDirs();

        // Save the log settings. Must be done AFTER the directories are created
        LogSettings.getSingletonInstance().update();

        // Read the version and revision from the JAR metafile
        JHVGlobals.determineVersionAndRevision();

        Log.info("Initializing JHelioviewer");
        // Load settings from file but do not apply them yet
        // The settings must not be applied before the kakadu engine has been
        // initialized
        Log.info("Load settings");
        Settings.getSingletonInstance().load();

        // Set the platform system properties
        SystemProperties.setPlatform();
        Log.info("OS: " + System.getProperty("jhv.os") + " - arch: " + System.getProperty("jhv.arch") + " - java arch: " + System.getProperty("jhv.java.arch"));

        Log.debug("Instantiate Kakadu engine");
        KakaduEngine engine = new KakaduEngine();

        try {
            JHVLoader.copyKDULibs();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            engine.startKduMessageSystem();
        } catch (JHV_KduException e) {
            Log.fatal("Failed to setup Kakadu message handlers.", e);
            Message.err("Error starting Kakadu message handler", e.getMessage(), true);
            return;
        }

        // Apply settings after kakadu engine has been initialized
        Log.info("Use cache directory: " + JHVDirectory.CACHE.getPath());
        JP2Image.setCachePath(JHVDirectory.CACHE.getFile());
        Settings.getSingletonInstance().update();
        J2KRenderGlobalOptions.setDoubleBufferingOption(true);

        Log.info("Start main window");
        try {
            EventQueue.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    Settings.getSingletonInstance().setLookAndFeelEverywhere(null, null);
                    ImageViewerGui.getSingletonInstance(); // build UI
                    ImageViewerGui.loadAtStart();
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            if (args.length != 0 && args[0].equals("--remote-plugins")) {
                Log.info("Load remote plugins");
                JHVLoader.loadRemotePlugins(args);
            } else {
                Log.info("Load bundled plugins");
                JHVLoader.loadBundledPlugin("EVEPlugin.jar");
                JHVLoader.loadBundledPlugin("PfssPlugin.jar");
                JHVLoader.loadBundledPlugin("SWEKPlugin.jar");
                JHVLoader.loadBundledPlugin("SWHVHEKPlugin.jar");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
