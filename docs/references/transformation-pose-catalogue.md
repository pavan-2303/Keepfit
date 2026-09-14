# Transformation Pose Catalogue

This reference defines the stable pose taxonomy approved for future Keepfit
transformation check-ins. It describes product behavior only; the current app
continues to use four photo angles until the corresponding version is
implemented and migrated.

## Pose Set

| Stable key | Label | Group | Default | Guidance purpose |
|---|---|---|---|---|
| `front_relaxed` | Front relaxed | Basic | Yes | Overall front proportions and posture |
| `right_side_relaxed` | Right side relaxed | Basic | Yes | Side profile and posture |
| `back_relaxed` | Back relaxed | Basic | Yes | Overall back proportions and posture |
| `left_side_relaxed` | Left side relaxed | Basic | Yes | Opposite side profile and posture |
| `front_hands_on_hips` | Front - hands on hips | Standard | No | Waist and shoulder proportions |
| `front_double_biceps` | Front double biceps | Flexed | No | Arms, shoulders, and upper-body development |
| `back_double_biceps` | Back double biceps | Flexed | No | Back, shoulders, and arms |
| `right_side_flexed` | Right side flexed | Flexed | No | Right-side chest, arms, legs, and glutes |
| `left_side_flexed` | Left side flexed | Flexed | No | Left-side chest, arms, legs, and glutes |
| `abs_and_core` | Abs and core | Flexed | No | Midsection definition and core presentation |
| `chest_focused` | Chest focused | Detailed | No | Chest presentation |
| `back_lat_spread` | Back lat spread | Detailed | No | Back width and thickness presentation |
| `side_chest_right` | Side chest - right | Detailed | No | Right-side chest and upper-body presentation |
| `side_chest_left` | Side chest - left | Detailed | No | Left-side chest and upper-body presentation |
| `legs_focused` | Legs focused | Detailed | No | Quadriceps, hamstrings, and calves |

The four Basic poses are selected for every new profile. Any of the other
eleven may be enabled or disabled individually. A user may save an incomplete
check-in even when an enabled pose is missing.

## Migration Mapping

| Existing value | New stable key |
|---|---|
| `FRONT` | `front_relaxed` |
| `RIGHT` | `right_side_relaxed` |
| `BACK` | `back_relaxed` |
| `LEFT` | `left_side_relaxed` |

The migration preserves capture date, cycle, file path, type, size, and import
time. The unique record constraint becomes cycle, capture date, and pose key.

## Capture and Comparison Behavior

- Present enabled poses as a guided sequence with a clear `captured / enabled`
  count.
- Let the user skip, replace, or revisit any pose without blocking the session.
- Compare only identical stable pose keys across two selected dates.
- When one side is missing, explain which date lacks the selected pose and
  offer another available pose or date.
- Use a reference thumbnail and optional alignment overlay as framing guidance,
  not as an appearance target.
- Encourage consistent camera height, distance, lighting, background, clothing,
  and time of day while keeping the guidance optional and non-judgmental.
- Keep relaxed poses neutral and reserve flexing cues for explicitly flexed
  poses.

## Reference Artwork Rules

- Do not crop, trace, or bundle the product-review poster unless its ownership
  and redistribution rights are separately documented.
- Produce original Keepfit artwork or acquire a licence that explicitly permits
  application redistribution.
- Prefer neutral silhouettes or illustrations that do not imply one ideal body
  type, gender, skin tone, or fitness level.
- Store source, creator, licence, allowed resolution, attribution, and approval
  status for every bundled asset.
- Reference images never leave the APK. User transformation photos never leave
  app-private storage except through explicit encrypted backup.

## Photo Processing Requirements

- Validate JPEG, PNG, and WebP imports before replacing an existing photo.
- Remove EXIF location and device metadata from the app-private copy.
- Correct orientation and bound image dimensions to a documented maximum while
  retaining adequate comparison detail.
- Preserve the existing file until the new processed file and Room update both
  succeed.
- Include processed files and pose metadata in explicit encrypted backup and
  restore validation.
