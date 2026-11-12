package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;

/** Konfigurasi Hidden Surface Removal pada OpenGL. */
public final class HSRManager {
    private HSRManager() {}
    public static void setupDepthTest(boolean on) {
        if (on) { glEnable(GL_DEPTH_TEST); glDepthFunc(GL_LESS); glDepthMask(true); }
        else { glDisable(GL_DEPTH_TEST); }
    }
    public static void setupBackFaceCulling(boolean on) {
        // Selalu tetapkan face yang dibuang dan aturan winding sebelum toggle.
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);
        if (on) glEnable(GL_CULL_FACE); else glDisable(GL_CULL_FACE);
    }
}
