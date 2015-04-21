package org.helioviewer.viewmodel.view;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.movie.MovieExport;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.jhv.display.DisplayListener;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.dialogs.ExportMovieDialog;
import org.helioviewer.viewmodel.view.AbstractView;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.opengl.GLInfo;
import org.helioviewer.viewmodel.view.opengl.GLSLShader;

import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;
import com.jogamp.opengl.util.awt.ImageUtil;

/**
 * A ComponentView is responsible for rendering the actual image to a Component.
 */
public class ComponentView implements GLEventListener, DisplayListener {

    private GLCanvas canvas;

    // screenshot & movie
    private ExportMovieDialog exportMovieDialog;
    private MovieExport export;
    private boolean exportMode = false;
    private boolean screenshotMode = false;
    private int previousScreenshot = -1;
    private File outputFile;

   /**
     * Activate is called before the component view will be the active component
     * view displayed on the GUI.
     */
    public void activate() {
        canvas.addGLEventListener(this);
        Displayer.addListener(this);
    }

    /**
     * Deactivate the Component View can be used to clean up the component view
     */
    public void deactivate() {
        Displayer.removeListener(this);
        canvas.removeGLEventListener(this);
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        final GL2 gl = drawable.getGL().getGL2();

        GLInfo.update((GLCanvas) drawable);

        GLSLShader.initShader(gl);
        GL3DState.setUpdated(gl, 100, 100);

        gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);

        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        gl.glEnable(GL2.GL_BLEND);
        gl.glEnable(GL2.GL_POINT_SMOOTH);

        gl.glEnable(GL2.GL_NORMALIZE);
        gl.glDepthFunc(GL2.GL_LEQUAL);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    }

    private static void displayBody(GL2 gl, int width, int height) {
        GL3DState state = GL3DState.get();
        GL3DCamera camera = GL3DState.getActiveCamera();

        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glBlendEquation(GL2.GL_FUNC_ADD);

        gl.glDisable(GL2.GL_BLEND);
        gl.glEnable(GL2.GL_DEPTH_TEST);

        gl.glPushMatrix();
        {
            camera.applyPerspective(state);
            camera.applyCamera(state);
            Displayer.getRenderablecontainer().render(state);
            camera.drawCamera(state);
            camera.resumePerspective(state);
        }
        gl.glPopMatrix();

        gl.glEnable(GL2.GL_BLEND);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        GL3DState.setUpdated(gl, width, height);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        gl.glPushMatrix();
        displayBody(gl, width, height);
        gl.glPopMatrix();

        if (exportMode || screenshotMode) {
            exportFrame();
        }
    }

    @Override
    public void display() {
        canvas.repaint();
    }

    private void exportFrame() {
        AbstractView mv = Displayer.getLayersModel().getActiveView();
        if (mv == null) {
            stopExport();
            return;
        }

        AWTGLReadBufferUtil rbu = new AWTGLReadBufferUtil(canvas.getGLProfile(), false);
        GL2 gl = canvas.getGL().getGL2();
        int width = canvas.getWidth();

        BufferedImage screenshot;

        if (exportMode) {
            int currentScreenshot = 1;
            int maxframeno = 1;
            if (mv instanceof JHVJPXView) {
                currentScreenshot = ((JHVJPXView) mv).getCurrentFrameNumber();
                maxframeno = ((JHVJPXView) mv).getMaximumFrameNumber();
            }

            screenshot = ImageUtil.createThumbnail(rbu.readPixelsToBufferedImage(gl, true), width);
            if (currentScreenshot != previousScreenshot) {
                export.writeImage(screenshot);
            }
            exportMovieDialog.setLabelText("Exporting frame " + (currentScreenshot + 1) + " / " + (maxframeno + 1));

            if ((!(mv instanceof JHVJPXView)) || (mv instanceof JHVJPXView && currentScreenshot < previousScreenshot)) {
                stopExport();
            }
            previousScreenshot = currentScreenshot;
        }

        if (screenshotMode) {
            screenshot = ImageUtil.createThumbnail(rbu.readPixelsToBufferedImage(gl, true), width);
            try {
                ImageIO.write(screenshot, "png", outputFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
            screenshotMode = false;
        }
    }

    public void startExport(ExportMovieDialog exportMovieDialog) {
        this.exportMovieDialog = exportMovieDialog;
        ImageViewerGui.getSingletonInstance().getLeftContentPane().setEnabled(false);

        AbstractView mv = Displayer.getLayersModel().getActiveView();
        if (mv instanceof JHVJPXView) {
            export = new MovieExport(canvas.getWidth(), canvas.getHeight());
            export.createProcess();
            exportMode = true;

            JHVJPXView jpxView = (JHVJPXView) mv;
            jpxView.pauseMovie();
            jpxView.setCurrentFrame(0);
            jpxView.playMovie();
        } else {
            exportMovieDialog.fail();
            exportMovieDialog = null;
        }
    }

    private void stopExport() {
        AbstractView mv = Displayer.getLayersModel().getActiveView();

        exportMode = false;
        previousScreenshot = -1;
        export.finishProcess();

        JTextArea text = new JTextArea("Exported movie at: " + export.getFileName());
        text.setBackground(null);
        JOptionPane.showMessageDialog(ImageViewerGui.getSingletonInstance().getMainImagePanel(), text);

        ImageViewerGui.getSingletonInstance().getLeftContentPane().setEnabled(true);

        ((JHVJPXView) mv).pauseMovie();
        exportMovieDialog.reset();
        exportMovieDialog = null;
    }

   /**
     * Saves the current screen content to the given file in the given format.
     *
     * @param imageFormat
     *            Desired output format
     * @param outputFile
     *            Desired output destination
     * @return
     * @throws IOException
     *             is thrown, if the given output file is not valid
     */
    public boolean saveScreenshot(String imageFormat, File outputFile) {
        this.outputFile = outputFile;
        screenshotMode = true;
        return true;
    }

    /**
     * Sets the component where the image will be displayed.
     * This component has to be build in the graphical user interface somewhere.
     */
    public void setComponent(Component component) {
        canvas = (GLCanvas) component;
    }

}
