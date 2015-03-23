package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;

/**
 * Action to start downloading all available image data of a layer (identified
 * by a view)
 *
 * @author Andre Dau
 * @author Malte Nuhn
 */
public class DownloadJPIPOfView extends AbstractAction {

    private static final long serialVersionUID = 1L;
    private final JHVJP2View view;

    /**
     * @param view
     *            - view of which the available image data should be downloaded
     */
    public DownloadJPIPOfView(JHVJP2View view) {
        super("Download Movie", IconBank.getIcon(JHVIcon.DOWNLOAD));
        this.view = view;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        LayersModel.getSingletonInstance().downloadLayer(view);
    }

}
