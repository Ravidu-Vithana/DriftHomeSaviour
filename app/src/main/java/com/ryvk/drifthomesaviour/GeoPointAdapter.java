package com.ryvk.drifthomesaviour;

import com.google.firebase.firestore.GeoPoint;
import com.google.gson.*;

import java.lang.reflect.Type;

public class GeoPointAdapter implements JsonSerializer<GeoPoint>, JsonDeserializer<GeoPoint> {

    @Override
    public JsonElement serialize(GeoPoint src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("latitude", src.getLatitude());
        jsonObject.addProperty("longitude", src.getLongitude());
        return jsonObject;
    }

    @Override
    public GeoPoint deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        double latitude = jsonObject.get("latitude").getAsDouble();
        double longitude = jsonObject.get("longitude").getAsDouble();
        return new GeoPoint(latitude, longitude);
    }
}