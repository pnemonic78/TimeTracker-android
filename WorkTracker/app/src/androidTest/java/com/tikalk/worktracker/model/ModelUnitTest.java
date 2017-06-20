package com.tikalk.worktracker.model;

import android.content.Context;
import android.content.res.AssetManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.google.gson.Gson;
import com.tikalk.worktracker.time.model.TimeRecord;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * Test the data model.
 *
 * @author moshe on 2017/06/19.
 */
@RunWith(AndroidJUnit4.class)
public class ModelUnitTest {
    @Test
    public void time1() throws Exception {
        Reader reader = openReader("time/time-1.json");
        assertNotNull(reader);

        Gson gson = new Gson();
        TimeRecord[] records = gson.fromJson(reader, TimeRecord[].class);
        reader.close();

        assertNotNull(records);
        assertEquals(2, records.length);
    }

    private Reader openReader(String name) throws IOException {
        Context context = InstrumentationRegistry.getContext();
        AssetManager assets = context.getAssets();
        InputStream in = assets.open(name);
        assertNotNull(in);
        return new InputStreamReader(in);
    }
}
