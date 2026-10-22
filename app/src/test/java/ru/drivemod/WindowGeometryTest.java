package ru.drivemod;

import org.junit.Test;
import static org.junit.Assert.*;

public class WindowGeometryTest {
    @Test public void portraitAndLandscapePairsAreRecognised() {
        assertTrue(WindowGeometry.separated(new int[]{0,24,1080,950}, new int[]{0,962,1080,2300},1080,2400));
        assertTrue(WindowGeometry.separated(new int[]{0,24,1150,1000}, new int[]{1162,24,2400,1000},2400,1080));
    }
    @Test public void fullscreenWithPictureInPictureIsNotSplit() {
        assertFalse(WindowGeometry.separated(new int[]{0,24,1080,2300},new int[]{600,1800,1050,2200},1080,2400));
    }
    @Test public void missingAndTinyWindowsAreRejected() {
        assertFalse(WindowGeometry.separated(null,new int[]{0,0,1080,1200},1080,2400));
        assertFalse(WindowGeometry.separated(new int[]{0,0,1080,1200},new int[]{0,2000,100,2100},1080,2400));
    }
}
