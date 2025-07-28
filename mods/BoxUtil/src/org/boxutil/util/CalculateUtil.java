package org.boxutil.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BoundsAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.util.Misc;
import org.boxutil.define.BoxDatabase;
import org.boxutil.units.standard.attribute.NodeData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lazywizard.lazylib.CollisionUtils;
import org.lwjgl.util.vector.*;

import java.awt.*;
import java.util.*;
import java.util.List;

public final class CalculateUtil {
    private static float clampF(float value, float min, float max) {
        return Math.max(Math.min(value, max), min);
    }

    private static int clampC(int value) {
        return Math.max(Math.min(value, 255), 0);
    }

    private static float clampC(float value) {
        return Math.max(Math.min(value, 255), 0);
    }

    public static int stepI(float edge, float value) {
        return value < edge ? 0 : 1;
    }

    public static float stepF(float edge, float value) {
        return value < edge ? 0.0f : 1.0f;
    }

    public static float smoothstep(float edgeL, float edgeR, float value) {
        float result = clampF((value - edgeL) / (edgeR - edgeL), 0.0f, 1.0f);
        return result * result * (3.0f - 2.0f * result);
    }

    public static float inverseLerp(float edgeL, float edgeR, float value) {
        return (value - edgeL) / (edgeR - edgeL);
    }

    public static Vector2f reflect(Vector2f vector, Vector2f normal, Vector2f result) {
        if (result == null) result = new Vector2f();
        float dot = Vector2f.dot(normal, vector) * 2.0f;
        return Vector2f.sub(vector, new Vector2f(normal.x * dot, normal.y * dot), result);
    }

    public static Vector3f reflect(Vector3f vector, Vector3f normal, Vector3f result) {
        if (result == null) result = new Vector3f();
        float dot = Vector3f.dot(normal, vector) * 2.0f;
        return Vector3f.sub(vector, new Vector3f(normal.x * dot, normal.y * dot, normal.z * dot), result);
    }

    public static boolean pointAtRight(Vector2f point, Vector2f forward) {
        return Vector2f.dot(forward, point) >= 0.0f;
    }

    public static boolean pointAtRight(Vector2f point, CombatEntityAPI entity) {
        float cosValue = (float) Math.cos(Math.toRadians(entity.getFacing()));
        return (cosValue * point.x + TrigUtil.sinFormCosF(cosValue, entity.getFacing()) * point.y) >= 0.0f;
    }

    public static boolean pointAtFront(Vector2f point, Vector2f forward) {
        return (forward.y * point.x + forward.x * point.y) >= 0.0f;
    }

    public static boolean pointAtFront(Vector2f point, CombatEntityAPI entity) {
        float cosValue = (float) Math.cos(Math.toRadians(entity.getFacing()));
        return (TrigUtil.sinFormCosF(cosValue, entity.getFacing()) * point.x + cosValue * point.y) >= 0.0f;
    }

    private static float[] intersectionLineCircle(Vector2f rayFrom, Vector2f facingVec, Vector2f circle, float radius, float lineLength) {
        Vector2f distance = Vector2f.sub(circle, rayFrom, new Vector2f());
        float dSQ = distance.lengthSquared();
        float projection = Vector2f.dot(distance, facingVec);
        float r2 = radius * radius;
        float tangentHalfSQ = projection * projection - dSQ + r2;
        return new float[]{facingVec.x, facingVec.y, tangentHalfSQ, projection, dSQ, r2, lineLength};
    }

    private static float[] intersectionRayCircle(Vector2f rayFrom, float rayFacing, Vector2f circle, float radius) {
        Vector2f facingVec = new Vector2f((float) Math.cos(Math.toRadians(rayFacing)), 0.0f);
        facingVec.y = TrigUtil.sinFormCosF(facingVec.x, rayFacing);
        return intersectionLineCircle(rayFrom, facingVec, circle, radius, 1.0f);
    }

    private static float[] intersectionSegmentCircle(Vector2f start, Vector2f end, Vector2f circle, float radius) {
        Vector2f facingVec = Vector2f.sub(end, start, new Vector2f());
        float beamLength = facingVec.length();
        facingVec.scale(1.0f / beamLength);
        return intersectionLineCircle(start, facingVec, circle, radius, beamLength);
    }

    public static boolean checkIntersectionRayCircle(Vector2f rayFrom, float rayFacing, Vector2f circle, float radius) {
        float[] check = intersectionRayCircle(rayFrom, rayFacing, circle, radius);
        return check[4] <= check[5] || (check[2] >= 0.0f && check[3] >= 0.0f);
    }

    public static @Nullable Vector2f getNearestIntersectionRayCircle(Vector2f rayFrom, float rayFacing, Vector2f circle, float radius) {
        float[] check = intersectionRayCircle(rayFrom, rayFacing, circle, radius);
        boolean pointOutside = check[4] > check[5];
        if (pointOutside && (check[2] < 0.0f || check[3] < 0.0f)) return null;
        float sSqrt = (float) Math.sqrt(check[2]);
        if (pointOutside) sSqrt = -sSqrt;
        float length = check[3] + sSqrt;
        return new Vector2f(check[0] * length + rayFrom.x, check[1] * length + rayFrom.y);
    }

    /**
     * Without shield arc check.
     */
    public static @Nullable Vector2f getNearestIntersectionRayShield(Vector2f rayFrom, float rayFacing, Vector2f circle, float radius) {
        float[] check = intersectionRayCircle(rayFrom, rayFacing, circle, radius);
        if (check[4] <= check[5]) return rayFrom;
        if (check[2] < 0.0f || check[3] < 0.0f) return null;
        float length = check[3] - (float) Math.sqrt(check[2]);
        return new Vector2f(check[0] * length + rayFrom.x, check[1] * length + rayFrom.y);
    }

    public static @Nullable Vector2f getNearestIntersectionSegmentCircle(Vector2f start, Vector2f end, Vector2f circle, float radius) {
        float[] check = intersectionSegmentCircle(start, end, circle, radius);
        boolean pointOutside = check[4] > check[5];
        if (pointOutside && (check[2] < 0.0f || check[3] < 0.0f)) return null;
        float sSqrt = (float) Math.sqrt(check[2]);
        if (pointOutside) sSqrt = -sSqrt;
        float length = check[3] + sSqrt;
        if (length > check[6]) return null;
        return new Vector2f(check[0] * length + start.x, check[1] * length + start.y);
    }

    /**
     * Without shield arc check.
     */
    public static @Nullable Vector2f getNearestIntersectionSegmentShield(Vector2f start, Vector2f end, Vector2f circle, float radius) {
        float[] check = intersectionSegmentCircle(start, end, circle, radius);
        if (check[4] <= check[5]) return start;
        if (check[2] < 0.0f || check[3] < 0.0f) return null;
        float length = check[3] - (float) Math.sqrt(check[2]);
        if (length > check[6]) return null;
        return new Vector2f(check[0] * length + start.x, check[1] * length + start.y);
    }

    /**
     * @param rotate angle of ZXY.
     */
    public static Vector3f getPointOnSphere(@Nullable Vector3f center, float radius, Vector3f rotate) {
        if (center == null)  center = new Vector3f(0.0f, 0.0f, 0.0f);
        if (radius == 0.0f) return center;
        Vector3f outPoint = new Vector3f(center.x, center.y, center.z);
        Vector4f point = new Vector4f(radius, 0.0f, 0.0f, 1.0f);
        Matrix4f.transform(TransformUtil.createModelMatrixRotateOnly(TransformUtil.rotationZXY(rotate.x, rotate.y, rotate.z), new Matrix4f()), point, point);
        return Vector3f.add(outPoint, new Vector3f(point.x, point.y, point.z), outPoint);
    }

    public static Vector3f getRandomPointOnSphere(@Nullable Vector3f center, float radius) {
        return getPointOnSphere(center, radius, new Vector3f((float) (Math.random() * 360.0d), (float) (Math.random() * 360.0d), (float) (Math.random() * 360.0d)));
    }

    public static Vector3f scaleFormCenter(@Nullable Vector3f center, float factor, @NotNull Vector3f target, @Nullable Vector3f out) {
        if (center == null) center = new Vector3f(0.0f, 0.0f, 0.0f);
        if (out == null) out = new Vector3f(0.0f, 0.0f, 0.0f);
        Vector3f.sub(target, center, out);
        out.scale(factor);
        Vector3f.add(target, center, out);
        return out;
    }

    public static byte mix(byte src, byte dst, float factor) {
        return (byte) Math.round(src * (1.0f - factor) + dst * factor);
    }
    public static int mix(int src, int dst, float factor) {
        return Math.round(src * (1.0f - factor) + dst * factor);
    }

    public static float mix(float src, float dst, float factor) {
        return src * (1.0f - factor) + dst * factor;
    }

    public static double mix(double src, double dst, double factor) {
        return src * (1.0f - factor) + dst * factor;
    }

    public static Color mix(Color src, Color dst, boolean mixAlpha, float factor) {
        return new Color(
                clampC(mix(src.getRed(), dst.getRed(), factor)),
                clampC(mix(src.getGreen(), dst.getGreen(), factor)),
                clampC(mix(src.getBlue(), dst.getBlue(), factor)),
                mixAlpha ? src.getAlpha() : mix(src.getAlpha(), dst.getAlpha(), factor)
        );
    }

    public static Vector2f mix(Vector2f src, Vector2f dst, @Nullable Vector2f result, float factor) {
        if (result == null) result = new Vector2f();
        result.x = mix(src.x, dst.x, factor);
        result.y = mix(src.y, dst.y, factor);
        return result;
    }

    public static Vector3f mix(Vector3f src, Vector3f dst, @Nullable Vector3f result, float factor) {
        if (result == null) result = new Vector3f();
        result.x = mix(src.x, dst.x, factor);
        result.y = mix(src.y, dst.y, factor);
        result.z = mix(src.z, dst.z, factor);
        return result;
    }

    public static Vector4f mix(Vector4f src, Vector4f dst, @Nullable Vector4f result, float factor) {
        if (result == null) result = new Vector4f();
        result.x = mix(src.x, dst.x, factor);
        result.y = mix(src.y, dst.y, factor);
        result.z = mix(src.z, dst.z, factor);
        result.w = mix(src.w, dst.w, factor);
        return result;
    }

    public static byte[] mix(byte[] src, byte[] dst, float factor) {
        byte[] out = new byte[src.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = mix(src[i], dst[i], factor);
        }
        return out;
    }

    public static int[] mix(int[] src, int[] dst, float factor) {
        int[] out = new int[src.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = mix(src[i], dst[i], factor);
        }
        return out;
    }

    public static float[] mix(float[] src, float[] dst, float factor) {
        float[] out = new float[src.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = mix(src[i], dst[i], factor);
        }
        return out;
    }

    public static double[] mix(double[] src, double[] dst, float factor) {
        double[] out = new double[src.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = mix(src[i], dst[i], factor);
        }
        return out;
    }

    public static float[] addAll(float[] src, float[]... dst) {
        float[] result = Arrays.copyOf(src, src.length);
        for (float[] array : dst) {
            for (int i = 0; i < result.length; i++) {
                result[i] += array[i];
            }
        }
        return result;
    }

    public static float[] add(float[] src, float[] dst) {
        float[] out = new float[src.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = src[i] + dst[i];
        }
        return out;
    }

    public static float[] add(float[] src, float dst) {
        float[] out = new float[src.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = src[i] + dst;
        }
        return out;
    }

    public static float[] subAll(float[] src, float[]... dst) {
        float[] result = Arrays.copyOf(src, src.length);
        for (float[] array : dst) {
            for (int i = 0; i < result.length; i++) {
                result[i] -= array[i];
            }
        }
        return result;
    }

    public static float[] sub(float[] src, float[] dst) {
        float[] out = new float[src.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = src[i] - dst[i];
        }
        return out;
    }

    public static float[] sub(float[] src, float dst) {
        float[] out = new float[src.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = src[i] - dst;
        }
        return out;
    }

    public static float[] mulAll(float[] src, float[]... dst) {
        float[] result = Arrays.copyOf(src, src.length);
        for (float[] array : dst) {
            for (int i = 0; i < result.length; i++) {
                result[i] *= array[i];
            }
        }
        return result;
    }

    public static float[] mul(float[] src, float[] dst) {
        float[] out = new float[src.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = src[i] * dst[i];
        }
        return out;
    }

    public static float[] mul(float[] src, float dst) {
        float[] out = new float[src.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = src[i] * dst;
        }
        return out;
    }

    public static float[] divAll(float[] src, float[]... dst) {
        float[] result = Arrays.copyOf(src, src.length);
        for (float[] array : dst) {
            for (int i = 0; i < result.length; i++) {
                result[i] /= array[i];
            }
        }
        return result;
    }

    public static float[] div(float[] src, float[] dst) {
        float[] out = new float[src.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = src[i] / dst[i];
        }
        return out;
    }

    public static float[] div(float[] src, float dst) {
        float[] out = new float[src.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = src[i] / dst;
        }
        return out;
    }

    public static Vector2f clamp(Vector2f vector, float min, float max) {
        vector.x = clampF(vector.x, min, max);
        vector.y = clampF(vector.y, min, max);
        return vector;
    }

    public static Vector2f clamp(Vector2f vector, Vector2f min, Vector2f max) {
        vector.x = clampF(vector.x, min.x, max.x);
        vector.y = clampF(vector.y, min.y, max.y);
        return vector;
    }

    public static Vector3f clamp(Vector3f vector, float min, float max) {
        vector.x = clampF(vector.x, min, max);
        vector.y = clampF(vector.y, min, max);
        vector.z = clampF(vector.z, min, max);
        return vector;
    }

    public static Vector3f clamp(Vector3f vector, Vector3f min, Vector3f max) {
        vector.x = clampF(vector.x, min.x, max.x);
        vector.y = clampF(vector.y, min.y, max.y);
        vector.z = clampF(vector.z, min.z, max.z);
        return vector;
    }

    public static Vector4f clamp(Vector4f vector, float min, float max) {
        vector.x = clampF(vector.x, min, max);
        vector.y = clampF(vector.y, min, max);
        vector.z = clampF(vector.z, min, max);
        vector.w = clampF(vector.w, min, max);
        return vector;
    }

    public static Vector4f clamp(Vector4f vector, Vector4f min, Vector4f max) {
        vector.x = clampF(vector.x, min.x, max.x);
        vector.y = clampF(vector.y, min.y, max.y);
        vector.z = clampF(vector.z, min.z, max.z);
        vector.w = clampF(vector.w, min.w, max.w);
        return vector;
    }

    public static float[] clamp(float[] array, float[] min, float[] max) {
        float[] out = new float[array.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = clampF(array[i], min[i], max[i]);
        }
        return out;
    }

    public static float[] clamp(float[] array, float min, float max) {
        float[] out = new float[array.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = clampF(array[i], min, max);
        }
        return out;
    }

    public static Vector2f min(Vector2f left, Vector2f right, Vector2f result) {
        if (result == null) result = new Vector2f();
        result.set(Math.min(left.x, right.x), Math.min(left.y, right.y));
        return result;
    }

    public static Vector3f min(Vector3f left, Vector3f right, Vector3f result) {
        if (result == null) result = new Vector3f();
        result.set(Math.min(left.x, right.x), Math.min(left.y, right.y), Math.min(left.z, right.z));
        return result;
    }

    public static Vector4f min(Vector4f left, Vector4f right, Vector4f result) {
        if (result == null) result = new Vector4f();
        result.set(Math.min(left.x, right.x), Math.min(left.y, right.y), Math.min(left.z, right.z), Math.min(left.w, right.w));
        return result;
    }

    public static Vector2f max(Vector2f left, Vector2f right, Vector2f result) {
        if (result == null) result = new Vector2f();
        result.set(Math.max(left.x, right.x), Math.max(left.y, right.y));
        return result;
    }

    public static Vector3f max(Vector3f left, Vector3f right, Vector3f result) {
        if (result == null) result = new Vector3f();
        result.set(Math.max(left.x, right.x), Math.max(left.y, right.y), Math.max(left.z, right.z));
        return result;
    }

    public static Vector4f max(Vector4f left, Vector4f right, Vector4f result) {
        if (result == null) result = new Vector4f();
        result.set(Math.max(left.x, right.x), Math.max(left.y, right.y), Math.max(left.z, right.z) , Math.max(left.w, right.w));
        return result;
    }

    /**
     * @param input cannot too large.
     */
    public static float fraction(float input) {
        return input - (long) input;
    }

    /**
     * Returns value of 2^n that less than input.
     */
    public static int getPOTMin(int input) {
        if (input < 2) return 0;
        return 1 << (31 - Integer.numberOfLeadingZeros(input));
    }

    /**
     * Returns value of 2^n that greater than input.
     */
    public static int getPOTMax(int input) {
        if (input < 2) return 1;
        int tmp = getPOTMin(input);
        if (tmp < input) tmp <<= 1;
        return tmp;
    }

    /**
     * Returns n value of 2^n that less than input.
     */
    public static byte getExponentPOTMin(int input) {
        if (input < 1) return 0;
        return (byte) (31 - Integer.numberOfLeadingZeros(input));
    }

    /**
     * Returns n value of 2^n that greater than input.
     */
    public static byte getExponentPOTMax(int input) {
        if (input < 1) return 0;
        byte bit = getExponentPOTMin(input);
        if ((1 << bit) < input) ++bit;
        return bit;
    }

    public static int getStepMax(int input, int step) {
        return input % step == 0 ? input : input + (step - input % step);
    }

    private CalculateUtil() {}
}
