package com.rentrewards.challenge.service;

import com.rentrewards.challenge.model.MemberAccount;
import com.rentrewards.challenge.model.PaymentEvent;
import com.rentrewards.challenge.model.PointsResult;
import com.rentrewards.challenge.model.ProcessingOutcome;

import java.time.YearMonth;

/**
 * Orchestrates the processing of an incoming payment webhook event:
 *
 *  1. Ignore the event if it is a duplicate delivery.
 *     First delivery is recorded during this check.
 *  2. Calculate base points (with linked-account multiplier).
 *  3. Apply streak bonus if the member is eligible.
 *  4. Enforce the monthly points cap per member.
 *  5. Return the outcome and awarded points.
 */
public class RewardsEngine {

    private static final long MONTHLY_POINTS_CAP = 100_000L;

    private final PointsCalculator pointsCalculator;
    private final ProcessedEventStore processedEventStore;

    public RewardsEngine(PointsCalculator pointsCalculator, ProcessedEventStore processedEventStore) {
        this.pointsCalculator = pointsCalculator;
        this.processedEventStore = processedEventStore;
    }

    public PointsResult processPayment(PaymentEvent event, MemberAccount member) {
        if (!processedEventStore.isFirstDelivery(event.getEventId())) {
            return new PointsResult(member.getMemberId(), 0, ProcessingOutcome.DUPLICATE);
        }

        long basePoints = pointsCalculator.calculateBasePoints(event);
        long pointsWithBonus = pointsCalculator.applyStreakBonusIfEligible(
                basePoints, member.getCurrentStreakMonths());

        YearMonth month = YearMonth.from(event.getPaymentDate());
        long pointsToAward = member.addPointsUpToCap(month, pointsWithBonus, MONTHLY_POINTS_CAP);

        ProcessingOutcome outcome = pointsToAward == 0
                ? ProcessingOutcome.CAPPED
                : ProcessingOutcome.AWARDED;
        return new PointsResult(member.getMemberId(), pointsToAward, outcome);
    }
}
