package com.example.android.aconfig.demo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import com.example.android.aconfig.demo.flags.FeatureFlags;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class InjectedContentTests {

    @Test
    public void testInjectedContentFlagOn() throws Exception {
        FeatureFlags fakeFeatureFlag = mock(FeatureFlags.class);
        when(fakeFeatureFlag.appendInjectedContent()).thenReturn(true);
        InjectedContent injectedContent = new InjectedContent(fakeFeatureFlag);
        StringBuilder expected = new StringBuilder();
        expected.append("The flag: appendInjectedContent is ON!!\n\n");
        assertEquals("Get appendInjectedContent", expected.toString(), injectedContent.getContent());
    }
}