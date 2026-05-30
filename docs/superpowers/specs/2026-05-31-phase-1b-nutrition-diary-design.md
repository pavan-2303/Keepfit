# Keepfit Phase 1B Nutrition Diary Design

## Scope

Phase 1B adds an offline nutrition workflow that covers personal foods,
reusable meals, daily diary logging, duplicate-yesterday, and derived totals.
The implementation should feel as direct as the workout tracker: fast entry,
clear daily structure, and no remote catalog dependency.

## Decisions

- Add `feature:nutrition` for nutrition repositories, ViewModels, and Compose
  screens.
- Extend `core:database` with nutrition entities and DAO queries and migrate the
  Room schema from version `2` to `3`.
- Keep diary entries local-date based and derive totals from stored foods and
  servings.
- Add `isFavorite` to `Food` even though the original architecture draft did
  not include it. This matches the roadmap requirement for favorites and is the
  smallest durable model change.
- Add optional daily nutrition goals to the single local `BodyProfile` row:
  `dailyCalorieGoal`, `dailyProteinGoalGrams`, `dailyCarbohydrateGoalGrams`,
  and `dailyFatGoalGrams`. This provides a source of truth for total-vs-goal
  comparisons before a dedicated settings feature exists.
- Keep recent foods as a derived query over recent diary entries rather than a
  separate table.
- Expanding a saved meal creates ordinary `FoodDiaryEntry` rows that retain the
  originating `savedMealId`.

## User Flow

The Nutrition tab will have three compact sections:

1. Today summary:
   Show calorie and macro totals with goal comparisons when goals exist.
2. Diary:
   Show `Breakfast`, `Lunch`, `Dinner`, and `Snacks` sections for the selected
   date, each with logged foods or meals plus servings.
3. Library:
   Support food search, favorites, recents, and saved meals in add-entry flows.

Primary actions:

- Create a food.
- Create a saved meal from foods already in the library.
- Add a food or saved meal to a meal section for the selected date.
- Duplicate the previous day's diary to the selected date.
- Archive a food while preserving old diary rows.

## Validation and Constraints

- `Food.name` and `Food.servingLabel` are required.
- `Food.servingAmount` must be strictly positive.
- Calories and macro fields must be non-negative.
- Diary servings and saved-meal servings must be strictly positive.
- Archived foods remain readable in history and existing diary entries.
- Duplicate-yesterday replaces only the selected date's entries after copying
  succeeds in one transaction.

## Testing Strategy

- Room instrumentation tests:
  food search, favorites, recents, saved meal expansion, duplicate-yesterday,
  archive readability, and profile migration from schema `2` to `3`.
- JVM tests:
  food validation, meal-item validation, and diary serving validation.
- Emulator verification:
  create food, create meal, log diary entries, duplicate yesterday, confirm
  totals, and confirm archived food remains visible in old entries.
