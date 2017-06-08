package com.tikalk.worktracker.model;

import android.net.Uri;

import com.activeandroid.serializer.TypeSerializer;

/**
 * Uri serializer.
 *
 * @author Moshe Waisberg.
 */
public class UriTypeSerializer extends TypeSerializer {
    @Override
    public Class<?> getDeserializedType() {
        return Uri.class;
    }

    @Override
    public Class<?> getSerializedType() {
        return String.class;
    }

    @Override
    public String serialize(Object data) {
        if (data == null) {
            return null;
        }
        return data.toString();
    }

    @Override
    public Uri deserialize(Object data) {
        if (data == null) {
            return null;
        }
        return Uri.parse(data.toString());
    }
}
