# Exercise Catalogue Rights and Content Register

This register records the source, storage rule, and release status of exercise
guidance displayed by Keepfit. Recheck third-party terms before every public
release that includes remote catalogue access.

## Source Register

| Source | Content | Current use | Persistence and caching | Public-release status |
|---|---|---|---|---|
| Keepfit offline guide | Names, categories, equipment tags, and concise instructions for 40 exercises | Bundled offline search, starter planning, and explicit addition to the personal library | Application code; no bundled media | Approved for this private repository; text was authored specifically for Keepfit |
| User private library | User-authored fields and explicitly imported MP4, WebM, or GIF files | Editable local exercises and private demonstrations | Room metadata plus app-private media; included only in explicit encrypted backups | User-controlled private content |
| AscendAPI ExerciseDB V1 free hosted API | Live exercise metadata, instructions, and 180p GIF URL | Explicit, view-only private prototype search | No Room, file, DataStore, backup, memory-cache, or disk-cache persistence; fetch fresh on every submitted search | Not approved for public release or import until commercial, attribution, caching, and persistence rights are confirmed in writing |
| hasaneyldrm exercises dataset | 1,324 exercise records with names, categories, equipment, muscles, and multilingual instructions | Approved candidate for an audited build-time offline metadata import | Pin an audited commit; copy only MIT-covered data and instruction text; preserve licence and provenance | Metadata and instruction text approved for implementation planning under MIT; Gym visual images and GIFs are excluded without a separate licence |

## Keepfit Offline Guide Inventory

Every entry below has original concise text and no bundled image, video, or GIF.
The stable key is part of starter-plan compatibility.

| Movement | Stable keys |
|---|---|
| Squat and legs | `chair-squat`, `bodyweight-squat`, `goblet-squat`, `leg-press`, `standing-calf-raise` |
| Single leg | `reverse-lunge`, `step-up`, `split-squat`, `dumbbell-reverse-lunge` |
| Hinge and glutes | `glute-bridge`, `dumbbell-romanian-deadlift`, `hip-hinge-drill`, `single-leg-glute-bridge`, `dumbbell-deadlift` |
| Push | `incline-push-up`, `dumbbell-bench-press`, `dumbbell-overhead-press`, `kneeling-push-up`, `push-up`, `pike-push-up`, `resistance-band-chest-press`, `resistance-band-overhead-press`, `dumbbell-lateral-raise`, `dumbbell-triceps-extension` |
| Pull | `one-arm-dumbbell-row`, `resistance-band-row`, `lat-pulldown`, `prone-w-raise`, `inverted-row`, `assisted-pull-up`, `seated-cable-row`, `dumbbell-biceps-curl`, `resistance-band-pull-apart`, `face-pull` |
| Core | `dead-bug`, `bird-dog`, `forearm-plank`, `side-plank`, `bear-hold`, `mountain-climber` |

Inventory total: 40.

## ExerciseDB Review

Checked on 2026-09-13:

- the free hosted V1 endpoint documents 1,500 exercises, no authentication,
  and one 180p GIF per exercise;
- the provider caching guide says API data may be cached only when the active
  plan explicitly permits it;
- the guide says media URLs rotate weekly and must never be stored permanently;
- the public documentation does not provide enough explicit commercial,
  attribution, durable-cache, or local-import permission for a public Keepfit
  release.

Keepfit therefore treats the integration as a labeled private prototype. A
live provider result cannot be added to the personal library. The user may
instead create an original local exercise manually.

## hasaneyldrm Dataset Review

Checked on 2026-09-14 at commit
`7455efae41b330c265e7cd4b78dfa848e7ce5ebd`:

- the repository contains 1,324 records and 1,324 unique source identifiers;
- every record has an English instruction value;
- six normalized exercise-name groups are duplicated and require review;
- the primary JSON file is approximately 16.6 MB and includes ten languages;
- 1,324 thumbnails total approximately 8.5 MB and 1,324 GIF files total
  approximately 122.8 MB;
- the repository licence grants MIT rights to code, tooling, dataset structure,
  and instruction text/translations, subject to retaining its notice; and
- the `images/` and `videos/` directories are a separate Gym visual exception.
  The repository states that cloning it does not grant another project rights
  to reuse that media.

Keepfit may therefore build a pinned, normalized offline catalogue from the
MIT-covered metadata and instruction text. The importer must generate Keepfit
UUIDs, retain the source identifier and commit provenance, validate every
record, resolve duplicates deterministically, and emit a review report. The APK
must not include the Gym visual thumbnails or GIFs without a separately
verified licence. Original or separately licensed Keepfit media is recorded per
asset in this register before bundling.

## Required Recheck

Before public release or any remote import feature:

1. obtain written commercial display and attribution requirements;
2. confirm whether the selected plan permits metadata, response, and media
   caching on an end-user device;
3. define the allowed cache lifetime around media URL rotation;
4. define provider provenance, removal, refresh, and backup behavior;
5. update the data model and migration plan if provider data becomes durable;
6. verify each bundled media asset separately before adding it to the offline
   guide.

## References

- [ExerciseDB V1 overview](https://docs.ascendapi.com/products/edb-v1/overview)
- [AscendAPI caching policy](https://docs.ascendapi.com/guides/caching)
- [ExerciseDB V1 API schema](https://docs.ascendapi.com/api-reference/exercisedb-v1/exercisedb-v1.json)
- [Coil GIF support](https://coil-kt.github.io/coil/gifs/)
- [Android Media3](https://developer.android.com/media/media3/exoplayer/hello-world)
- [hasaneyldrm exercise dataset](https://github.com/hasaneyldrm/exercises-dataset)
- [Dataset MIT and media exception](https://github.com/hasaneyldrm/exercises-dataset/blob/main/LICENSE)
- [Dataset media notice](https://github.com/hasaneyldrm/exercises-dataset/blob/main/NOTICE.md)
