package org.helioviewer.jhv.gui.dialogs.observation;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.ImageViewerGui;

@SuppressWarnings("serial")
public class ObservationDialog extends JDialog implements ActionListener {

    private ObservationDialogPanel observationPanel;

    private final JPanel contentPane = new JPanel();
    private final JPanel uiSelectionPane = new JPanel();
    private final JPanel buttonPane = new JPanel();
    private final JButton btnImages = new JButton();
    private final JButton btnClose = new JButton("Cancel");
    private final JButton availabilityButton = new JButton("Available data");

    private final ImageDataPanel imageObservationPanel;

    private static ObservationDialog instance;

    public static ObservationDialog getInstance() {
        if (instance == null) {
            instance = new ObservationDialog(ImageViewerGui.getMainFrame());
        }
        return instance;
    }

    public ImageDataPanel getObservationImagePane() {
        return imageObservationPanel;
    }

    private ObservationDialog(JFrame mainFrame) {
        super(mainFrame, true);

        // set dialog settings
        setContentPane(contentPane);
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));
        contentPane.setBorder(BorderFactory.createEmptyBorder(3, 9, 1, 9));
        contentPane.setFocusable(true);

        // set up components
        uiSelectionPane.setBorder(BorderFactory.createEtchedBorder());
        uiSelectionPane.add(availabilityButton);

        availabilityButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (observationPanel instanceof ImageDataPanel) {
                    String url = Settings.getSingletonInstance().getProperty("availability.images.url");
                    int sourceId = imageObservationPanel.getSourceId();
                    if (sourceId != -1)
                        url += "#IID" + sourceId;

                    JHVGlobals.openURL(url);
                } else {
                    String url = Settings.getSingletonInstance().getProperty("availability.timelines.url");
                    JHVGlobals.openURL(url);
                }
             }
        });

        buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 3, 3));
        buttonPane.add(btnClose);
        buttonPane.add(btnImages);

        btnImages.addActionListener(this);
        btnClose.addActionListener(this);

        getRootPane().registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeDialog();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        imageObservationPanel = new ImageDataPanel();
    }

    /**
     * Shows up the dialog and initializes the UI with the panel of the given
     * name.
     * */
    public void showDialog(Object layer, ObservationDialogPanel observationPanel) {
        if (layer == null) {
            setTitle("New Layer");
            btnImages.setText("Add");
        } else {
            setTitle("Change Layer");
            btnImages.setText("Change");
        }
        observationPanel.setupLayer(layer);

        this.layer = layer;

        if (this.observationPanel != observationPanel) {
            this.observationPanel = observationPanel;

            contentPane.removeAll();
            contentPane.add(uiSelectionPane);
            contentPane.add(observationPanel);
            contentPane.add(buttonPane);
        }

        pack();
        setLocationRelativeTo(ImageViewerGui.getMainFrame());
        getRootPane().setDefaultButton(btnImages);
        setVisible(true);
    }

    private Object layer; // tbd if timelines get change layer

    public void loadButtonPressed() {
        if (observationPanel.loadButtonPressed(layer))
            closeDialog();
    }

    private void closeDialog() {
        setVisible(false);
    }

    public void setAvailabilityStatus(boolean status) {
        availabilityButton.setEnabled(status);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(btnImages)) {
            loadButtonPressed();
        } else if (e.getSource().equals(btnClose)) {
            closeDialog();
        }
    }

}
