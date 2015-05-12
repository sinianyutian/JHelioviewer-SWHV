package org.helioviewer.jhv.camera;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.viewmodel.view.AbstractView;

public class GL3DCameraOptionsPanel extends JPanel implements LayersListener {

    private JPanel optionsPanel;
    private JTabbedPane tab;
    private GL3DCamera previousCamera;

    private static GL3DEarthCameraOptionPanel earthCameraOptionPanel;
    private static GL3DFollowObjectCameraOptionPanel followObjectCameraOptionPanel;
    private static GL3DObserverCameraOptionPanel observerCameraOptionPanel;

    private GL3DCameraOptionPanel getCameraOptionsPanel(GL3DCamera camera) {
        if (camera instanceof GL3DEarthCamera) {
            if (earthCameraOptionPanel == null)
                earthCameraOptionPanel = new GL3DEarthCameraOptionPanel((GL3DEarthCamera) camera);
            return earthCameraOptionPanel;
        } else if (camera instanceof GL3DFollowObjectCamera) {
            if (followObjectCameraOptionPanel == null)
                followObjectCameraOptionPanel = new GL3DFollowObjectCameraOptionPanel((GL3DFollowObjectCamera) camera);
            return followObjectCameraOptionPanel;
        } else if (camera instanceof GL3DObserverCamera) {
            if (observerCameraOptionPanel == null)
                observerCameraOptionPanel = new GL3DObserverCameraOptionPanel((GL3DObserverCamera) camera);
            return observerCameraOptionPanel;
        }
        return null;
    }

    public GL3DCameraOptionsPanel(GL3DCamera newCamera) {
        Displayer.getLayersModel().addLayersListener(this);
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        addCameraTabs();
        previousCamera = newCamera;
        changeCamera(newCamera);
    }

    private void addCameraTabs() {
        tab = new JTabbedPane();
        tab.add("Observer", new GL3DObserverCameraOptionPanel((GL3DObserverCamera) Displayer.getActiveCamera()));
        tab.add("Earth", new JPanel(new BorderLayout()));
        tab.add("Expert", new JPanel(new BorderLayout()));
        tab.add(new JPanel());
        tab.setTabComponentAt(3, new JLabel(IconBank.getIcon(JHVIcon.INFO)));
        add(tab);

        tab.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int index = tab.getSelectedIndex();
                if (index == 0) {
                    changeCamera(new GL3DObserverCamera(false));
                }
                if (index == 1) {
                    changeCamera(new GL3DEarthCamera());
                }
                if (index == 2) {
                    changeCamera(new GL3DFollowObjectCamera());
                }
                if (index == 3) {
                    optionsPanel = infoPanel();
                }
                tab.setComponentAt(index, optionsPanel);
            }
        });
    }

    private void initCamera(GL3DCamera newCamera) {
        changeCamera(newCamera);
        optionsPanel = getCameraOptionsPanel(newCamera);
        Displayer.display();
        tab.setComponentAt(0, optionsPanel);
        tab.setSelectedIndex(0);
    }

    private void changeCamera(GL3DCamera newCamera) {
        boolean trackingMode = previousCamera.getTrackingMode();
        newCamera.setTrackingMode(trackingMode);
        optionsPanel = getCameraOptionsPanel(newCamera);

        Displayer.setActiveCamera(newCamera);
        Displayer.display();
        previousCamera = newCamera;
    }

    private JPanel infoPanel() {
        JPanel infoExplainPanel = new JPanel();
        infoExplainPanel.setLayout(new BoxLayout(infoExplainPanel, BoxLayout.PAGE_AXIS));
        infoExplainPanel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));

        String explanation = "Observer camera: view from observer.\n";
        explanation += "Camera time defined by timestamps of the active layer.\n\n";
        explanation += "Earth camera: view from Earth.\n";
        explanation += "Camera time defined by timestamps of the active layer.\n\n";
        explanation += "Expert camera: view from selected object.\n";
        explanation += "Camera time defined by timestamps of the active layer, ";
        explanation += "unless \"Use active layer timestamps\" is off. ";
        explanation += "In that case, camera time is interpolated in the configured time interval.";

        JTextArea infoText = new JTextArea(explanation);
        infoText.setEditable(false);
        infoText.setLineWrap(true);
        infoText.setWrapStyleWord(true);
        infoText.setOpaque(false);
        infoExplainPanel.add(infoText);
        return infoExplainPanel;
    }

    public void fireInit() {
        this.initCamera(new GL3DObserverCamera(false));
        if (Displayer.getLayersModel().getNumLayers() > 0) {
            visactivate();
        } else {
            visdeactivate();
        }
    }

    public void enableComponents(Container container, boolean enable) {
        Component[] components = container.getComponents();
        for (Component component : components) {
            component.setEnabled(enable);
            if (component instanceof Container) {
                enableComponents((Container) component, enable);
            }
        }
    }

    void visactivate() {
        /*
         * GL3DCameraOptionPanel optionsPanel; enableComponents(tab, true);
         * optionsPanel =
         * cameraOptionsAttributeManager.getCameraOptionAttributePanel
         * (cameraSelectorModel.getObserverCamera());
         * enableComponents(optionsPanel, true); optionsPanel =
         * cameraOptionsAttributeManager
         * .getCameraOptionAttributePanel(cameraSelectorModel.getEarthCamera());
         * enableComponents(optionsPanel, true); optionsPanel =
         * cameraOptionsAttributeManager
         * .getCameraOptionAttributePanel(cameraSelectorModel
         * .getFollowObjectCamera()); enableComponents(optionsPanel, true);
         */
    }

    void visdeactivate() {
        /*
         * enableComponents(tab, false); optionsPanel =
         * cameraOptionsAttributeManager
         * .getCameraOptionAttributePanel(cameraSelectorModel
         * .getObserverCamera()); enableComponents(optionsPanel, false);
         * optionsPanel =
         * cameraOptionsAttributeManager.getCameraOptionAttributePanel
         * (cameraSelectorModel.getEarthCamera());
         * enableComponents(optionsPanel, false); optionsPanel =
         * cameraOptionsAttributeManager
         * .getCameraOptionAttributePanel(cameraSelectorModel
         * .getFollowObjectCamera()); enableComponents(optionsPanel, false);
         */
    }

    @Override
    public void layerAdded(int idx) {
        if (Displayer.getLayersModel().getNumLayers() > 0) {
            this.visactivate();
        }
    }

    @Override
    public void layerRemoved(int oldIdx) {
        if (Displayer.getLayersModel().getNumLayers() == 0) {
            this.visdeactivate();
        }
    }

    @Override
    public void activeLayerChanged(AbstractView view) {
    }

}
