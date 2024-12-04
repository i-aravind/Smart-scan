package com.smartscan.app;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.smartscan.dto.TestDTO;

public class AppTest
{
    @Test
    public void testAppMainFalse() {
    	App.main(null);
    	TestDTO testDTO = new TestDTO();
    	testDTO.setField1(1);
    	testDTO.setField2("Test");
    	assertTrue(Boolean.FALSE);
    }
    
    @Test
    public void testAppMainTrue() {
    	assertTrue(Boolean.TRUE);
    }
}
