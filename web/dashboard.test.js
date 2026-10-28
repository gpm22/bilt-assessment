import test from "node:test";
import assert from "node:assert/strict";

import { buildViewModel } from "./dashboard.js";

const member = {
  pointsThisMonth: 1_500,
  monthlyCap: 100_000,
  streakMonths: 6,
};

test("shows awarded points as a successful payment", () => {
  const view = buildViewModel(
    { pointsAwarded: 1_500, outcome: "AWARDED" },
    member,
  );

  assert.equal(view.title, "1,500 points credited");
  assert.equal(view.description, "Your rent payment was processed successfully.");
  assert.equal(view.tone, "success");
  assert.equal(view.progressPercent, 1.5);
});

test("shows a duplicate event as skipped rather than credited", () => {
  const view = buildViewModel(
    { pointsAwarded: 0, outcome: "DUPLICATE" },
    member,
  );

  assert.equal(view.title, "Duplicate event skipped");
  assert.equal(
    view.description,
    "This payment was already processed. No additional points were awarded.",
  );
  assert.equal(view.tone, "neutral");
});

test("shows when the member has reached the monthly cap", () => {
  const view = buildViewModel(
    { pointsAwarded: 0, outcome: "CAPPED" },
    { ...member, pointsThisMonth: 100_000 },
  );

  assert.equal(view.title, "Monthly cap reached");
  assert.equal(
    view.description,
    "You have reached your monthly points cap. No more points can be awarded this month.",
  );
  assert.equal(view.tone, "warning");
  assert.equal(view.progressPercent, 100);
});

test("falls back to a neutral status for unknown outcomes", () => {
  const view = buildViewModel(
    { pointsAwarded: 0, outcome: "SOMETHING_ELSE" },
    member,
  );

  assert.equal(view.title, "Status unavailable");
  assert.equal(view.description, "We could not determine this payment outcome.");
  assert.equal(view.tone, "neutral");
});

test("clamps progress to 100 when points exceed monthly cap", () => {
  const view = buildViewModel(
    { pointsAwarded: 500, outcome: "AWARDED" },
    { ...member, pointsThisMonth: 120_000 },
  );

  assert.equal(view.progressPercent, 100);
});
