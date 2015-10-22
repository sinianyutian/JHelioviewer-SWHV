package org.helioviewer.jhv.plugins.swek.sources.hek;

import java.util.List;

import org.helioviewer.jhv.base.math.Vec3d;
import org.helioviewer.jhv.data.datatype.event.JHVCoordinateSystem;
import org.helioviewer.jhv.data.datatype.event.JHVPositionInformation;

/**
 * Defines the HEK event position information.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class HEKPositionInformation implements JHVPositionInformation {

    /** coordinate system */
    private final JHVCoordinateSystem coordinateSystem;

    /** bound box */
    private final List<Vec3d> boundBox;

    /** bound cc */
    private final List<Vec3d> boundCC;

    /** central point */
    private final Vec3d centralPoint;

    /**
     * Create a HEKPositionInformation for the given coordinate system, bound
     * box and central point.
     *
     * @param coordinateSystem
     *            the coordinate system.
     * @param boundBox
     *            the bound box
     * @param centralPoint
     *            the central point
     */
    public HEKPositionInformation(JHVCoordinateSystem coordinateSystem, List<Vec3d> boundBox, List<Vec3d> boundCC,
            Vec3d centralPoint) {
        this.coordinateSystem = coordinateSystem;
        this.boundBox = boundBox;
        this.centralPoint = centralPoint;
        this.boundCC = boundCC;
    }

    @Override
    public JHVCoordinateSystem getCoordinateSystem() {
        return coordinateSystem;
    }

    @Override
    public List<Vec3d> getBoundBox() {
        return boundBox;
    }

    @Override
    public Vec3d centralPoint() {
        return centralPoint;
    }

    @Override
    public List<Vec3d> getBoundCC() {
        return boundCC;
    }

}
