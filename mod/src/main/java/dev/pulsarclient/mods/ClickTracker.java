package dev.pulsarclient.mods;

import java.util.ArrayDeque;
import java.util.Deque;

/** Guarda os cliques do último segundo para calcular o CPS. */
public final class ClickTracker {
    private static final Deque<Long> LEFT = new ArrayDeque<Long>();
    private static final Deque<Long> RIGHT = new ArrayDeque<Long>();

    private ClickTracker() {}

    public static void click(int button) {
        if (button == 0) LEFT.addLast(System.currentTimeMillis());
        else if (button == 1) RIGHT.addLast(System.currentTimeMillis());
    }

    public static int left() { return count(LEFT); }
    public static int right() { return count(RIGHT); }

    private static int count(Deque<Long> clicks) {
        long limit = System.currentTimeMillis() - 1000L;
        while (!clicks.isEmpty() && clicks.peekFirst() < limit) clicks.pollFirst();
        return clicks.size();
    }
}
