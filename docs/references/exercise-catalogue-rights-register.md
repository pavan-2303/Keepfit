# Exercise Catalogue Rights and Content Register

This register records the source, storage rule, and release status of exercise
guidance displayed by Keepfit. Recheck third-party terms before refreshing a
bundled source or adding any new media.

## Source Register

| Source | Content | Current use | Persistence and caching | Public-release status |
|---|---|---|---|---|
| Keepfit legacy starter guide | Names, categories, equipment tags, and concise instructions for 40 exercises | Starter-plan compatibility only; no separate browser | Application code; no bundled media | Approved for this private repository; text was authored specifically for Keepfit |
| Keepfit movement figures v1 | Start/finish pose geometry, equipment marks, motion, form cues, and safety cues for 25 bundled exercises | Exercise details and active-workout guidance | Original Compose Canvas code and immutable registry; no external visual file | Approved as original code-native Keepfit artwork; reviewed 2026-09-15 |
| User private library | User-authored fields and explicitly imported MP4, WebM, or GIF files | Editable local exercises and private demonstrations | Room metadata plus app-private media; included only in explicit encrypted backups | User-controlled private content |
| AscendAPI ExerciseDB V1 free hosted API | Live exercise metadata, instructions, and GIF URLs | Retired v0.7.0 private prototype; no runtime code remains | Nothing is fetched, persisted, cached, or distributed | Not used in v0.14.0 |
| hasaneyldrm exercises dataset | 1,324 source records with names, categories, equipment, muscles, and multilingual instructions | 1,316 normalized English-only records bundled and seeded into Room | Pinned commit, checked-in schema/licence/notice/audit, deterministic import; no upstream media | Metadata and English instruction text approved under MIT with copyright notice; Gym visual images and GIFs excluded |

## Legacy Keepfit Starter Guide Inventory

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

## Keepfit Movement Figures v1 Inventory

Every entry below uses the same original code-native body renderer, normalized
pose geometry, and rights declaration embedded in
`CoreExerciseGuidanceCatalog`. No source image, generated bitmap, SVG, GIF, or
video is used. The verification script resolves every UUID and source ID
against the pinned v0.14 catalogue and rejects duplicates or missing rights
metadata.

| Source ID | Bundled exercise | Stable exercise UUID |
|---|---|---|
| `0025` | Barbell bench press | `5ca9f46f-1ae9-5ff8-9627-32cd73c56a13` |
| `0289` | Dumbbell bench press | `b4a97e4a-61ec-5c4d-81b7-1a043aa7a599` |
| `0662` | Push-up | `76a597e5-4c1e-5d5c-8119-a0bde7b77981` |
| `0493` | Incline push-up | `b9430a44-8fba-5b3e-871e-990ae9519cc9` |
| `0405` | Dumbbell seated shoulder press | `d33d986b-1046-52d8-89c4-b622fdc8dfc5` |
| `0334` | Dumbbell lateral raise | `75099793-4b69-527a-8cdc-d2114d298725` |
| `0027` | Barbell bent over row | `044e1e2b-38be-5e18-b318-842e0940a4fa` |
| `0861` | Cable seated row | `9eedbccb-509e-569b-ba9f-266e90021893` |
| `2330` | Cable lat pulldown full range of motion | `9949981e-fc2e-5acc-bc69-8bcd53db97ef` |
| `0652` | Pull-up | `bb222adb-b2eb-549f-9f57-9fdcd8d68a1f` |
| `0043` | Barbell full squat | `46632848-5ff2-54d4-887a-e23d56c8d195` |
| `1760` | Dumbbell goblet squat | `6109caee-8f5a-56d8-9fbc-63608a1237a7` |
| `0032` | Barbell deadlift | `902c8ae3-c298-5a9c-b5a3-3237d2e9906a` |
| `1459` | Dumbbell romanian deadlift | `c2c1d965-ed7a-5fa4-8d76-436a0cc84442` |
| `1460` | Walking lunge | `01aac1d3-12c0-526b-9a16-3978c10c2bb0` |
| `0431` | Dumbbell step-up | `c9e43998-9b82-5501-9ad3-e30d0a24a230` |
| `3013` | Low glute bridge on floor | `f635ff87-e394-5404-9f0b-d054b584cbf8` |
| `0760` | Smith leg press | `9cfb3bcf-6367-55d6-89ac-2c41150dd7ef` |
| `0417` | Dumbbell standing calf raise | `9e04873f-54e6-52b3-b729-2c1d48139a3b` |
| `0416` | Dumbbell standing biceps curl | `29314612-9bbb-5c61-b04b-7b0457ea27bd` |
| `0201` | Cable pushdown | `9041ce96-f296-5934-a769-d7688b1bad28` |
| `0430` | Dumbbell standing triceps extension | `6e579c24-9ed1-5417-a4b3-2c9700415f8a` |
| `2135` | Weighted front plank | `7c44fed5-775b-5b0a-9ee5-56fbeb2dd5b6` |
| `0276` | Dead bug | `f9c7869a-4e52-5650-a184-a457b9b0d6d7` |
| `0630` | Mountain climber | `1e544139-12c7-5bef-9079-f7c1d5f03c85` |

Inventory total: 25. Artwork family: `Keepfit movement figures v1`. Creator:
Keepfit. Rights basis: original code-native artwork. Reviewed: 2026-09-15.

## Retired ExerciseDB Review

Checked on 2026-09-13:

- the free hosted V1 endpoint documents 1,500 exercises, no authentication,
  and one 180p GIF per exercise;
- the provider caching guide says API data may be cached only when the active
  plan explicitly permits it;
- the guide says media URLs rotate weekly and must never be stored permanently;
- the public documentation does not provide enough explicit commercial,
  attribution, durable-cache, or local-import permission for a public Keepfit
  release.

The v0.7.0 private prototype followed those restrictions. v0.14.0 removes its
provider, dependency injection, network code, UI, and tests. The current app
does not contact ExerciseDB.

## hasaneyldrm Dataset Review

Re-audited and imported on 2026-09-15 at commit
`7455efae41b330c265e7cd4b78dfa848e7ce5ebd`:

- the repository contains 1,324 records and 1,324 unique source identifiers;
- every record has an English instruction value;
- executable whitespace, case, and punctuation normalization identifies eight
  duplicate-name groups; the smallest numeric source ID is retained in each;
- the deterministic accepted output contains 1,316 records;
- the repository licence grants MIT rights to code, tooling, dataset structure,
  and instruction text/translations, subject to retaining its notice; and
- the `images/` and `videos/` directories are a separate Gym visual exception.
  The repository states that cloning it does not grant another project rights
  to reuse that media.

The importer validates the source JSON against the pinned schema, generates
stable Keepfit UUIDs, retains source ID and commit provenance, keeps English
instructions and normalized metadata, resolves duplicates deterministically,
and emits an audit report. It explicitly excludes `media_id`, `image`,
`gif_url`, `attribution`, `created_at`, and non-English instructions.

Pinned source hashes (SHA-256):

| Input | SHA-256 |
|---|---|
| `exercises.json` | `fa3864922389360f91ec84b74292b559e98b58d5aea08a97f023a1182f4d679c` |
| `exercises.schema.json` | `8acd26f837d46954c11eec9a087bb6e905005aafcf8d3dba05f8aa035b15e903` |
| `LICENSE` | `18cad2f010ab9cc219ee5b11ba0d6bc05d44f3c063a0633ed9fed350aeea9050` |
| `NOTICE.md` | `784d3d47b9aacc8141372a82539f0134344079e367ff23039864bc27e5100578` |
| normalized `exercises-v1.json` | `1a5502c0298e21a70011c19ff5bbe2c9bce4760f09084eeed7ec78a61622a44d` |

The upstream copyright notice retained in the app and distributable assets is
`Copyright (c) 2026 Hasan Emir Yıldırım`. The APK must not include the Gym
visual thumbnails or GIFs without a separately verified licence. Original or
separately licensed Keepfit media is recorded per asset before bundling.

## Required Recheck

Before public release, a catalogue refresh, or any new remote import feature:

1. rerun the source audit and review any licence or notice change;
2. review every rejected row and duplicate decision;
3. update provenance and migration behavior for the new pinned revision;
4. obtain written rights before adding third-party visual media; and
5. verify each bundled media asset separately before distribution.

## References

- [ExerciseDB V1 overview](https://docs.ascendapi.com/products/edb-v1/overview)
- [AscendAPI caching policy](https://docs.ascendapi.com/guides/caching)
- [ExerciseDB V1 API schema](https://docs.ascendapi.com/api-reference/exercisedb-v1/exercisedb-v1.json)
- [Coil GIF support](https://coil-kt.github.io/coil/gifs/)
- [Android Media3](https://developer.android.com/media/media3/exoplayer/hello-world)
- [hasaneyldrm exercise dataset](https://github.com/hasaneyldrm/exercises-dataset)
- [Dataset MIT and media exception](https://github.com/hasaneyldrm/exercises-dataset/blob/main/LICENSE)
- [Dataset media notice](https://github.com/hasaneyldrm/exercises-dataset/blob/main/NOTICE.md)
