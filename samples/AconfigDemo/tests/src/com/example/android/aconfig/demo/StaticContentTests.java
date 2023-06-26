package com.example.android.aconfig.demo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.example.android.aconfig.demo.flags.Flags;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class StaticContentTests {

    @Test
    public void testFlag() {
        assertFalse(Flags.appendStaticContent());
    }
}