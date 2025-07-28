package org.boxutil.units.builtin.legacy;

import org.boxutil.units.builtin.legacy.array.Stack2f;
import org.boxutil.units.builtin.legacy.array.Stack3f;
import org.boxutil.units.builtin.legacy.array.Stack3i;
import org.boxutil.units.builtin.legacy.array.TriIndex;
import org.lwjgl.opengl.GL11;

public class LegacyModelData {
    private final Stack3f[] v;
    private final Stack3f[] vn;
    private final Stack2f[] vt;
    private final TriIndex[] vf;

    public LegacyModelData(Stack3f[] v, Stack3f[] vn, Stack2f[] vt, TriIndex[] vf) {
        this.v = v;
        this.vn = vn;
        this.vt = vt;
        this.vf = vf;
    }

    /**
     * Call "GL11.glEnable(GL11.GL_CULL_FACE);" before draw, if needed.
     */
    public void glDraw(boolean normalInverse, boolean withTexture) {
        GL11.glBegin(GL11.GL_TRIANGLES);
        for (TriIndex mapping : this.vf) {
            for (Stack3i index : mapping.getIndex()) {
                Stack3f v = this.v[index.getX()];
                Stack3f vn = this.vn[index.getY()];
                Stack2f vt = this.vt[index.getZ()];

                if (normalInverse) GL11.glNormal3f(-vn.getX(), -vn.getY(), -vn.getZ()); else GL11.glNormal3f(vn.getX(), vn.getY(), vn.getZ());
                if (withTexture) GL11.glTexCoord2f(vt.getX(), vt.getY());
                GL11.glVertex3f(v.getX(), v.getY(), v.getZ());
            }
        }
        GL11.glEnd();
    }

    public Stack3f[] getVertices() {
        return v;
    }

    public Stack3f[] getNormals() {
        return vn;
    }

    public Stack2f[] getUVs() {
        return vt;
    }

    public TriIndex[] getPatches() {
        return vf;
    }
}
