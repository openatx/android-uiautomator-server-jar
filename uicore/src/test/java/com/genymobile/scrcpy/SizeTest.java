package com.genymobile.scrcpy;

import org.junit.Test;
import static org.junit.Assert.*;

public class SizeTest {
    @Test
    public void rotate() {
        Size s = new Size(1920, 1080).rotate();
        assertEquals(1080, s.getWidth());
        assertEquals(1920, s.getHeight());
    }
}
