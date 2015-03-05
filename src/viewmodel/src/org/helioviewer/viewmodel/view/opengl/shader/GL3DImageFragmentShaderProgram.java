package org.helioviewer.viewmodel.view.opengl.shader;

import javax.media.opengl.GL2;

public class GL3DImageFragmentShaderProgram extends GLFragmentShaderProgram {
    private final double[] phiParamFloat = new double[4];
    private final double[] thetaParamFloat = new double[4];
    private final double[] differencePhiParamFloat = new double[4];
    private final double[] differenceThetaParamFloat = new double[4];
    private final double[] cutOffRadiusFloat = new double[4];
    private final double[] outerCutOffRadiusFloat = new double[4];

    /**
     * Binds (= activates it) the shader, if it is not active so far.
     *
     * @param gl
     *            Valid reference to the current gl object
     */
    @Override
    public final void bind(GL2 gl) {
        gl.glBindProgramARB(GL2.GL_FRAGMENT_PROGRAM_ARB, ShaderFactory.getFragmentId());
        ShaderFactory.bindEnvVars(gl, GL2.GL_FRAGMENT_PROGRAM_ARB, ShaderFactory.cutOffRadiusRef, cutOffRadiusFloat);
        ShaderFactory.bindEnvVars(gl, GL2.GL_FRAGMENT_PROGRAM_ARB, ShaderFactory.outerCutOffRadiusRef, outerCutOffRadiusFloat);

        ShaderFactory.bindEnvVars(gl, GL2.GL_FRAGMENT_PROGRAM_ARB, ShaderFactory.phiParamRef, this.phiParamFloat);
        ShaderFactory.bindEnvVars(gl, GL2.GL_FRAGMENT_PROGRAM_ARB, ShaderFactory.thetaParamRef, this.thetaParamFloat);
        ShaderFactory.bindEnvVars(gl, GL2.GL_FRAGMENT_PROGRAM_ARB, ShaderFactory.differencePhiParamRef, this.differencePhiParamFloat);
        ShaderFactory.bindEnvVars(gl, GL2.GL_FRAGMENT_PROGRAM_ARB, ShaderFactory.differenceThetaParamRef, this.differenceThetaParamFloat);
    }

    public void setCutOffRadius(double cutOffRadius, double outerCutOffRadius) {
        cutOffRadiusFloat[0] = cutOffRadius;
        outerCutOffRadiusFloat[0] = outerCutOffRadius;

    }

    public void changeAngles(double theta, double phi) {
        thetaParamFloat[0] = theta;
        phiParamFloat[0] = phi;
    }

}
