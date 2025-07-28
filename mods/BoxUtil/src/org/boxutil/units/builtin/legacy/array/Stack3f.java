package org.boxutil.units.builtin.legacy.array;

public class Stack3f extends Stack2f {
    public float z = 0.0f;

    public Stack3f(float x, float y, float z) {
        super(x, y);
        this.z = z;
    }

    public float getZ() {
        return this.z;
    }
}
