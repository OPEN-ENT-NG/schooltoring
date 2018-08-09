package fr.openent.schooltoring.utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class Utils {

    /**
     * Extract String values from JsonObject array based on key parameter
     *
     * @param arr Array containing Json objects
     * @param key Key to extract
     * @return JsonArray containing every values
     */
    public static JsonArray extractStringValues(JsonArray arr, String key) {
        JsonArray values = new JsonArray();
        JsonObject o;

        for (int i = 0; i < arr.size(); i++) {
            o = arr.getJsonObject(i);
            values.add(o.getString(key));
        }

        return values;
    }

    /**
     * Extract features ids from a stringify sql array in Json objects array
     *
     * @param arr Array containing Json objects
     * @param key Key to extract. If null, default value is "features"
     * @return JsonArray containing every features id
     */
    public static JsonArray extractFeaturesId(JsonArray arr, String key) {
        key = (key == null ? "features" : key);
        JsonObject o;
        JsonArray tmpFeatures, values = new JsonArray();

        for (int i = 0; i < arr.size(); i++) {
            o = arr.getJsonObject(i);
            tmpFeatures = new JsonArray(o.getString(key));
            for (int j = 0; j < tmpFeatures.size(); j++) {
                values.add(tmpFeatures.getString(j));
            }
        }

        return values;
    }

    /**
     * Map arr in object
     *
     * @param arr Array containing objects to map
     * @param key key used as key map. If null, default value is "id"
     * @return
     */
    public static JsonObject mapObjectsWithStringKeys(JsonArray arr, String key) {
        key = (key == null ? "id" : key);
        JsonObject map = new JsonObject(), tmpO;

        for (int i = 0; i < arr.size(); i++) {
            tmpO = arr.getJsonObject(i);
            map.put(tmpO.getString(key), tmpO);
        }

        return map;
    }
}
