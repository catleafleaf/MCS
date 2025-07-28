package org.boxutil.manager;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.jetbrains.annotations.Nullable;
import org.apache.log4j.Level;
import org.boxutil.define.BoxDatabase;
import org.boxutil.units.builtin.legacy.LegacyModelData;
import org.boxutil.units.builtin.legacy.array.Stack2f;
import org.boxutil.units.builtin.legacy.array.Stack3f;
import org.boxutil.units.builtin.legacy.array.TriIndex;
import org.boxutil.units.standard.attribute.ModelData;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class ModelManager {
    private static final HashMap<String, ModelData> _BUtil_ModelDataMap = new HashMap<>();

    /**
     * General method for reading model files, when game starting.<p>
     * Cannot import any files then bigger than 16MB for user's device.<p>
     * DO NOT CONTAIN N-GONS
     */
    public static HashMap<String, ModelData> loadModelDataCSV(String path) {
        try {
            return wavefrontOBJCSVLoadCore(path);
        } catch (JSONException | IOException e) {
            Global.getLogger(ModelManager.class).log(Level.ERROR, "'BoxUtil' models csv data loading failed at: '" + path + "'." + e.getMessage());
            return null;
        }
    }

    public static ModelData addModelData(String initID, String objPath, @Nullable String diffusePath, @Nullable String normalPath, @Nullable String aoPath, @Nullable String emissivePath) {
        return addModelData(initID, objPath, diffusePath, normalPath, aoPath, emissivePath, GL30.GL_HALF_FLOAT, GL30.GL_HALF_FLOAT);
    }

    /**
     * Add one of model data by manual.<p>
     * Not recommended to use an existing ID, only when you know what you are doing.<p>
     * Cannot import any files then bigger than 16MB for user's device.<p>
     * DO NOT CONTAIN N-GONS
     */
    public static ModelData addModelData(String initID, String objPath, @Nullable String diffusePath, @Nullable String normalPath, @Nullable String aoPath, @Nullable String emissivePath, int type, int tbnType) {
        try {
            return addOBJDataCore(initID, objPath, diffusePath, normalPath, aoPath, emissivePath, type, tbnType);
        } catch (IOException e) {
            Global.getLogger(ModelManager.class).log(Level.ERROR, "'BoxUtil' loading '" + initID + "' failed at: '" + objPath + "'." + e.getMessage());
            return null;
        }
    }

    /**
     * @see #addModelData(String, String, String, String, String, String, int, int)
     */
    public static ModelData addModelData(String id, ModelData obj) {
        return _BUtil_ModelDataMap.put(id, obj);
    }

    public static ModelData getModelData(String id) {
        return _BUtil_ModelDataMap.get(id);
    }

    public static boolean containsModelID(String id) {
        return _BUtil_ModelDataMap.containsKey(id);
    }

    /**
     * For your custom shader, use triangle mode to draw.<p>
     * Will not pull in this manager's hashmap.<p>
     * It is raw import.<p>
     * Cannot import any files then bigger than 16MB for user's device.<p>
     * DO NOT CONTAIN N-GONS
     *
     * @return Should save it, such as add it to a static 'List'.
     */
    public static LegacyModelData getLegacyModelData(String modelPath) {
        try {
            return getLegacyModelDataCore(modelPath);
        } catch (IOException e) {
            Global.getLogger(ModelManager.class).log(Level.ERROR, "'BoxUtil' model data loading failed at: '" + modelPath + "'." + e.getMessage());
            return null;
        }
    }

    private static ModelData addOBJDataCore(String initID, String objPath, String diffusePath, String normalPath, String aoPath, String emissivePath, int type, int tbnType) throws IOException {
        String objFile = Global.getSettings().loadText(objPath);
        if (objFile.getBytes().length > BoxDatabase.MAX_MODEL_FILE_SIZE) {
            Global.getLogger(ModelManager.class).log(Level.WARN, "'BoxUtil' model ID '" + initID + "' at path '" + objPath + "' was too bigger than 16MB.");
            return null;
        }
        BufferedReader reader = new BufferedReader(new StringReader(objFile));
        List<Vector3f> vertex = new ArrayList<>();
        List<Vector3f> normal = new ArrayList<>();
        List<Vector2f> uv = new ArrayList<>();
        List<TriIndex> tri = new ArrayList<>();

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("v ")) {
                String[] splitString = line.split(" ");
                float x = Float.parseFloat(splitString[1]);
                float y = Float.parseFloat(splitString[2]);
                float z = Float.parseFloat(splitString[3]);
                vertex.add(new Vector3f(x, y, z));
            } else if (line.startsWith("vn ")) {
                String[] splitString = line.split(" ");
                float x = Float.parseFloat(splitString[1]);
                float y = Float.parseFloat(splitString[2]);
                float z = Float.parseFloat(splitString[3]);
                Vector3f normalTmp = new Vector3f(x, y, z).normalise(null);
                normal.add(normalTmp);
            } else if (line.startsWith("vt ")) {
                String[] splitString = line.split(" ");
                float u = Float.parseFloat(splitString[1]);
                float v = Float.parseFloat(splitString[2]);
                uv.add(new Vector2f(u, v));
            } else if (line.startsWith("f ")) {
                String[] splitString = line.split(" ");
                String p1 = splitString[1];
                String p2 = splitString[2];
                String p3 = splitString[3];
                String p4 = splitString[splitString.length - 1];
                if (p3.contains(p4)) {
                    tri.add(new TriIndex(p1, p2, p3));
                } else {
                    tri.add(new TriIndex(p1, p2, p3));
                    tri.add(new TriIndex(p3, p4, p1));
                }
            }
        }

        SpriteAPI diffuse;
        SpriteAPI normalMap;
        SpriteAPI ao;
        SpriteAPI emissive;
        if (diffusePath == null || diffusePath.isEmpty()) diffuse = BoxDatabase.BUtil_ONE; else diffuse = Global.getSettings().getSprite(diffusePath);
        if (normalPath == null || normalPath.isEmpty()) normalMap = BoxDatabase.BUtil_Z; else normalMap = Global.getSettings().getSprite(normalPath);
        if (aoPath == null || aoPath.isEmpty()) ao = BoxDatabase.BUtil_ONE; else ao = Global.getSettings().getSprite(aoPath);
        if (emissivePath == null || emissivePath.isEmpty()) emissive = BoxDatabase.BUtil_NONE; else emissive = Global.getSettings().getSprite(emissivePath);

        Global.getLogger(ModelManager.class).info("'BoxUtil' loaded common OBJ data with ID: '" + initID + "', at path: '" + objPath + "'.");
        Global.getLogger(ModelManager.class).info("'BoxUtil' OBJ data ID: '" + initID + "' have vertices count: " + vertex.size() + " and triangles count: " + tri.size() + ".");
        return _BUtil_ModelDataMap.put(initID, new ModelData(initID, vertex, normal, uv, tri, diffuse, normalMap, ao, emissive, type, tbnType));
    }

    private static HashMap<String, ModelData> wavefrontOBJCSVLoadCore(String path) throws JSONException, IOException {
        HashMap<String, ModelData> map = new HashMap<>();
        JSONArray objDataArray = Global.getSettings().loadCSV(path);
        for (int i = 0; i < objDataArray.length(); i++) {
            JSONObject objData = objDataArray.getJSONObject(i);
            if (!objData.optString("obj_id").isEmpty() && !objData.optString("obj_path").isEmpty()) {
                String objID = objData.getString("obj_id");
                String objPath = objData.getString("obj_path");
                String diffusePath = null;
                String normalPath = null;
                String aoPath = null;
                String emissivePath = null;
                int type = GL30.GL_HALF_FLOAT;
                int tbnType = GL30.GL_HALF_FLOAT;
                if (!objData.getString("diffuse_path").isEmpty()) diffusePath = objData.optString("diffuse_path");
                if (!objData.getString("normal_path").isEmpty()) normalPath = objData.optString("normal_path");
                if (!objData.getString("ao_path").isEmpty()) aoPath = objData.optString("ao_path");
                if (!objData.getString("emissive_path").isEmpty()) emissivePath = objData.optString("emissive_path");
                if (!objData.getString("type").isEmpty()) {
                    String typeString = objData.optString("type").toUpperCase();
                    type = typeString.contentEquals("F8") ? GL11.GL_BYTE : typeString.contentEquals("F32") ? GL11.GL_FLOAT : GL30.GL_HALF_FLOAT;
                }
                if (!objData.getString("type_tbn").isEmpty()) {
                    String typeString = objData.optString("type_tbn").toUpperCase();
                    tbnType = typeString.contentEquals("F8") ? GL11.GL_BYTE : typeString.contentEquals("F32") ? GL11.GL_FLOAT : GL30.GL_HALF_FLOAT;
                }
                ModelData obj = addModelData(objID, objPath, diffusePath, normalPath, aoPath, emissivePath, type, tbnType);
                if (obj != null) map.put(objID, obj);
            }
        }
        return map;
    }

    private static LegacyModelData getLegacyModelDataCore(String modelPath) throws IOException {
        String objFile = Global.getSettings().loadText(modelPath);
        if (objFile.getBytes().length > BoxDatabase.MAX_MODEL_FILE_SIZE) {
            Global.getLogger(ModelManager.class).log(Level.WARN, "'BoxUtil' model file at '" + modelPath + "' was too bigger than 16MB.");
            return null;
        }
        BufferedReader reader = new BufferedReader(new StringReader(objFile));
        List<Stack3f> vertex = new ArrayList<>();
        List<Stack3f> vNormal = new ArrayList<>();
        List<Stack2f> vUV = new ArrayList<>();
        List<TriIndex> tri = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("v ")) {
                String[] splitString = line.split(" ");
                float x = Float.parseFloat(splitString[1]);
                float y = Float.parseFloat(splitString[2]);
                float z = Float.parseFloat(splitString[3]);
                vertex.add(new Stack3f(x, y, z));
            } else if (line.startsWith("vn ")) {
                String[] splitString = line.split(" ");
                float x = Float.parseFloat(splitString[1]);
                float y = Float.parseFloat(splitString[2]);
                float z = Float.parseFloat(splitString[3]);
                vNormal.add(new Stack3f(x, y, z));
            } else if (line.startsWith("vt ")) {
                String[] splitString = line.split(" ");
                float u = Float.parseFloat(splitString[1]);
                float v = Float.parseFloat(splitString[2]);
                vUV.add(new Stack2f(u, v));
            } else if (line.startsWith("f ")) {
                String[] splitString = line.split(" ");
                String p1 = splitString[1];
                String p2 = splitString[2];
                String p3 = splitString[3];
                String p4 = splitString[splitString.length - 1];
                if (p3.contains(p4)) {
                    tri.add(new TriIndex(p1, p2, p3));
                } else {
                    tri.add(new TriIndex(p1, p2, p3));
                    tri.add(new TriIndex(p3, p4, p1));
                }
            }
        }

        Global.getLogger(ModelManager.class).info("'BoxUtil' loaded legacy OBJ data at path: '" + modelPath + "'.");
        return new LegacyModelData(vertex.toArray(new Stack3f[0]), vNormal.toArray(new Stack3f[0]), vUV.toArray(new Stack2f[0]), tri.toArray(new TriIndex[0]));
    }

    private ModelManager() {}
}
