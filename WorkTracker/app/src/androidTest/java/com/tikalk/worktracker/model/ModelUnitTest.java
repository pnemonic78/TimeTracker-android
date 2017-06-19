package com.tikalk.worktracker.model;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;

/**
 * Test the data model.
 *
 * @author moshe on 2017/06/19.
 */
@RunWith(AndroidJUnit4.class)
public class ModelUnitTest {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("com.tikalk.worktracker", appContext.getPackageName());
    }
}
