package ru.drivemod;

import org.junit.Test;
import static org.junit.Assert.*;
import static ru.drivemod.PairLaunchMachine.Action.*;

public class PairLaunchMachineTest {
    @Test public void waitsForFirstWindowBeforeOpeningSecond() {
        PairLaunchMachine m = new PairLaunchMachine(); m.begin(1000);
        assertEquals(NONE, m.observe(false, false, false, 2000));
        assertEquals(REQUEST_SPLIT, m.observe(true, false, false, 2200));
        assertEquals(NONE, m.observe(true, false, false, 2400));
        assertEquals(OPEN_SECOND, m.observe(true, true, false, 2600));
        assertEquals(NONE, m.observe(true, true, false, 2700));
        assertEquals(READY, m.observe(true, true, true, 3000));
        assertEquals(NONE, m.observe(true, true, true, 3100));
    }
    @Test public void reusesExistingSplitWithoutTogglingItOff() {
        PairLaunchMachine m = new PairLaunchMachine(); m.begin(0);
        assertEquals(OPEN_SECOND, m.observe(true, true, false, 300));
        assertEquals(READY, m.observe(true, true, true, 600));
    }
    @Test public void missingGlobalSplitActionFallsBackOnlyOnce() {
        PairLaunchMachine m = new PairLaunchMachine(); m.begin(0);
        assertEquals(REQUEST_SPLIT, m.observe(true, false, false, 100));
        assertEquals(OPEN_SECOND, m.observe(true, false, false, 1100));
        assertEquals(NONE, m.observe(false, false, false, 1200));
    }
    @Test public void oneFullscreenAppNeverCountsAsSuccess() {
        PairLaunchMachine m = new PairLaunchMachine(); m.begin(0);
        m.observe(true, false, false, 100); m.observe(true, false, false, 1100);
        assertEquals(FAIL, m.observe(false, false, false, 10100));
        assertEquals(PairLaunchMachine.Phase.FAILED, m.phase());
        assertEquals(NONE, m.observe(true, true, true, 11000));
    }
    @Test public void firstAppTimeoutStopsStartupAndNewSessionResetsIt() {
        PairLaunchMachine m = new PairLaunchMachine(); m.begin(0);
        assertEquals(FAIL, m.observe(false, false, false, 8000));
        m.begin(10000); assertEquals(REQUEST_SPLIT, m.observe(true, false, false, 10100));
        m.stop(); assertEquals(NONE, m.observe(true, true, true, 10200));
    }
}
