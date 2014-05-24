package org.helioviewer.gl3d.gui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.gl3d.camera.GL3DCamera;

/**
 * Action that enables the Solar Rotation Tracking, which ultimately changes the
 * current {@link GL3DCamera} to the
 * {@link GL3DSolarRotationTrackingTrackballCamera}
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DToggleSolarRotationAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    public GL3DToggleSolarRotationAction() {
        super("Track");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (GL3DCameraSelectorModel.getInstance().getCurrentCamera() instanceof GL3DTrackballCamera) {
            GL3DCameraSelectorModel.getInstance().setCurrentCamera(GL3DCameraSelectorModel.getInstance().getSolarRotationCamera());
        } else {
            GL3DCameraSelectorModel.getInstance().setCurrentCamera(GL3DCameraSelectorModel.getInstance().getTrackballCamera());
        }
    }

}
