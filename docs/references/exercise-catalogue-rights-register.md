# Exercise Content and Rights Register

This register defines which exercise content may ship with Keepfit.

## Current Content Boundary

| Source | Current use | Storage | Release status |
|---|---|---|---|
| User-created exercises | Personal names, classifications, instructions, and notes | Room | Private user data |
| User-imported MP4, WebM, or GIF demonstrations | Optional personal movement reference | App-private storage | Owned or licensed by the importing user |
| Keepfit starter-plan definitions | Created only after the user accepts an offline starter plan | Application code, then ordinary personal Room rows | Original Keepfit text; no bundled media |
| Keepfit movement figures | Code-native movement geometry and safety cues for supported legacy IDs | Application code | Original Apache-2.0 source; no bitmap or video asset |

A fresh installation contains no exercise records. Keepfit does not package or
download a third-party exercise catalogue or third-party exercise media.

## Retired Sources

The `hasaneyldrm/exercises-dataset` metadata import was distributed in versions
0.14.0 through 0.22.0 under its MIT terms. Version 0.23.0 removes its checked-in
data, import code, APK notices, and untouched seeded rows. Upgrade migration
14-to-15 preserves only rows the user edited, referenced, or attached media to,
and clears their legacy provenance so they become personal records.

The earlier AscendAPI ExerciseDB prototype is also retired; no runtime request,
cache, or distributed media remains.

Historical version and done records retain the original audit decisions as an
immutable account of what those releases shipped.

## Contribution Rules

- Do not commit third-party exercise descriptions or media without an explicit
  approved product change and licence review.
- Do not commit a user's private demonstration or fitness data.
- New Keepfit-authored guidance must record its author, rights basis, and review
  date in the source registry that owns it.
- Restricted media must follow [MEDIA-LICENSE.md](../../MEDIA-LICENSE.md) and
  must not enter the public repository unless its terms explicitly permit it.
