package ru.drivemod;

/** Timing and one-shot actions are independent of Android so startup races can be tested. */
final class PairLaunchMachine {
    enum Phase { IDLE, WAIT_FIRST, WAIT_SPLIT, WAIT_PAIR, ACTIVE, FAILED }
    enum Action { NONE, REQUEST_SPLIT, OPEN_SECOND, READY, FAIL }
    private Phase phase = Phase.IDLE;
    private long since;
    Phase phase() { return phase; }
    void begin(long now) { phase = Phase.WAIT_FIRST; since = now; }
    void stop() { phase = Phase.IDLE; }
    void fail() { phase = Phase.FAILED; }
    Action observe(boolean firstVisible, boolean firstPartial, boolean confirmedPair, long now) {
        if (phase == Phase.WAIT_FIRST) {
            if (firstVisible) {
                since = now;
                if (firstPartial) { phase = Phase.WAIT_PAIR; return Action.OPEN_SECOND; }
                phase = Phase.WAIT_SPLIT; return Action.REQUEST_SPLIT;
            }
            if (now - since >= 8000) { phase = Phase.FAILED; return Action.FAIL; }
        } else if (phase == Phase.WAIT_SPLIT) {
            // Some Android 12L+ launchers expose no global split action. LAUNCH_ADJACENT is the fallback.
            if (firstPartial || now - since >= 1000) {
                phase = Phase.WAIT_PAIR; since = now; return Action.OPEN_SECOND;
            }
        } else if (phase == Phase.WAIT_PAIR) {
            if (confirmedPair) { phase = Phase.ACTIVE; return Action.READY; }
            if (now - since >= 9000) { phase = Phase.FAILED; return Action.FAIL; }
        }
        return Action.NONE;
    }
}
