package org.helioviewer.viewmodel.view;

import org.helioviewer.viewmodel.changeevent.ChangeEvent;

/**
 * View to merged multiple Views.
 *
 * <p>
 * The LayeredView a central element of the view chain. It is responsible for
 * displaying multiple views, which are organized as a stack of layers. The
 * basic functionality this includes to add, move and remove layers.
 *
 * <p>
 * When drawing the different layers, the layer with index zero is drawn first,
 * so the stack of layers is drawn in order bottom to top.
 *
 * <p>
 * The position of the layers in relation to each other is calculated based on
 * their regions. Thus, every view that is connected as a layer must provide a
 * {@link RegionView}.
 *
 * <p>
 * As an additional feature, the LayeredView support hiding layers.
 *
 *
 */

public class LayeredView extends AbstractView implements ViewListener {

    /**
     * {@inheritDoc}
     */
    @Override
    public void viewChanged(View sender, ChangeEvent event) {
    }

}
