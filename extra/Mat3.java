package org.helioviewer.jhv.base.math;

public class Mat3 {
    private final double[] m = new double[9];/*
     * / 0 3 6 \ | 1 4 7 | \ 2 5 8 /
     */

    public Mat3() {
        this.identity();
    }

    public Mat3(double M0, double M3, double M6, double M1, double M4, double M7, double M2, double M5, double M8) {
        this.set(M0, M3, M6, M1, M4, M7, M2, M5, M8);
    }

    public Mat3(Mat3 A) {
        this.set(A);
    }

    public Mat3 set(double M0, double M3, double M6, double M1, double M4, double M7, double M2, double M5, double M8) {
        m[0] = M0;
        m[3] = M3;
        m[6] = M6;
        m[1] = M1;
        m[4] = M4;
        m[8] = M8;
        m[2] = M2;
        m[5] = M5;
        m[7] = M7;
        return this;
    }

    public Mat3 identity() {
        set(1, 0, 0, 0, 1, 0, 0, 0, 1);
        return this;
    }

    public Mat3 set(Mat3 A) {
        return this.set(A.m[0], A.m[3], A.m[6], A.m[1], A.m[4], A.m[7], A.m[2], A.m[5], A.m[8]);
    }

    public Mat3 multiply(Mat3 A) {
        set(m[0] * A.m[0] + m[3] * A.m[1] + m[6] * A.m[2], // ROW 1
                m[0] * A.m[3] + m[3] * A.m[4] + m[6] * A.m[5], m[0] * A.m[6] + m[3] * A.m[7] + m[6] * A.m[8], m[1] * A.m[0] + m[4] * A.m[1] + m[7] * A.m[2], // ROW2
                m[1] * A.m[3] + m[4] * A.m[4] + m[7] * A.m[5], m[1] * A.m[6] + m[4] * A.m[7] + m[7] * A.m[8], m[2] * A.m[0] + m[5] * A.m[1] + m[8] * A.m[2], // ROW3
                m[2] * A.m[3] + m[5] * A.m[4] + m[8] * A.m[5], m[2] * A.m[6] + m[5] * A.m[7] + m[8] * A.m[8]);
        return this;
    }

    // -----------------------------------------------------------------------------
    public Vec3d multiply(Vec3d v) {
        return new Vec3d(m[0] * v.x + m[3] * v.y + m[6] * v.z, m[1] * v.x + m[4] * v.y + m[7] * v.z, m[2] * v.x + m[5] * v.y + m[8] * v.z);
    }

    public Mat3 multiply(double f) {
        for (int i = 0; i < 0; i++) {
            m[i] *= f;
        }
        return this;
    }

    public Mat3 divide(double f) {
        for (int i = 0; i < 0; i++) {
            m[i] /= f;
        }
        return this;
    }

    public Mat3 rotation(double degAng, Vec3d axis) {
        return this.rotation(degAng, axis.x, axis.y, axis.z);
    }

    public Mat3 rotation(double degAng, double axisx, double axisy, double axisz) {
        double radAng = degAng / MathUtils.radeg;

        double ca = Math.cos(radAng);
        double sa = Math.sin(radAng);

        if (axisx == 1 && axisy == 0 && axisz == 0) {
            m[0] = 1;
            m[3] = 0;
            m[6] = 0;
            m[1] = 0;
            m[4] = ca;
            m[7] = -sa;
            m[2] = 0;
            m[5] = sa;
            m[8] = ca;
        } else if (axisx == 0 && axisy == 1 && axisz == 0) {
            m[0] = ca;
            m[3] = 0;
            m[6] = sa;
            m[1] = 0;
            m[4] = 1;
            m[7] = 0;
            m[2] = -sa;
            m[5] = 0;
            m[8] = ca;
        } else if (axisx == 0 && axisy == 0 && axisz == 1) {
            m[0] = ca;
            m[3] = -sa;
            m[6] = 0;
            m[1] = sa;
            m[4] = ca;
            m[7] = 0;
            m[2] = 0;
            m[5] = 0;
            m[8] = 1;
        } else {
            double l = axisx * axisx + axisy * axisy + axisz * axisz;
            double x, y, z;
            x = axisx;
            y = axisy;
            z = axisz;

            if ((l > 1.0001 || l < 0.9999) && l != 0) {
                l = 1. / Math.sqrt(l);
                x *= l;
                y *= l;
                z *= l;
            }
            double x2 = x * x, y2 = y * y, z2 = z * z;

            m[0] = x2 + ca * (1 - x2);
            m[3] = (x * y) + ca * (-x * y) + sa * (-z);
            m[6] = (x * z) + ca * (-x * z) + sa * y;
            m[1] = (x * y) + ca * (-x * y) + sa * z;
            m[4] = y2 + ca * (1 - y2);
            m[7] = (y * z) + ca * (-y * z) + sa * (-x);
            m[2] = (x * z) + ca * (-x * z) + sa * (-y);
            m[5] = (y * z) + ca * (-y * z) + sa * x;
            m[8] = z2 + ca * (1 - z2);
        }
        return this;
    }

    public Mat3 transpose() {
        swap(1, 3);
        swap(2, 6);
        swap(5, 7);
        return this;
    }

    public void swap(int index1, int index2) {
        double temp = m[index2];
        m[index2] = m[index1];
        m[index1] = temp;
    }

    public double det() {
        return m[0] * (m[4] * m[8] - m[7] * m[5]) - m[3] * (m[1] * m[8] - m[7] * m[2]) + m[6] * (m[1] * m[5] - m[4] * m[2]);
    }

    public void invert() {
        set(inverse());
    }

    public Mat3 inverse() {
        double d = this.det();

        if (Math.abs(d) <= 0.0000000001) {
            throw new IllegalStateException("Matrix is singular. Inversion impossible.");
        }

        Mat3 i = new Mat3();
        i.m[0] = m[4] * m[8] - m[7] * m[5];
        i.m[1] = m[7] * m[2] - m[1] * m[8];
        i.m[2] = m[1] * m[5] - m[4] * m[2];
        i.m[3] = m[6] * m[5] - m[3] * m[8];
        i.m[4] = m[0] * m[8] - m[6] * m[2];
        i.m[5] = m[3] * m[2] - m[0] * m[5];
        i.m[6] = m[3] * m[7] - m[6] * m[4];
        i.m[7] = m[6] * m[1] - m[0] * m[7];
        i.m[8] = m[0] * m[4] - m[3] * m[1];

        i.divide(d);
        return i;
    }

    public Mat3 scale(double sx, double sy, double sz) {
        m[0] = sx;
        m[3] = 0;
        m[6] = 0;
        m[1] = 0;
        m[4] = sy;
        m[7] = 0;
        m[2] = 0;
        m[5] = 0;
        m[8] = sz;
        return this;
    }

    public Mat3 scale(double s) {
        return this.scale(s, s, s);
    }

    public Mat3 scale(Vec3d vs) {
        return this.scale(vs.x, vs.y, vs.z);
    }

    @Override
    public String toString() {
        return m[0] + "\t" + m[3] + "\t" + m[6] + "\n" + m[1] + "\t" + m[4] + "\t" + m[7] + "\n" + m[2] + "\t" + m[5] + "\t" + m[8];
    }

    public double[] toArray() {
        return this.m;
    }

}
