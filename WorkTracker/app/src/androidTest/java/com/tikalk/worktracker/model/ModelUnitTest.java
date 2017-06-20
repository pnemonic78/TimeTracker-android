package com.tikalk.worktracker.model;

import android.content.Context;
import android.content.res.AssetManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tikalk.worktracker.time.model.TimeRecord;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Modifier;
import java.util.Calendar;
import java.util.TimeZone;

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

        Gson gson = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.FINAL, Modifier.TRANSIENT, Modifier.STATIC)
                .create();
        TimeRecord[] records = gson.fromJson(reader, TimeRecord[].class);
        reader.close();

        assertNotNull(records);
        assertEquals(2, records.length);

        TimeRecord record = records[0];
        assertNotNull(record);
        assertEquals(100L, record.getId().longValue());
        assertEquals(1, record.getVersion());
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(0L);
        calendar.set(2017, Calendar.JUNE, 1, 9, 00, 00);
        assertEquals(calendar.getTime(), record.getStart());
        calendar.set(2017, Calendar.JUNE, 1, 18, 00, 00);
        assertEquals(calendar.getTime(), record.getFinish());
        User user = record.getUser();
        assertNotNull(user);
        assertEquals(10L, user.getId().longValue());
        assertEquals("test", user.getUsername());
        assertEquals("test@tikalk.com", user.getEmail());
        ProjectTask task = record.getTask();
        assertNotNull(task);
        assertEquals(101L, task.getId().longValue());
        assertEquals("General", task.getName());
        Project project = task.getProject();
        assertNotNull(project);
        assertEquals(1L, project.getId().longValue());
        assertEquals("Tikal", project.getName());
    }

    private Reader openReader(String name) throws IOException {
        Context context = InstrumentationRegistry.getContext();
        AssetManager assets = context.getAssets();
        InputStream in = assets.open(name);
        assertNotNull(in);
        return new InputStreamReader(in);
    }
}
