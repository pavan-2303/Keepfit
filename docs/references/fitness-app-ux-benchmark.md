# Fitness App UX Benchmark

> **Reviewed:** 2026-09-13
> **Purpose:** Product input for Keepfit's post-v0.9 journey reorganization

## Why this review exists

Keepfit has useful local features, but first-run questions, plan creation,
daily execution, detailed logs, and configuration were added at different
times. The result exposes too many equally weighted controls and makes a user
assemble their own journey. This review identifies interaction patterns worth
adapting without copying the commercial, account-first, or sales-heavy parts
of other products.

## Products reviewed

| Product | Useful pattern | What Keepfit should avoid |
|---|---|---|
| [cult.fit](https://play.google.com/store/apps/details?id=fit.cure.android) | Goal, current level, target, preferred format, and schedule are collected as focused choices; a selected beginner program places one session on Today | The super-app storefront, memberships, booking, commerce, and crowded discovery surfaces |
| [FITPASS FITCOACH](https://fitpass.co.in/fitcoach) | A short health-and-preference profile drives one prominent daily routine with Start and a clear alternative action | Marketing-heavy AI claims and treating a generated result as automatically safe or correct |
| [Fitbod](https://help.fitbod.me/hc/en-us/articles/34336407191191-My-Plan) | My Plan groups goal, location/equipment, workouts per week, duration, experience, split, and variability while retaining recommended defaults | Exposing advanced split and progression controls before a beginner needs them |
| [Freeletics](https://www.freeletics.com/en/blog/posts/getting-started-with-freeletics/) | Onboarding feeds a named training journey; Today adapts for time, equipment, space, soreness, and feedback after each session | A mandatory account/subscription and an unnecessarily long questionnaire without visible benefit |

Additional cult.fit screen references show a progression from current fitness
level to goal, target, preferred format, and workout schedule. FITPASS material
documents height, weight, age, gender, fitness goal, workout type, and preferred
time. These are plan inputs, not reasons to collect every health field.

## Current Keepfit usability findings

1. Registration asks for name and height, then the inputs that actually shape
   a plan appear later in Workouts. The transition from setup to a useful week
   is therefore easy to miss.
2. Today is directionally correct, but large cards give setup, status,
   secondary metrics, and primary action similar visual weight.
3. Feature pages mix frequent actions with infrequent administration. Creating
   exercises, managing templates, browsing catalogues, and starting today's
   session compete for attention.
4. Long vertical pages reveal too much at once. A person must understand the
   product structure before completing a simple task.
5. The assistant begins as a large access panel plus task/proposal surface,
   leaving too little room for replies. Connection state should be compact and
   conversation should own the screen.

## Keepfit direction

The next UX slice after v0.9 should reorganize the journey around three layers:

```text
First run                  Daily use                 Management
---------                  ---------                 ----------
Goal and experience        One next action           Plan and preferences
Days and duration          Compact supporting cues   Libraries and history
Equipment and limits       Start / adapt / log       Integrations and backup
Tracking preference        Review result             Detailed editing
Review starter week
```

### Guided first run

Use seven short, skippable steps with a progress indicator:

1. primary goal;
2. current experience and recent activity;
3. realistic days per week and preferred days;
4. normal session duration and preferred time;
5. training location and equipment;
6. movements to avoid, framed as preference rather than injury treatment;
7. nutrition-tracking depth and optional body inputs.

Name is collected only when it helps personalize copy. Height, weight, age,
and other body data remain optional and are never required for a workout plan.
The app explains why each optional field is useful. The final step previews the
deterministic starter week before applying it.

### Daily hierarchy

- Keep exactly one primary action above the fold: resume or start today's plan.
- Show workout duration, exercise count, and the most relevant constraint in a
  compact line rather than a large informational card.
- Place Shorten, Move, Substitute, and Skip behind one `Adjust` action.
- Show nutrition, steps, and review as compact supporting rows; expand only on
  selection.
- Move configuration, libraries, backup, and integrations out of daily paths.

### Navigation proposal

Use five primary destinations: Today, Plan, Log, Progress, and Coach. Put
Settings in the top-level profile menu. Plan owns the starter journey, weekly
schedule, and templates. Log owns workout history and nutrition entry.
Progress owns review, records, trends, and transformation. Coach owns ordinary
questions and read-only insights over relevant local aggregates. Exercise and
food libraries become contextual management screens rather than permanent
full-page sections.

### Coach hierarchy

- Give responses and the composer most of the viewport.
- Reduce access state to one status row with connection management in an
  overflow menu.
- Show a few useful starter questions only when the conversation is empty.
- Add local history only for questions about the user's own progress and show
  when that context was used.
- Let OpenRouter and its providers enforce the user's account limits; do not
  add a second Keepfit daily allowance.

## Visual direction for the reorganization

This is a functional training log, not a content marketplace. The visual system
should feel like a calm training notebook:

- base: `#F7F8F3` chalk white;
- ink: `#18201B` deep green-black;
- action: `#196B4A` training green;
- progress: `#D9772B` warm effort orange;
- recovery: `#DDE9E1` quiet sage;
- warning: `#B23A35` restrained red.

Use one typographic family with a compact, strong title scale and tabular
figures for workout values. Reserve large type for the one current action or a
meaningful result. Use fewer containers, smaller corner radii, and dividers or
spacing to group related rows. A card must represent one actionable object,
not merely decorate a paragraph.

## Acceptance direction for the next slice

- A new user reaches a reviewed starter week through one uninterrupted flow.
- A returning user can start today's workout without scrolling.
- No normal page presents more than one dominant call to action.
- Infrequent setup is at least one interaction away from daily execution.
- Large-font, one-handed reach, TalkBack labels, and back behavior are verified.
- Existing local data and every current feature remain reachable after the
  navigation change.

This benchmark is product research, not permission to reproduce another app's
branding, proprietary copy, or screen layouts.
