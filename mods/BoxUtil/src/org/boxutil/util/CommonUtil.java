package org.boxutil.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.*;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class CommonUtil {
    private static final double[] _LINEAR = new double[]{0.2126729d, 0.7151522d, 0.0721750d};
    private final static MethodHandle[] _METHOD_HANDLE = new MethodHandle[4]; // new File(), ImageIO.write(), File.exists(), File.mkdirs()
    private final static int[] _IMAGE_SAVE_MASK = new int[]{0x00FF0000, 0x0000FF00, 0x000000FF, 0xFF000000};
    private final static byte[] _IMAGE_SAVE_MOVE = new byte[]{16, 8, 0, 24};
    private static String SEPARATOR = "\\";
    private static boolean _INVOKE_INIT = false;
    private static int _GL_TRANSFER_FBO = 0;
    private static boolean _GL_TRANSFER_FBO_INIT = false;

    public final static class Kelvin {
        public final static Color K_1000 = new Color(255, 68, 0);
        public final static Color K_1930 = new Color(255, 133, 3);
        public final static Color K_3000 = new Color(255, 177, 109);
        public final static Color K_4000 = new Color(255, 205, 166);
        public final static Color K_5600 = new Color(255, 239, 225);
        public final static Color K_6000 = new Color(255, 246, 236);
        public final static Color K_6500 = new Color(255, 254, 250);
        public final static Color K_6700 = new Color(254, 248, 255);
        public final static Color K_7000 = new Color(242, 242, 250);
        public final static Color K_10000 = new Color(201, 218, 255);
        public final static Color K_15000 = new Color(181, 205, 255);
        public final static Color K_40000 = new Color(151, 185, 255);
    }

    private static int colorClamp(int value) {
        return Math.max(Math.min(value, 255), 0);
    }

    private static float colorClamp(float value) {
        return Math.max(Math.min(value, 1.0f), 0.0f);
    }

    /**
     * Not accurate numbers.<p>
     * The value at [1000, 40000], unit is K(Kelvin).
     */
    public static Color getKelvinColor(float temperature) {
        float k = temperature;
        if (k <= 1000.0f) return Kelvin.K_1000;
        else if (k == 6500.0f) return Kelvin.K_6500;
        else if (k >= 40000.0f) return Kelvin.K_40000;

        int red, green, blue;
        k *= 0.01f;
        if (k <= 66.0f) {
            red = 0;

            float toClamp = 99.4708025861f * (float) Math.log(k) - 161.1195681661f;
            green = colorClamp((int) toClamp);

            if (k <= 19.0f) {
                blue = 0;
            } else {
                toClamp = 138.5177312231f * (float) Math.log(k - 10.0f) - 305.0447927307f;
                blue = colorClamp((int) toClamp);
            }
        } else {
            float km = k - 60.0f;
            float toClamp = 329.698727446f * (float) Math.pow(km, -0.1332047592f);
            red = colorClamp((int) toClamp);

            toClamp = 288.1221695283f * (float) Math.pow(km, -0.0755148492f);
            green = colorClamp((int) toClamp);

            blue = 255;
        }

        return new Color(red, green, blue);
    }

    public static float[] RGBToHSVArray(float r, float g, float b) {
        float[] p = g > b ? new float[]{g, b, 0.0f, -0.33333334f} : new float[]{b, g, -1.0f, 0.6666667f};
        float[] q = r > p[0] ? new float[]{r, p[1], p[2], p[0]} : new float[]{p[0], p[1], p[3], r};
        float d = q[0] - Math.min(q[3], q[1]);
        return new float[]{Math.abs(q[2] + (q[3] - q[1]) / (6.0f * d + 1e-6f)), d / (q[0] + 1e-6f), q[0]};
    }

    public static Vector3f RGBToHSV(Vector3f color, Vector3f out) {
        if (out == null) out = new Vector3f();
        float[] array = RGBToHSVArray(color.x, color.y, color.z);
        out.set(array[0], array[1], array[2]);
        return out;
    }

    public static Vector3f RGBToHSV(Color color, Vector3f out) {
        return RGBToHSV(colorNormalization3f(color, out), out);
    }

    public static float[] HSVToRGBArray(float h, float s, float v) {
        float inX = h * 6.0f;
        float[] result = new float[]{inX, inX + 4.0f, inX + 2.0f};
        result[0] %= 6.0f;
        result[0] = Math.max(Math.min(Math.abs(result[0] - 3.0f) - 1.0f, 1.0f), 0.0f);
        result[1] %= 6.0f;
        result[1] = Math.max(Math.min(Math.abs(result[1] - 3.0f) - 1.0f, 1.0f), 0.0f);
        result[2] %= 6.0f;
        result[2] = Math.max(Math.min(Math.abs(result[2] - 3.0f) - 1.0f, 1.0f), 0.0f);
        float factorOM = 1.0f - s;
        result[0] = v * (factorOM + result[0] * s);
        result[1] = v * (factorOM + result[1] * s);
        result[2] = v * (factorOM + result[2] * s);
        return result;
    }

    public static Vector3f HSVToRGB(Vector3f color, Vector3f out) {
        if (out == null) out = new Vector3f();
        float[] array = HSVToRGBArray(color.x, color.y, color.z);
        out.set(array[0], array[1], array[2]);
        return out;
    }

    public static Color HSVToRGB(Vector3f color) {
        return toCommonColor(HSVToRGB(color, null));
    }

    public static Color HSVToRGB(Vector3f color, float alpha) {
        Vector3f out = HSVToRGB(color, null);
        return toCommonColor(new Vector4f(out.x, out.y, out.z, colorClamp(Math.round(alpha * 255.0f))));
    }

    public static Color toCommonColor(@NotNull Vector3f color) {
        int r = colorClamp(Math.round(color.x * 255.0f));
        int g = colorClamp(Math.round(color.y * 255.0f));
        int b = colorClamp(Math.round(color.z * 255.0f));
        return new Color(r, g, b);
    }

    public static Vector3f clampColor3f(@NotNull Vector3f color, Vector3f out) {
        if (out == null) out = new Vector3f();
        out.x = colorClamp(color.x);
        out.y = colorClamp(color.y);
        out.z = colorClamp(color.z);
        return out;
    }

    public static Color toCommonColor(@NotNull Vector4f color) {
        int r = colorClamp(Math.round(color.x * 255.0f));
        int g = colorClamp(Math.round(color.y * 255.0f));
        int b = colorClamp(Math.round(color.z * 255.0f));
        int a = colorClamp(Math.round(color.w * 255.0f));
        return new Color(r, g, b, a);
    }

    public static Vector4f clampColor4f(@NotNull Vector4f color, Vector4f out) {
        if (out == null) out = new Vector4f();
        out.x = colorClamp(color.x);
        out.y = colorClamp(color.y);
        out.z = colorClamp(color.z);
        out.w = colorClamp(color.w);
        return out;
    }

    public static Vector3f colorNormalization3f(@NotNull Color color, Vector3f out) {
        if (out == null) out = new Vector3f();
        out.x = color.getRed() / 255.0f;
        out.y = color.getGreen() / 255.0f;
        out.z = color.getBlue() / 255.0f;
        return out;
    }

    public static Vector4f colorNormalization4f(@NotNull Color color, Vector4f out) {
        if (out == null) out = new Vector4f();
        out.x = color.getRed() / 255.0f;
        out.y = color.getGreen() / 255.0f;
        out.z = color.getBlue() / 255.0f;
        out.w = color.getAlpha() / 255.0f;
        return out;
    }

    public static float[] colorNormalization3fArray(@NotNull Color color, float[] out) {
        if (out == null) out = new float[3];
        Vector3f tmp = colorNormalization3f(color, new Vector3f());
        out[0] = tmp.x;
        out[1] = tmp.y;
        out[2] = tmp.z;
        return out;
    }

    public static float[] colorNormalization4fArray(@NotNull Color color, float[] out) {
        if (out == null) out = new float[4];
        Vector4f tmp = colorNormalization4f(color, new Vector4f());
        out[0] = tmp.x;
        out[1] = tmp.y;
        out[2] = tmp.z;
        out[3] = tmp.w;
        return out;
    }

    public static byte[] colorToByteArray(Color color) {
        return new byte[]{(byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) color.getAlpha()};
    }

    public static byte[] color4fToByteArray(Vector4f color) {
        return new byte[]{(byte) (color.x * 255.0f), (byte) (color.y * 255.0f), (byte) (color.z * 255.0f), (byte) (color.w * 255.0f)};
    }

    /**
     * @param color length is 4
     */
    public static Color byteArrayToColor(byte... color) {
        return new Color(color[0] & 0xFF, color[1] & 0xFF, color[2] & 0xFF, color[3] & 0xFF);
    }

    /**
     * @param color length is 4
     */
    public static Vector4f byteArrayToColor4f(byte... color) {
        return new Vector4f((color[0] & 0xFF) / 255.0f, (color[1] & 0xFF) / 255.0f, (color[2] & 0xFF) / 255.0f, (color[3] & 0xFF) / 255.0f);
    }

    public static Vector4f colorNormalization4f(@NotNull Color color, float alpha) {
        return new Vector4f(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, alpha);
    }

    public static boolean isZeroVector3f(@NotNull Vector3f check) {
        return check.x == 0.0f && check.y == 0.0f && check.z == 0.0f;
    }

    public static boolean isZeroVector4f(@NotNull Vector4f check) {
        return check.x == 0.0f && check.y == 0.0f && check.z == 0.0f && check.w == 0.0f;
    }

    public static float[] getVector2fArray(@NotNull Vector2f vector) {
        return new float[]{vector.x, vector.y};
    }

    public static float[] getVector3fArray(@NotNull Vector3f vector) {
        return new float[]{vector.x, vector.y, vector.z};
    }

    public static float[] getVector4fArray(@NotNull Vector4f vector) {
        return new float[]{vector.x, vector.y, vector.z, vector.w};
    }

    public static float[] getMatrix2fArray(@NotNull Matrix2f matrix) {
        return new float[]{
                matrix.m00, matrix.m01,
                matrix.m10, matrix.m11};
    }

    public static float[] getMatrix3fArray(@NotNull Matrix3f matrix) {
        return new float[]{
                matrix.m00, matrix.m01, matrix.m02,
                matrix.m10, matrix.m11, matrix.m12,
                matrix.m20, matrix.m21, matrix.m22};
    }

    public static float[] getMatrix4fArray(@NotNull Matrix4f matrix) {
        return new float[]{
                matrix.m00, matrix.m01, matrix.m02, matrix.m03,
                matrix.m11, matrix.m11, matrix.m12, matrix.m13,
                matrix.m20, matrix.m21, matrix.m22, matrix.m23,
                matrix.m30, matrix.m31, matrix.m32, matrix.m33};
    }

    public static Matrix2f toMatrix2f(float[] array) {
        Matrix2f matrix = new Matrix2f();
        if (array.length < 4) return matrix;
        matrix.m00 = array[0];  matrix.m01 = array[1];
        matrix.m10 = array[2];  matrix.m11 = array[3];
        return matrix;
    }

    public static Matrix3f toMatrix3f(float[] array) {
        Matrix3f matrix = new Matrix3f();
        if (array == null || array.length < 9) return matrix;
        matrix.m00 = array[0];  matrix.m01 = array[1];  matrix.m02 = array[2];
        matrix.m10 = array[3];  matrix.m11 = array[4];  matrix.m12 = array[5];
        matrix.m20 = array[6];  matrix.m21 = array[7];  matrix.m22 = array[8];
        return matrix;
    }

    public static Matrix4f toMatrix4f(float[] array) {
        Matrix4f matrix = new Matrix4f();
        if (array == null || array.length < 16) return matrix;
        matrix.m00 = array[0];  matrix.m01 = array[1];  matrix.m02 = array[2];  matrix.m03 = array[3];
        matrix.m10 = array[4];  matrix.m11 = array[5];  matrix.m12 = array[6];  matrix.m13 = array[7];
        matrix.m20 = array[8];  matrix.m21 = array[9];  matrix.m22 = array[10];  matrix.m23 = array[11];
        matrix.m30 = array[12];  matrix.m31 = array[13];  matrix.m32 = array[14];  matrix.m33 = array[15];
        return matrix;
    }

    public static FloatBuffer createFloatBuffer(Vector2f vector) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(2);
        vector.store(buffer);
        buffer.flip();
        return buffer;
    }

    public static FloatBuffer createFloatBuffer(Vector2f... vectors) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vectors.length * 2);
        for (Vector2f vec : vectors) {
            vec.store(buffer);
        }
        buffer.flip();
        return buffer;
    }

    public static FloatBuffer createFloatBuffer(Vector3f vector) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(3);
        vector.store(buffer);
        buffer.flip();
        return buffer;
    }

    public static FloatBuffer createFloatBuffer(Vector3f... vectors) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vectors.length * 3);
        for (Vector3f vec : vectors) {
            vec.store(buffer);
        }
        buffer.flip();
        return buffer;
    }

    public static FloatBuffer createFloatBuffer(Vector4f vector) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(4);
        vector.store(buffer);
        buffer.flip();
        return buffer;
    }

    public static FloatBuffer createFloatBuffer(Vector4f... vectors) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vectors.length * 4);
        for (Vector4f vec : vectors) {
            vec.store(buffer);
        }
        buffer.flip();
        return buffer;
    }

    public static FloatBuffer createFloatBuffer(Matrix2f matrix) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(4);
        matrix.store(buffer);
        buffer.flip();
        return buffer;
    }

    public static FloatBuffer createFloatBuffer(Matrix2f... matrices) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(matrices.length * 4);
        for (Matrix2f mat : matrices) {
            mat.store(buffer);
        }
        buffer.flip();
        return buffer;
    }

    public static FloatBuffer createFloatBuffer(Matrix3f matrix) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(9);
        matrix.store(buffer);
        buffer.flip();
        return buffer;
    }

    public static FloatBuffer createFloatBuffer(Matrix3f... matrices) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(matrices.length * 9);
        for (Matrix3f mat : matrices) {
            mat.store(buffer);
        }
        buffer.flip();
        return buffer;
    }

    public static FloatBuffer createFloatBuffer(Matrix4f matrix) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
        matrix.store(buffer);
        buffer.flip();
        return buffer;
    }

    public static FloatBuffer createFloatBuffer(Matrix4f... matrices) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(matrices.length * 16);
        for (Matrix4f mat : matrices) {
            mat.store(buffer);
        }
        buffer.flip();
        return buffer;
    }

    public static PointerBuffer createPointerBuffer(long... array) {
        PointerBuffer buffer = BufferUtils.createPointerBuffer(array.length);
        for (int i = 0; i < array.length; ++i) buffer.put(i, array[i]);
        buffer.position(0);
        buffer.limit(buffer.capacity());
        return buffer;
    }

    public static DoubleBuffer createFloatBuffer(double... array) {
        DoubleBuffer buffer = BufferUtils.createDoubleBuffer(array.length);
        for (int i = 0; i < array.length; ++i) buffer.put(i, array[i]);
        buffer.position(0);
        buffer.limit(buffer.capacity());
        return buffer;
    }

    public static FloatBuffer createFloatBuffer(float... array) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(array.length);
        for (int i = 0; i < array.length; ++i) buffer.put(i, array[i]);
        buffer.position(0);
        buffer.limit(buffer.capacity());
        return buffer;
    }

    public static LongBuffer createLongBuffer(long... array) {
        LongBuffer buffer = BufferUtils.createLongBuffer(array.length);
        for (int i = 0; i < array.length; ++i) buffer.put(i, array[i]);
        buffer.position(0);
        buffer.limit(buffer.capacity());
        return buffer;
    }

    public static IntBuffer createIntBuffer(int... array) {
        IntBuffer buffer = BufferUtils.createIntBuffer(array.length);
        for (int i = 0; i < array.length; ++i) buffer.put(i, array[i]);
        buffer.position(0);
        buffer.limit(buffer.capacity());
        return buffer;
    }

    public static ShortBuffer createShortBuffer(short... array) {
        ShortBuffer buffer = BufferUtils.createShortBuffer(array.length);
        for (int i = 0; i < array.length; ++i) buffer.put(i, array[i]);
        buffer.position(0);
        buffer.limit(buffer.capacity());
        return buffer;
    }

    public static CharBuffer createShortBuffer(char... array) {
        CharBuffer buffer = BufferUtils.createCharBuffer(array.length);
        for (int i = 0; i < array.length; ++i) buffer.put(i, array[i]);
        buffer.position(0);
        buffer.limit(buffer.capacity());
        return buffer;
    }

    public static ByteBuffer createByteBuffer(byte... array) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(array.length);
        for (int i = 0; i < array.length; ++i) buffer.put(i, array[i]);
        buffer.position(0);
        buffer.limit(buffer.capacity());
        return buffer;
    }

    /**
     * ignore special values.
     */
    public static short float16ToShort(float src) {
        int bits = Float.floatToIntBits(src) >>> 13;
        return (short) ((bits & 0b1100000000000000000) >>> 3 | (bits & 0b11111111111111));
    }

    /**
     * ignore special values.
     */
    public static short[] float16ToShort(float... array) {
        short[] result = new short[array.length];
        for (int i = 0; i < array.length; i++) result[i] = float16ToShort(array[i]);
        return result;
    }

    /**
     * ignore special values.
     */
    public static ShortBuffer putFloat16(@NotNull ShortBuffer buffer, float src) {
        buffer.put(float16ToShort(src));
        return buffer;
    }

    /**
     * ignore special values.
     */
    public static ShortBuffer putFloat16(@NotNull ShortBuffer buffer, float... array) {
        for (float num : array) buffer.put(float16ToShort(num));
        return buffer;
    }

    public static short packingBytesToShort(byte i1, byte i0) {
        return (short) ((i1 << 8) | (i0 & 0xff));
    }

    public static short[] packingBytesToShort(byte[] i1, byte[] i0) {
        short[] result = new short[i1.length];
        for (int i = 0; i < i1.length; i++) result[i] = packingBytesToShort(i1[i], i0[i]);
        return result;
    }

    public static ShortBuffer putPackingBytes(@NotNull ShortBuffer buffer, int index, byte i1, byte i0) {
        buffer.put(index, packingBytesToShort(i1, i0));
        return buffer;
    }

    public static ShortBuffer putPackingBytes(@NotNull ShortBuffer buffer, byte i1, byte i0) {
        buffer.put(packingBytesToShort(i1, i0));
        return buffer;
    }

    public static ShortBuffer putPackingBytes(@NotNull ShortBuffer buffer, byte[] i1, byte[] i0) {
        for (int i = 0; i < i1.length; i++) buffer.put(packingBytesToShort(i1[i], i0[i]));
        return buffer;
    }

    public static int packingBytesToInt(byte i3, byte i2, byte i1, byte i0) {
        return (i3 << 24) | ((i2 << 16) & 0xff0000) | ((i1 << 8) & 0xff00) | (i0 & 0xff);
    }

    public static int[] packingBytesToInt(byte[] i3, byte[] i2, byte[] i1, byte[] i0) {
        int[] result = new int[i3.length];
        for (int i = 0; i < i3.length; i++) result[i] = packingBytesToInt(i3[i], i2[i], i1[i], i0[i]);
        return result;
    }

    public static IntBuffer putPackingBytes(@NotNull IntBuffer buffer, int index, byte i3, byte i2, byte i1, byte i0) {
        buffer.put(index, packingBytesToInt(i3, i2, i1, i0));
        return buffer;
    }

    public static IntBuffer putPackingBytes(@NotNull IntBuffer buffer, byte i3, byte i2, byte i1, byte i0) {
        buffer.put(packingBytesToInt(i3, i2, i1, i0));
        return buffer;
    }

    public static IntBuffer putPackingBytes(@NotNull IntBuffer buffer, byte[] i3, byte[] i2, byte[] i1, byte[] i0) {
        for (int i = 0; i < i3.length; i++) buffer.put(packingBytesToInt(i3[i], i2[i], i1[i], i0[i]));
        return buffer;
    }

    public static float packingBytesToFloat(byte i3, byte i2, byte i1, byte i0) {
        return Float.intBitsToFloat(packingBytesToInt(i3, i2, i1, i0));
    }

    public static float[] packingBytesToFloat(byte[] i3, byte[] i2, byte[] i1, byte[] i0) {
        float[] result = new float[i3.length];
        for (int i = 0; i < i3.length; i++) result[i] = packingBytesToFloat(i3[i], i2[i], i1[i], i0[i]);
        return result;
    }

    public static FloatBuffer putPackingBytes(FloatBuffer buffer, int index, byte i3, byte i2, byte i1, byte i0) {
        buffer.put(index,packingBytesToFloat(i3, i2, i1, i0));
        return buffer;
    }

    public static FloatBuffer putPackingBytes(@NotNull FloatBuffer buffer, byte i3, byte i2, byte i1, byte i0) {
        buffer.put(packingBytesToFloat(i3, i2, i1, i0));
        return buffer;
    }

    public static FloatBuffer putPackingBytes(@NotNull FloatBuffer buffer, byte[] i3, byte[] i2, byte[] i1, byte[] i0) {
        for (int i = 0; i < i3.length; i++) buffer.put(packingBytesToFloat(i3[i], i2[i], i1[i], i0[i]));
        return buffer;
    }

    public static byte normalizedFloatToByte(float src) {
        return src < 0 ? (byte) (Byte.MIN_VALUE * -src) : (byte) (Byte.MAX_VALUE * src);
    }

    public static byte[] normalizedFloatToByte(float... array) {
        byte[] result = new byte[array.length];
        for (int i = 0; i < array.length; i++)
            result[i] = array[i] < 0 ? (byte) (Byte.MIN_VALUE * -array[i]) : (byte) (Byte.MAX_VALUE * array[i]);
        return result;
    }

    public static ByteBuffer putNormalizedByte(@NotNull ByteBuffer buffer, float src) {
        buffer.put(normalizedFloatToByte(src));
        return buffer;
    }

    public static ByteBuffer putNormalizedByte(@NotNull ByteBuffer buffer, float... array) {
        for (float num : array) buffer.put(normalizedFloatToByte(num));
        return buffer;
    }

    /**
     * Return tangent and bi-tangent, array size is 2.
     */
    public static Vector3f[] tangentMaker(@NotNull Vector3f v1, @NotNull Vector3f v2, @NotNull Vector3f v3, @NotNull Vector2f u1, @NotNull Vector2f u2, @NotNull Vector2f u3) {
        Vector3f tangent = new Vector3f();
        Vector3f bitangent = new Vector3f();

        Vector3f edge1 = Vector3f.sub(v2, v1, null);
        Vector3f edge2 = Vector3f.sub(v3, v1, null);
        Vector2f deltaUV1 = Vector2f.sub(u2, u1, null);
        Vector2f deltaUV2 = Vector2f.sub(u3, u1, null);
        float f = 1.0f / (deltaUV1.getX() * deltaUV2.getY() - deltaUV2.getX() * deltaUV1.getY());

        tangent.setX(f * (deltaUV2.getY() * edge1.getX() - deltaUV1.getY() * edge2.getX()));
        tangent.setY(f * (deltaUV2.getY() * edge1.getY() - deltaUV1.getY() * edge2.getY()));
        tangent.setZ(f * (deltaUV2.getY() * edge1.getZ() - deltaUV1.getY() * edge2.getZ()));
        tangent.normalise(tangent);

        bitangent.setX(f * (-deltaUV2.getX() * edge1.getX() + deltaUV1.getX() * edge2.getX()));
        bitangent.setY(f * (-deltaUV2.getX() * edge1.getY() + deltaUV1.getX() * edge2.getY()));
        bitangent.setZ(f * (-deltaUV2.getX() * edge1.getZ() + deltaUV1.getX() * edge2.getZ()));
        bitangent.normalise(bitangent);

        return new Vector3f[]{tangent, bitangent};
    }

    public static Vector3f surfaceNormal(@NotNull Vector3f v1, @NotNull Vector3f v2, @NotNull Vector3f v3) {
        Vector3f a = Vector3f.sub(v1, v3, null);
        Vector3f b = Vector3f.sub(v2, v3, null);

        Vector3f normal = Vector3f.cross(a, b, null);
        normal.normalise(normal);
        return normal;
    }

    /**
     * @return int[] = {sizeX, sizeY, channelNum, pixelPreImage, ivec3(averageColor), ivec3(averageBrightColor)};<p>
     *     ByteBuffer = pixels value;
     */
    public static Pair<int[], ByteBuffer> getRawPixels(String file, int channelNum) {
        byte channel = (byte) Math.max(Math.min(channelNum, 4), 1);
        Pair<int[], ByteBuffer> result = new Pair<>(new int[10], null);
        double[] avc = new double[channel];
        if (file == null || file.isEmpty()) return result;
        try {
            InputStream inputStream = Global.getSettings().openStream(file);
            BufferedImage imageBuffer = ImageIO.read(new BufferedInputStream(inputStream));
            Raster data = imageBuffer.getData();

            String[] formatA = file.split("\\.");
            if (formatA.length < 1) return result;
            String format = formatA[formatA.length - 1].toLowerCase();
            final boolean isPNG = format.contentEquals("png");
            final boolean formatCheck = format.contentEquals("bmp") || format.contentEquals("jpg") || format.contentEquals("jpeg") || isPNG;
            if (!formatCheck) {
                Global.getLogger(CommonUtil.class).error("'BoxUtil' cannot loading file: '" + file + "', only support to reading 'bmp/jpg/jpeg/png'");
                return result;
            }
            result.one[0] = data.getWidth();
            result.one[1] = data.getHeight();
            result.one[2] = channel;
            result.one[3] = result.one[0] * result.one[1];
            result.two = BufferUtils.createByteBuffer(result.one[3] * channel);

            Global.getLogger(CommonUtil.class).info("'BoxUtil' loading sprite file: '" + file + "', with width " + result.one[0] + " and height " + result.one[1] + ", pixel have " + channel + " channels.");

            int[] pixels = new int[4];
            for (int y = result.one[1] - 1; y >= 0; --y) {
                for (int x = 0; x < result.one[0]; ++x) {
                    data.getPixel(x, y, pixels);
                    if (pixels[3] == 0) {
                        if (isPNG) pixels[0] = pixels[1] = pixels[2] = 0; else pixels[3] = 255;
                    }
                    for (int c = 0; c < channel; ++c) {
                        avc[c] += pixels[c];
                        result.two.put((byte) pixels[c]);
                    }
                }
            }

            result.two.position(0);
            result.two.limit(result.two.capacity());
            for (int i = 0; i < Math.min(avc.length, 3); ++i) {
                avc[i] = Math.round(avc[i] / result.one[3]);
                double gray = Math.round(avc[i] * _LINEAR[i]);
                result.one[4 + i] = Math.max(Math.min((int) avc[i], 255), 0);
                result.one[7 + i] = Math.max(Math.min((int) gray, 255), 0);
            }
            inputStream.close();
        } catch (IOException e) {
            Global.getLogger(CommonUtil.class).error("'BoxUtil' image loading failed: " + e);
            return result;
        }
        return result;
    }

    /**
     * Requires OpenGL 3.0 or higher and Framebuffer support.<p>
     * Get the level 0 texture object bytes data.
     *
     * @param target {@link GL11#GL_TEXTURE_1D} and {@link GL11#GL_TEXTURE_2D} only.
     * @param texture only support the 8bit per channel ubyte texture, and format must be {@link GL11#GL_RED}, {@link GL30#GL_RG}, {@link GL11#GL_RGB}, {@link GL11#GL_RGBA}.
     *
     * @return int[] = {width, height, internalFormat};<p>
     *     ByteBuffer = pixels value, null if format not support or failed;<p>
     *     channelNum returns 0 if failed or format not support or failed.
     */
    public static Pair<int[], ByteBuffer> getGLTexture(int target, int texture, byte channelNum) {
        if (!_GL_TRANSFER_FBO_INIT) {
            _GL_TRANSFER_FBO_INIT = true;
            _GL_TRANSFER_FBO = GL30.glGenFramebuffers();
        }
        final int[] format = new int[]{GL11.GL_RED, GL30.GL_RG, GL11.GL_RGB, GL11.GL_RGBA};
        final byte[] alignment = new byte[]{1, 2, 1, 4};
        final byte picker = (byte) Math.min(channelNum - 1, 3);
        Pair<int[], ByteBuffer> result = new Pair<>(new int[3], null);
        if (_GL_TRANSFER_FBO < 0 || texture == 0 || channelNum < 1) return result;
        GL11.glBindTexture(target, texture);
        result.one[0] = GL11.glGetTexLevelParameteri(target, 0, GL11.GL_TEXTURE_WIDTH);
        result.one[1] = GL11.glGetTexLevelParameteri(target, 0, GL11.GL_TEXTURE_HEIGHT);
        result.one[2] = GL11.glGetTexLevelParameteri(target, 0, GL11.GL_TEXTURE_INTERNAL_FORMAT);
        result.two = BufferUtils.createByteBuffer(result.one[0] * result.one[1] * channelNum);
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, alignment[picker]);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, _GL_TRANSFER_FBO);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, target, texture, 0);
        GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
        GL11.glReadPixels(0, 0, result.one[0], result.one[1], format[picker], GL11.GL_UNSIGNED_BYTE, result.two);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, target, 0, 0);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 4);
        return result;
    }

    /**
     * General method for save OpenGL texture object to local ARGB png format image.<p>
     * Root path is in <code>'gameRootDirectory'\</code> for default.
     *
     * @param path <strong>unneeded</strong> add a <code>\</code> at the start; set to null that save to <code>'gameRootDirectory'\saves\images\</code>.
     * @param saveFileName the name without format, length less than or equal 100; set to null that save to <code>"SavedTexture_'System.currentTimeMillis()'.png"</code>.
     * @param buffer default OpenGL order, LB = positionZero, TR = positionMax.
     * @param width the texture width.
     * @param height the texture height.
     * @param bufferChannel the OpenGL texture object channel num: <strong>1, 2, 3, 4</strong>. if less than <strong>4</strong>, will set alpha channel to <strong>255</strong>.
     *
     * @return true if success, false if failed.
     */
    public static boolean saveBytesImage(@Nullable String path, @Nullable String saveFileName, ByteBuffer buffer, int width, int height, byte bufferChannel) {
        if (!_INVOKE_INIT) {
            _INVOKE_INIT = true;
            Class<?> imageIOClass, fileClass;
            try {
                imageIOClass = Class.forName("javax.imageio.ImageIO", false, Class.class.getClassLoader());
                fileClass = Class.forName("java.io.File", false, Class.class.getClassLoader());
                _METHOD_HANDLE[0] = MethodHandles.lookup().findConstructor(fileClass, MethodType.methodType(void.class, String.class));
                _METHOD_HANDLE[1] = MethodHandles.lookup().findStatic(imageIOClass, "write", MethodType.methodType(boolean.class, RenderedImage.class, String.class, fileClass));
                _METHOD_HANDLE[2] = MethodHandles.lookup().findVirtual(fileClass, "exists", MethodType.methodType(boolean.class));
                _METHOD_HANDLE[3] = MethodHandles.lookup().findVirtual(fileClass, "mkdirs", MethodType.methodType(boolean.class));
                SEPARATOR = MethodHandles.lookup().findStaticGetter(fileClass, "separator", String.class).invoke().toString();
            } catch (java.lang.Throwable ignored) {}
        }
        if (_METHOD_HANDLE[0] == null || _METHOD_HANDLE[1] == null || _METHOD_HANDLE[2] == null || _METHOD_HANDLE[3] == null) {
            Global.getLogger(CommonUtil.class).error("'BoxUtil' image save failed, method is invalid.");
            return false;
        }
        if (width < 1 || height < 1 || bufferChannel < 1) {
            Global.getLogger(CommonUtil.class).error("'BoxUtil' image save failed, no a valid parameters: width= " + width + ", height= " + height + ", bufferChannel= " + bufferChannel);
            return false;
        }
        try {
            Path gameRootCore = Paths.get("").toAbsolutePath(), gameRoot;
            gameRoot = gameRootCore.getParent();
            String absolutePath = gameRoot == null ? gameRootCore.toString() : gameRoot.toString();
            String fixedSavePath = path;
            if (fixedSavePath == null) absolutePath += SEPARATOR + "saves" + SEPARATOR + "images";
            else if (!fixedSavePath.isEmpty()) {
                for (char check : new char[]{'\\', '/', ':', '*', '?', '"', '<', '>', '|', '\'', '.', '\n', '\r', '\t', '\0', '\f', '\b'}) fixedSavePath = fixedSavePath.replace(check, '_');
                absolutePath += SEPARATOR + fixedSavePath;
            }
            Object file = _METHOD_HANDLE[0].invoke(absolutePath);
            if (!(boolean) _METHOD_HANDLE[2].invoke(file)) _METHOD_HANDLE[3].invoke(file);
            String fixedSaveFileName = saveFileName;
            long currTime = System.currentTimeMillis();
            if (fixedSaveFileName == null || fixedSaveFileName.isEmpty()) {
                fixedSaveFileName = "";
            } else {
                for (char check : new char[]{'\\', '/', ':', '*', '?', '"', '<', '>', '|', '\'', '.', '\n', '\r', '\t', '\0', '\f', '\b'}) fixedSaveFileName = fixedSaveFileName.replace(check, '_');
            }
            if (fixedSaveFileName.isEmpty()) fixedSaveFileName = "SavedTexture_" + currTime;
            else if (fixedSaveFileName.length() > 100) fixedSaveFileName = fixedSaveFileName.substring(0, 100);
            absolutePath += SEPARATOR + fixedSaveFileName + ".png";
            file = _METHOD_HANDLE[0].invoke(absolutePath);

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            int bufferPose = 0, pixel;
            boolean setAlpha = bufferChannel < 4;
            for (int y = height - 1; y >= 0; --y) {
                for (int x = 0; x < width; ++x) {
                    pixel = 0x0;
                    for (byte c = 0; c < bufferChannel; ++c) {
                        pixel |= (buffer.get(bufferPose) << _IMAGE_SAVE_MOVE[c]) & _IMAGE_SAVE_MASK[c];
                        ++bufferPose;
                    }
                    if (setAlpha) pixel |= 0xFF000000;
                    image.setRGB(x, y, pixel);
                }
            }
            boolean success = (boolean) _METHOD_HANDLE[1].invoke(image, "png", file);
            if (success) Global.getLogger(CommonUtil.class).info("'BoxUtil' image saved: " + absolutePath);
            else Global.getLogger(CommonUtil.class).error("'BoxUtil' image save failed, no appropriate writer is found: " + absolutePath);
            return success;
        } catch (Throwable e) {
            Global.getLogger(CommonUtil.class).error("'BoxUtil' image save failed: " + e);
            return false;
        }
    }

    /**
     * Requires OpenGL 3.0 or higher and Framebuffer support.<p>
     * Save OpenGL texture object to local ARGB png format image.<p>
     * Root path is in <code>'gameRootDirectory'\</code> for default.
     *
     * @param path <strong>unneeded</strong> add a <code>\</code> at the start; set to null that save to <code>'gameRootDirectory'\saves\images\</code>.
     * @param saveFileName the name without format, length less than or equal 100; set to null that save to <code>"SavedTexture_'System.currentTimeMillis()'.png"</code>.
     * @param target {@link GL11#GL_TEXTURE_1D} and {@link GL11#GL_TEXTURE_2D} only.
     * @param texture only support the 8bit per channel ubyte texture, and format must be {@link GL11#GL_RED}, {@link GL30#GL_RG}, {@link GL11#GL_RGB}, {@link GL11#GL_RGBA}.
     *
     * @return true if success, false if failed.
     */
    public static boolean saveGLTexture(@Nullable String path, @Nullable String saveFileName, int target, int texture, byte channelNum) {
        Pair<int[], ByteBuffer> result = getGLTexture(target, texture, channelNum);
        if (result.one[0] < 1 || result.one[1] < 1 || result.two == null) return false;
        return saveBytesImage(path, saveFileName, result.two, result.one[0], result.one[1], channelNum);
    }

    public static void glDebug(String tags, Object info) {
        Global.getLogger(CommonUtil.class).info(tags + info);
        Global.getLogger(CommonUtil.class).info(GLU.gluErrorString(GL11.glGetError()));
        Util.checkGLError();
    }

    private CommonUtil() {}
}
