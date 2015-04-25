package org.helioviewer.viewmodel.view.fitsview;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.viewmodel.imagedata.ARGBInt32ImageData;
import org.helioviewer.viewmodel.imagedata.ColorMask;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.viewmodel.imagedata.SingleChannelShortImageData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.metadata.MetaDataConstructor;
import org.helioviewer.viewmodel.metadata.ObserverMetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.AbstractView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.viewport.StaticViewport;
import org.helioviewer.viewmodel.viewport.Viewport;

import com.jogamp.opengl.GL2;

/**
 * Implementation of View for FITS images.
 *
 * <p>
 * For further informations about the behavior of this view,
 * {@link View} is a good start to get into the concept.
 *
 * @author Andreas Hoelzl
 * */
public class JHVFITSView extends AbstractView {

    protected Viewport viewport;
    protected Region region;
    protected FITSImage fits;
    protected MetaData m;
    private final URI uri;

    /**
     * Constructor which loads a fits image from a given URI.
     *
     * @param uri
     *            Specifies the location of the FITS file.
     * @throws IOException
     *             when an error occurred during reading the fits file.
     * */
    public JHVFITSView(URI uri) throws IOException {
        this.uri = uri;
        if (!uri.getScheme().equalsIgnoreCase("file"))
            throw new IOException("FITS does not support the " + uri.getScheme() + " protocol");

        try {
            fits = new FITSImage(uri.toURL().toString());
        } catch (Exception e) {
            throw new IOException("FITS image data cannot be accessed.");
        }

        initFITSImageView();
    }

    /**
     * Constructor which uses a given fits image.
     *
     * @param fits
     *            FITSImage object which contains the image data
     * @param uri
     *            Specifies the location of the FITS file.
     * */
    public JHVFITSView(FITSImage fits, URI uri) {
        this.uri = uri;
        this.fits = fits;
        initFITSImageView();
    }

    /**
     * Initializes global variables.
     */
    private void initFITSImageView() {
        m = MetaDataConstructor.getMetaData(fits);

        BufferedImage bi = fits.getImage(0, 0, fits.getPixelHeight(), fits.getPixelWidth());
        if (bi.getColorModel().getPixelSize() <= 8) {
            imageData = new SingleChannelByte8ImageData(bi, new ColorMask());
        } else if (bi.getColorModel().getPixelSize() <= 16) {
            imageData = new SingleChannelShortImageData(bi.getColorModel().getPixelSize(), bi, new ColorMask());
        } else {
            imageData = new ARGBInt32ImageData(bi, new ColorMask());
        }
        imageData.setMETADATA(m);

        region = StaticRegion.createAdaptedRegion(m.getPhysicalLowerLeft().x, m.getPhysicalLowerLeft().y, m.getPhysicalImageSize().x, m.getPhysicalImageSize().y);
        imageData.setRegion(region);
        imageData.setMETADATA(this.m);
        viewport = StaticViewport.createAdaptedViewport(100, 100);
    }

    /**
     * Updates the sub image depending on the current region.
     *
     * @param event
     *            Event that belongs to the request.
     * */
    private void updateImageData() {

        Displayer.display();
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public boolean setViewport(Viewport v) {
        // check if viewport has changed
        if (viewport != null && v != null && viewport.getWidth() == v.getWidth() && viewport.getHeight() == v.getHeight())
            return false;

        viewport = v;
        return true;
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public Region getRegion() {
        return region;
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public boolean setRegion(Region r) {
        // check if region has changed
        if ((region == r) || (region != null && r != null && region.getCornerX() == r.getCornerX() && region.getCornerY() == r.getCornerY() && region.getWidth() == r.getWidth() && region.getHeight() == r.getHeight()))
            return false;

        region = r;
        updateImageData();
        return true;
    }

    /**
     * Returns the header information as XML string.
     *
     * @return XML string including all header information.
     * */
    public String getHeaderAsXML() {
        return fits.getHeaderAsXML();
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public MetaData getMetaData() {
        return m;
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public ImageData getSubimageData() {
        return imageData;
    }

    /**
     * Returns the FITS image managed by this class.
     *
     * @return FITS image.
     */
    public FITSImage getFITSImage() {
        return fits;
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public String getName() {
        if (m instanceof ObserverMetaData) {
            ObserverMetaData observerMetaData = (ObserverMetaData) m;
            return observerMetaData.getFullName();
        } else {
            String name = uri.getPath();
            return name.substring(name.lastIndexOf('/') + 1, name.lastIndexOf('.'));
        }
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public URI getUri() {
        return uri;
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public URI getDownloadURI() {
        return uri;
    }

    @Override
    public boolean getBaseDifferenceMode() {
        return false;
    }

    @Override
    public boolean getDifferenceMode() {
        return false;
    }

    @Override
    public ImageData getBaseDifferenceImageData() {
        return imageData;
    }

    @Override
    public ImageData getPreviousImageData() {
        return imageData;
    }

    @Override
    public ImageData getImageData() {
        return imageData;
    }

    @Override
    public void applyFilters(GL2 gl) {
        super.applyFilters(gl);
    }

}
