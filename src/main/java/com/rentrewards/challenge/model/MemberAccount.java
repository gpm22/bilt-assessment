package com.rentrewards.challenge.model;

import java.time.YearMonth;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal in-memory representation of a member's rewards account.
 * In production this would be backed by a database; for this challenge
 * an in-memory model is enough to exercise the business rules.
 */
public class MemberAccount {

    private final String memberId;
    private int currentStreakMonths;
    private final Map<YearMonth, Long> pointsByMonth = new ConcurrentHashMap<>();

    public MemberAccount(String memberId, int currentStreakMonths) {
        this.memberId = memberId;
        this.currentStreakMonths = currentStreakMonths;
    }

    public String getMemberId() {
        return memberId;
    }

    public int getCurrentStreakMonths() {
        return currentStreakMonths;
    }

    public long getPointsForMonth(YearMonth month) {
        return pointsByMonth.getOrDefault(month, 0L);
    }

    public void addPointsForMonth(YearMonth month, long points) {
        pointsByMonth.merge(month, points, Long::sum);
    }

    /**
     * Atomically adds as many of the given points as fit under the cap for that month.
     *
     * @return the points actually added, 0 if the cap was already reached
     */
    public long addPointsUpToCap(YearMonth month, long points, long cap) {
        long[] added = new long[1];
        pointsByMonth.compute(month, (key, earned) -> {
            long current = earned == null ? 0L : earned;
            added[0] = Math.min(points, Math.max(0, cap - current));
            return current + added[0];
        });
        return added[0];
    }
}
