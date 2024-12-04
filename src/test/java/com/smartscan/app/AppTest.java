package com.smartscan.app;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AppTest
{
    @Test
    public void testAppMainFalse() {
    	App.main(null);
    	assertTrue(Boolean.FALSE);
    }
    
    @Test
    public void testAppMainTrue() {
    	assertTrue(Boolean.TRUE);
    }
}
