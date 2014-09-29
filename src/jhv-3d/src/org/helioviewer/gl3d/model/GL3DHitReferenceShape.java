package org.helioviewer.gl3d.model;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.helioviewer.base.math.MathUtils;
import org.helioviewer.base.physics.Astronomy;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.model.image.GL3DImageMesh;
import org.helioviewer.gl3d.scenegraph.GL3DAABBox;
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.gl3d.scenegraph.GL3DMesh;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.GL3DTriangle;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DQuatd;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec2d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRay;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.TimestampChangedReason;
import org.helioviewer.viewmodel.metadata.HelioviewerPositionedMetaData;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.TimedMovieView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;

/**
 * The {@link GL3DHitReferenceShape} unifies all possible Image Layers (
 * {@link GL3DImageMesh} nodes in the Scene Graph) in a single mesh node. This
 * node offers a mathematically simpler representation for faster hit point
 * detection when used for determining the region of interest on the image
 * meshes.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DHitReferenceShape extends GL3DMesh implements ViewListener {
    private static final double extremeValue = 4000000.;

    private boolean hitCoronaPlane;
    private final GL3DMat4d hitRotation;

    private Date currentDate;

    private long timediff;

    private double currentRotation;

    private final GL3DQuatd localRotation;

    public GL3DHitReferenceShape() {
        this(false);
    }

    public GL3DHitReferenceShape(boolean hitCoronaPlane) {
        super("Hit Reference Shape");
        this.hitCoronaPlane = hitCoronaPlane;
        this.hitRotation = new GL3DMat4d();
        this.hitRotation.setIdentity();
        this.localRotation = GL3DQuatd.createRotation(0., new GL3DVec3d(0., 1., 0.));
    }

    public boolean isHitCoronaPlane() {
        return hitCoronaPlane;
    }

    public void setHitCoronaPlane(boolean hitCoronaPlane) {
        this.hitCoronaPlane = hitCoronaPlane;
    }

    @Override
    public void shapeDraw(GL3DState state) {
        return;
    }

    @Override
    public GL3DMeshPrimitive createMesh(GL3DState state, List<GL3DVec3d> positions, List<GL3DVec3d> normals, List<GL3DVec2d> textCoords, List<Integer> indices, List<GL3DVec4d> colors) {
        GL3DVec3d ll = createVertex(-extremeValue, -extremeValue, 0);
        GL3DVec3d lr = createVertex(extremeValue, -extremeValue, 0);
        GL3DVec3d tr = createVertex(extremeValue, extremeValue, 0);
        GL3DVec3d tl = createVertex(-extremeValue, extremeValue, 0);

        positions.add(ll);
        positions.add(lr);
        positions.add(tr);
        positions.add(tl);

        indices.add(0);
        indices.add(1);
        indices.add(2);

        indices.add(0);
        indices.add(2);
        indices.add(3);

        return GL3DMeshPrimitive.TRIANGLES;
    }

    private GL3DVec3d createVertex(double x, double y, double z) {
        return new GL3DVec3d(x, y, z);
    }

    @Override
    public boolean hit(GL3DRay ray) {
        if (isDrawBitOn(Bit.Hidden) || this.wmI == null) {
            return false;
        }

        ray.setOriginOS(this.wmI.multiply(this.localRotation.toMatrix().multiply(ray.getOrigin())));
        GL3DVec3d helpingDir = this.wmI.multiply(this.localRotation.toMatrix().multiply(ray.getDirection()));
        helpingDir.normalize();
        ray.setDirOS(helpingDir);
        return this.shapeHit(ray);
    }

    @Override
    public boolean shapeHit(GL3DRay ray) {
        boolean isSphereHit = false;
        if (!this.hitCoronaPlane) {
            isSphereHit = isSphereHit(ray);
            if (isSphereHit) {
                ray.isOnSun = true;
            }
        } else if (this.hitCoronaPlane || !isSphereHit) {
            super.shapeHit(ray);
        }
        if (ray.getHitPoint() != null) {
            ray.setHitPointOS(this.wmI.multiply(this.localRotation.toMatrix().multiply(ray.getHitPoint())));
            ray.setHitPoint(this.localRotation.toMatrix().multiply(ray.getHitPoint()));
            return true;
        }
        return false;
    }

    public boolean shapeHitPlanar(GL3DRay ray) {
        for (GL3DTriangle t : this.getTriangles()) {
            if (t.intersectsPlanar(ray)) {
                ray.setOriginShape(this);

                GL3DVec3d rayCopy2 = ray.getDirection().copy();
                rayCopy2.multiply(ray.getLength());
                GL3DVec3d rayCopy = ray.getOrigin().copy();
                rayCopy.add(rayCopy2);
                ray.setHitPoint(rayCopy);
                return true;
            }
        }
        return false;
    }

    private boolean isSphereHit(GL3DRay ray) {
        GL3DVec3d l = new GL3DVec3d(0, 0, 0).subtract(ray.getOrigin());
        GL3DVec3d rayDirCopy = ray.getDirection().copy();
        rayDirCopy.normalize();
        double s = l.dot(rayDirCopy);
        double l2 = l.length2();
        double r2 = Constants.SunRadius2;
        if (s < 0 && l2 > r2) {
            return false;
        }

        double s2 = s * s;
        double m2 = l2 - s2;
        if (m2 > r2) {
            return false;
        }

        double q = Math.sqrt(r2 - m2);
        double t;
        if (l2 > r2) {
            t = s - q;
        } else {
            t = s + q;
        }
        ray.setLength(t);
        GL3DVec3d rayCopy2 = ray.getDirection().copy();
        rayCopy2.normalize();
        rayCopy2.multiply(t);

        GL3DVec3d rayCopy = ray.getOrigin().copy();
        rayCopy.add(rayCopy2);
        ray.setHitPoint(rayCopy);
        ray.isOutside = false;
        ray.setOriginShape(this);
        return true;
    }

    @Override
    public GL3DAABBox buildAABB() {
        this.aabb.fromOStoWS(new GL3DVec3d(-extremeValue, -extremeValue, -extremeValue), new GL3DVec3d(extremeValue, extremeValue, extremeValue), this.wm);

        return this.aabb;
    }

    @Override
    public void viewChanged(View sender, ChangeEvent aEvent) {
        TimestampChangedReason timestampReason = aEvent.getLastChangedReasonByType(TimestampChangedReason.class);
        if ((timestampReason != null) && (timestampReason.getView() instanceof TimedMovieView) && LinkedMovieManager.getActiveInstance().isMaster((TimedMovieView) timestampReason.getView())) {
            currentDate = timestampReason.getNewDateTime().getTime();
            updateRotation(timestampReason.getView());
        }
    }

    public void updateRotation(View view) {
        this.timediff = (currentDate.getTime()) / 1000 - Constants.referenceDate;
        MetaDataView metadataView = view.getAdapter(MetaDataView.class);

        this.currentRotation = Astronomy.getL0Radians(currentDate);//DifferentialRotation.calculateRotationInRadians(0., this.timediff) % (Math.PI * 2.0);
        if (metadataView.getMetaData() instanceof HelioviewerPositionedMetaData && ((HelioviewerPositionedMetaData) (metadataView.getMetaData())).getInstrument().equalsIgnoreCase("SECCHI")) {
            this.currentRotation -= ((HelioviewerPositionedMetaData) (metadataView.getMetaData())).getStonyhurstLongitude() / MathUtils.radeg;
        }
        Calendar cal = new GregorianCalendar();
        cal.setTime(new Date(currentDate.getTime()));
        double bobs0 = 0.;
        double b0 = -Astronomy.getB0InRadians(cal);
        if (metadataView.getMetaData() instanceof HelioviewerPositionedMetaData && ((HelioviewerPositionedMetaData) (metadataView.getMetaData())).getInstrument().equalsIgnoreCase("SECCHI")) {
            bobs0 = ((HelioviewerPositionedMetaData) (metadataView.getMetaData())).getStonyhurstLatitude() / MathUtils.radeg - Astronomy.getB0InRadians(cal);

        }
        this.localRotation.clear();
        this.localRotation.rotate(GL3DQuatd.createRotation(-b0, new GL3DVec3d(1, 0, 0)));
        this.localRotation.rotate(GL3DQuatd.createRotation(this.currentRotation, new GL3DVec3d(0, 1, 0)));
        this.localRotation.rotate(GL3DQuatd.createRotation(-bobs0, new GL3DVec3d(1, 0, 0)));

    }
}
