# Voice catalog

*Auto-generated from `catalog/v1/models.json` on 2026-11-26 by `tools/catalog/build_model_list.py` (run from the weekly [catalog-refresh](../.github/workflows/catalog-refresh.yml) workflow). Do not edit by hand — the next refresh will overwrite your changes.*

## Summary

- **634 voices** across **8 model families**
- **89 languages** covered
- Bundle size: 13–635 MB (median 36 MB)
- **6 voices** support reference-audio cloning

### By family

| Family | Voices | Notes |
|---|---:|---|
| **piper** | 536 | Compact VITS-based voices from the rhasspy/piper project. 10–60 MB per voice, sub-second on a 2020+ phone, ~70 languages covered. |
| **kokoro** | 7 | Higher-quality multilingual VITS variant (Kokoro-82M). 80–360 MB per voice; English bundles ship 1–50 speakers in a single model. |
| **kitten** | 7 | Tiny English-only VITS distillations tuned for low-end phones. <60 MB, fastest synthesis on the catalog. |
| **matcha** | 5 | Diffusion-based Matcha-TTS voices. Ships a vocoder side-asset alongside the main weights — Browse handles the dual download. |
| **supertonic** | 2 | Newest (2026) multilingual model from Supertone. Single 100–200 MB bundle covering ~30 languages × 10 speakers. |
| **zipvoice** | 4 | Flow-matching voice-cloning model. Accepts a reference clip + transcript and synthesises the target text in the cloned voice. |
| **pocket** | 2 | Compact voice-cloning model. Same reference-audio API as ZipVoice but with a smaller voice-embedding cache and lighter weights. |
| **vits** | 71 | _(family blurb pending — add to `FAMILY_BLURB`)_ |

## piper (536)

Compact VITS-based voices from the rhasspy/piper project. 10–60 MB per voice, sub-second on a 2020+ phone, ~70 languages covered.

| Title | Languages | Speakers | Size | Tier | Quality | RTF | License |
|---|---|---:|---:|---|---|---:|---|
| 25hours_single | vi-VN | 1 | 67 MB | low | low | — | MIT |
| 25hours_single | vi-VN | 1 | 36 MB | low | low | — | MIT |
| 25hours_single | vi-VN | 1 | 21 MB | low | low | — | MIT |
| Aivars | lv-LV | 1 | 67 MB | mid | medium | — | MIT |
| Aivars | lv-LV | 1 | 36 MB | mid | medium | — | MIT |
| Aivars | lv-LV | 1 | 21 MB | mid | medium | — | MIT |
| Alan | en-GB | 1 | 67 MB | low | low | — | MIT |
| Alan | en-GB | 1 | 36 MB | low | low | — | MIT |
| Alan | en-GB | 1 | 21 MB | low | low | — | MIT |
| Alan | en-GB | 1 | 67 MB | mid | medium | — | MIT |
| Alan | en-GB | 1 | 36 MB | mid | medium | — | MIT |
| Alan | en-GB | 1 | 21 MB | mid | medium | — | MIT |
| Alba | en-GB | 1 | 67 MB | mid | medium | — | MIT |
| Alba | en-GB | 1 | 36 MB | mid | medium | — | MIT |
| Alba | en-GB | 1 | 21 MB | mid | medium | — | MIT |
| Ald | es-MX | 1 | 67 MB | mid | medium | — | MIT |
| Ald | es-MX | 1 | 36 MB | mid | medium | — | MIT |
| Ald | es-MX | 1 | 21 MB | mid | medium | — | MIT |
| Alex | nl-NL | 1 | 67 MB | mid | medium | — | MIT |
| Alex | nl-NL | 1 | 36 MB | mid | medium | — | MIT |
| Alex | nl-NL | 1 | 21 MB | mid | medium | — | MIT |
| Alma | sv-SE | 1 | 67 MB | mid | medium | — | MIT |
| Alma | sv-SE | 1 | 36 MB | mid | medium | — | MIT |
| Alma | sv-SE | 1 | 21 MB | mid | medium | — | MIT |
| Amir | fa-IR | 1 | 67 MB | mid | medium | — | MIT |
| Amir | fa-IR | 1 | 36 MB | mid | medium | — | MIT |
| Amir | fa-IR | 1 | 21 MB | mid | medium | — | MIT |
| Amy | en-US | 1 | 67 MB | low | low | — | MIT |
| Amy | en-US | 1 | 36 MB | low | low | — | MIT |
| Amy | en-US | 1 | 21 MB | low | low | — | MIT |
| Amy | en-US | 1 | 67 MB | mid | medium | — | MIT |
| Amy | en-US | 1 | 36 MB | mid | medium | — | MIT |
| Amy | en-US | 1 | 21 MB | mid | medium | — | MIT |
| Anna | hu-HU | 1 | 67 MB | mid | medium | — | MIT |
| Anna | hu-HU | 1 | 36 MB | mid | medium | — | MIT |
| Anna | hu-HU | 1 | 21 MB | mid | medium | — | MIT |
| Antton | eu-ES | 1 | 67 MB | mid | medium | — | MIT |
| Antton | eu-ES | 1 | 36 MB | mid | medium | — | MIT |
| Antton | eu-ES | 1 | 21 MB | mid | medium | — | MIT |
| Arjun | ml-IN | 1 | 67 MB | mid | medium | — | MIT |
| Arjun | ml-IN | 1 | 36 MB | mid | medium | — | MIT |
| Arjun | ml-IN | 1 | 21 MB | mid | medium | — | MIT |
| Artur | sl-SI | 1 | 67 MB | mid | medium | — | MIT |
| Artur | sl-SI | 1 | 36 MB | mid | medium | — | MIT |
| Artur | sl-SI | 1 | 21 MB | mid | medium | — | MIT |
| Bass | pl-PL | 1 | 116 MB | high | high | — | MIT |
| Bass | pl-PL | 1 | 59 MB | high | high | — | MIT |
| Bass | pl-PL | 1 | 35 MB | high | high | — | MIT |
| Berfin_renas | ku-TR | 1 | 80 MB | mid | medium | — | MIT |
| Berfin_renas | ku-TR | 1 | 42 MB | mid | medium | — | MIT |
| Berfin_renas | ku-TR | 1 | 24 MB | mid | medium | — | MIT |
| Berta | hu-HU | 1 | 67 MB | mid | medium | — | MIT |
| Berta | hu-HU | 1 | 36 MB | mid | medium | — | MIT |
| Berta | hu-HU | 1 | 21 MB | mid | medium | — | MIT |
| Bryce | en-US | 1 | 67 MB | mid | medium | — | MIT |
| Bryce | en-US | 1 | 36 MB | mid | medium | — | MIT |
| Bryce | en-US | 1 | 21 MB | mid | medium | — | MIT |
| Bui | is-IS | 1 | 67 MB | mid | medium | — | MIT |
| Bui | is-IS | 1 | 36 MB | mid | medium | — | MIT |
| Bui | is-IS | 1 | 21 MB | mid | medium | — | MIT |
| Cadu | pt-BR | 1 | 67 MB | mid | medium | — | MIT |
| Cadu | pt-BR | 1 | 36 MB | mid | medium | — | MIT |
| Cadu | pt-BR | 1 | 21 MB | mid | medium | — | MIT |
| Carlfm | es-ES | 1 | 27 MB | low | x_low | — | MIT |
| Carlfm | es-ES | 1 | 17 MB | low | x_low | — | MIT |
| Carlfm | es-ES | 1 | 13 MB | low | x_low | — | MIT |
| Chaowen | zh-CN | 1 | 60 MB | mid | medium | — | MIT |
| Chaowen | zh-CN | 1 | 29 MB | mid | medium | — | MIT |
| Chaowen | zh-CN | 1 | 14 MB | mid | medium | — | MIT |
| Chitwan | ne-NP | 1 | 67 MB | mid | medium | — | MIT |
| Chitwan | ne-NP | 1 | 36 MB | mid | medium | — | MIT |
| Chitwan | ne-NP | 1 | 21 MB | mid | medium | — | MIT |
| Claude | es-MX | 1 | 67 MB | high | high | — | MIT |
| Claude | es-MX | 1 | 36 MB | high | high | — | MIT |
| Claude | es-MX | 1 | 21 MB | high | high | — | MIT |
| Cori | en-GB | 1 | 116 MB | high | high | — | MIT |
| Cori | en-GB | 1 | 59 MB | high | high | — | MIT |
| Cori | en-GB | 1 | 35 MB | high | high | — | MIT |
| Cori | en-GB | 1 | 67 MB | mid | medium | — | MIT |
| Cori | en-GB | 1 | 36 MB | mid | medium | — | MIT |
| Cori | en-GB | 1 | 21 MB | mid | medium | — | MIT |
| Daniela | es-AR | 1 | 116 MB | high | high | — | MIT |
| Daniela | es-AR | 1 | 59 MB | high | high | — | MIT |
| Daniela | es-AR | 1 | 35 MB | high | high | — | MIT |
| Danny | en-US | 1 | 67 MB | low | low | — | MIT |
| Danny | en-US | 1 | 36 MB | low | low | — | MIT |
| Danny | en-US | 1 | 21 MB | low | low | — | MIT |
| Darkman | pl-PL | 1 | 67 MB | mid | medium | — | MIT |
| Darkman | pl-PL | 1 | 36 MB | mid | medium | — | MIT |
| Darkman | pl-PL | 1 | 21 MB | mid | medium | — | MIT |
| Davefx | es-ES | 1 | 67 MB | mid | medium | — | MIT |
| Davefx | es-ES | 1 | 36 MB | mid | medium | — | MIT |
| Davefx | es-ES | 1 | 21 MB | mid | medium | — | MIT |
| Denis | ru-RU | 1 | 67 MB | mid | medium | — | MIT |
| Denis | ru-RU | 1 | 36 MB | mid | medium | — | MIT |
| Denis | ru-RU | 1 | 21 MB | mid | medium | — | MIT |
| Dfki | tr-TR | 1 | 67 MB | mid | medium | — | MIT |
| Dfki | tr-TR | 1 | 36 MB | mid | medium | — | MIT |
| Dfki | tr-TR | 1 | 21 MB | mid | medium | — | MIT |
| Dii | de-DE | 1 | 67 MB | high | — | — | MIT |
| Dii | de-DE | 1 | 36 MB | high | — | — | MIT |
| Dii | de-DE | 1 | 21 MB | high | — | — | MIT |
| Dii | en-GB | 1 | 67 MB | high | — | — | MIT |
| Dii | en-GB | 1 | 36 MB | high | — | — | MIT |
| Dii | en-GB | 1 | 21 MB | high | — | — | MIT |
| Dii | it-IT | 1 | 67 MB | high | — | — | MIT |
| Dii | it-IT | 1 | 36 MB | high | — | — | MIT |
| Dii | it-IT | 1 | 21 MB | high | — | — | MIT |
| Dii | nl-NL | 1 | 67 MB | high | — | — | MIT |
| Dii | nl-NL | 1 | 36 MB | high | — | — | MIT |
| Dii | nl-NL | 1 | 21 MB | high | — | — | MIT |
| Dii | pt-BR | 1 | 67 MB | high | — | — | MIT |
| Dii | pt-BR | 1 | 36 MB | high | — | — | MIT |
| Dii | pt-BR | 1 | 21 MB | high | — | — | MIT |
| Dii | pt-PT | 1 | 67 MB | high | — | — | MIT |
| Dii | pt-PT | 1 | 36 MB | high | — | — | MIT |
| Dii | pt-PT | 1 | 21 MB | high | — | — | MIT |
| Dmitri | ru-RU | 1 | 67 MB | mid | medium | — | MIT |
| Dmitri | ru-RU | 1 | 36 MB | mid | medium | — | MIT |
| Dmitri | ru-RU | 1 | 21 MB | mid | medium | — | MIT |
| Edon | sq-AL | 1 | 67 MB | mid | medium | — | MIT |
| Edon | sq-AL | 1 | 36 MB | mid | medium | — | MIT |
| Edon | sq-AL | 1 | 21 MB | mid | medium | — | MIT |
| Edresson | pt-BR | 1 | 67 MB | low | low | — | MIT |
| Edresson | pt-BR | 1 | 36 MB | low | low | — | MIT |
| Edresson | pt-BR | 1 | 21 MB | low | low | — | MIT |
| Eva_k | de-DE | 1 | 27 MB | low | x_low | — | MIT |
| Eva_k | de-DE | 1 | 17 MB | low | x_low | — | MIT |
| Eva_k | de-DE | 1 | 13 MB | low | x_low | — | MIT |
| Faber | pt-BR | 1 | 67 MB | mid | medium | — | MIT |
| Faber | pt-BR | 1 | 36 MB | mid | medium | — | MIT |
| Faber | pt-BR | 1 | 21 MB | mid | medium | — | MIT |
| Fasih | ur-PK | 1 | 67 MB | mid | medium | — | MIT |
| Fasih | ur-PK | 1 | 36 MB | mid | medium | — | MIT |
| Fasih | ur-PK | 1 | 21 MB | mid | medium | — | MIT |
| Ganji | fa-IR | 1 | 67 MB | mid | medium | — | MIT |
| Ganji | fa-IR | 1 | 36 MB | mid | medium | — | MIT |
| Ganji | fa-IR | 1 | 21 MB | mid | medium | — | MIT |
| Ganji_adabi | fa-IR | 1 | 67 MB | mid | medium | — | MIT |
| Ganji_adabi | fa-IR | 1 | 36 MB | mid | medium | — | MIT |
| Ganji_adabi | fa-IR | 1 | 21 MB | mid | medium | — | MIT |
| Gilles | fr-FR | 1 | 67 MB | low | low | — | MIT |
| Gilles | fr-FR | 1 | 36 MB | low | low | — | MIT |
| Gilles | fr-FR | 1 | 21 MB | low | low | — | MIT |
| Glados | de-DE | 1 | 116 MB | high | — | — | MIT |
| Glados | de-DE | 1 | 58 MB | high | — | — | MIT |
| Glados | de-DE | 1 | 35 MB | high | — | — | MIT |
| Glados | de-DE | 1 | 67 MB | low | — | — | MIT |
| Glados | de-DE | 1 | 36 MB | low | — | — | MIT |
| Glados | de-DE | 1 | 21 MB | low | — | — | MIT |
| Glados | de-DE | 1 | 67 MB | mid | — | — | MIT |
| Glados | de-DE | 1 | 36 MB | mid | — | — | MIT |
| Glados | de-DE | 1 | 21 MB | mid | — | — | MIT |
| Glados | en-US | 1 | 116 MB | high | — | — | MIT |
| Glados | en-US | 1 | 58 MB | high | — | — | MIT |
| Glados | en-US | 1 | 35 MB | high | — | — | MIT |
| Glados | es-ES | 1 | 67 MB | mid | — | — | MIT |
| Glados | es-ES | 1 | 36 MB | mid | — | — | MIT |
| Glados | es-ES | 1 | 21 MB | mid | — | — | MIT |
| Glados_turret | de-DE | 1 | 116 MB | high | — | — | MIT |
| Glados_turret | de-DE | 1 | 58 MB | high | — | — | MIT |
| Glados_turret | de-DE | 1 | 35 MB | high | — | — | MIT |
| Glados_turret | de-DE | 1 | 67 MB | low | — | — | MIT |
| Glados_turret | de-DE | 1 | 36 MB | low | — | — | MIT |
| Glados_turret | de-DE | 1 | 21 MB | low | — | — | MIT |
| Glados_turret | de-DE | 1 | 67 MB | mid | — | — | MIT |
| Glados_turret | de-DE | 1 | 36 MB | mid | — | — | MIT |
| Glados_turret | de-DE | 1 | 21 MB | mid | — | — | MIT |
| Gosia | pl-PL | 1 | 67 MB | mid | medium | — | MIT |
| Gosia | pl-PL | 1 | 36 MB | mid | medium | — | MIT |
| Gosia | pl-PL | 1 | 21 MB | mid | medium | — | MIT |
| Gwryw_gogleddol | cy-GB | 1 | 67 MB | mid | medium | — | MIT |
| Gwryw_gogleddol | cy-GB | 1 | 36 MB | mid | medium | — | MIT |
| Gwryw_gogleddol | cy-GB | 1 | 21 MB | mid | medium | — | MIT |
| Gyro | fa-IR | 1 | 67 MB | mid | medium | — | MIT |
| Gyro | fa-IR | 1 | 36 MB | mid | medium | — | MIT |
| Gyro | fa-IR | 1 | 21 MB | mid | medium | — | MIT |
| Harri | fi-FI | 1 | 67 MB | low | low | — | MIT |
| Harri | fi-FI | 1 | 36 MB | low | low | — | MIT |
| Harri | fi-FI | 1 | 21 MB | low | low | — | MIT |
| Harri | fi-FI | 1 | 67 MB | mid | medium | — | MIT |
| Harri | fi-FI | 1 | 36 MB | mid | medium | — | MIT |
| Harri | fi-FI | 1 | 21 MB | mid | medium | — | MIT |
| Hfc_female | en-US | 1 | 67 MB | mid | medium | — | MIT |
| Hfc_female | en-US | 1 | 36 MB | mid | medium | — | MIT |
| Hfc_female | en-US | 1 | 21 MB | mid | medium | — | MIT |
| Hfc_male | en-US | 1 | 67 MB | mid | medium | — | MIT |
| Hfc_male | en-US | 1 | 36 MB | mid | medium | — | MIT |
| Hfc_male | en-US | 1 | 21 MB | mid | medium | — | MIT |
| Huayan | zh-CN | 1 | 67 MB | mid | medium | — | MIT |
| Imre | hu-HU | 1 | 67 MB | mid | medium | — | MIT |
| Imre | hu-HU | 1 | 36 MB | mid | medium | — | MIT |
| Imre | hu-HU | 1 | 21 MB | mid | medium | — | MIT |
| Irina | ru-RU | 1 | 67 MB | mid | medium | — | MIT |
| Irina | ru-RU | 1 | 36 MB | mid | medium | — | MIT |
| Irina | ru-RU | 1 | 21 MB | mid | medium | — | MIT |
| Iseke | kk-KZ | 1 | 27 MB | low | x_low | — | MIT |
| Iseke | kk-KZ | 1 | 16 MB | low | x_low | — | MIT |
| Iseke | kk-KZ | 1 | 13 MB | low | x_low | — | MIT |
| Jarvis_wg_glos | pl-PL | 1 | 67 MB | mid | — | — | MIT |
| Jarvis_wg_glos | pl-PL | 1 | 36 MB | mid | — | — | MIT |
| Jarvis_wg_glos | pl-PL | 1 | 21 MB | mid | — | — | MIT |
| Jeff | pt-BR | 1 | 67 MB | mid | medium | — | MIT |
| Jeff | pt-BR | 1 | 36 MB | mid | medium | — | MIT |
| Jeff | pt-BR | 1 | 21 MB | mid | medium | — | MIT |
| Jenny_dioco | en-GB | 1 | 67 MB | mid | medium | — | MIT |
| Jenny_dioco | en-GB | 1 | 36 MB | mid | medium | — | MIT |
| Jenny_dioco | en-GB | 1 | 21 MB | mid | medium | — | MIT |
| Jirka | cs-CZ | 1 | 67 MB | low | low | — | MIT |
| Jirka | cs-CZ | 1 | 36 MB | low | low | — | MIT |
| Jirka | cs-CZ | 1 | 21 MB | low | low | — | MIT |
| Jirka | cs-CZ | 1 | 67 MB | mid | medium | — | MIT |
| Jirka | cs-CZ | 1 | 36 MB | mid | medium | — | MIT |
| Jirka | cs-CZ | 1 | 21 MB | mid | medium | — | MIT |
| Joe | en-US | 1 | 67 MB | mid | medium | — | MIT |
| Joe | en-US | 1 | 36 MB | mid | medium | — | MIT |
| Joe | en-US | 1 | 21 MB | mid | medium | — | MIT |
| John | en-US | 1 | 67 MB | mid | medium | — | MIT |
| John | en-US | 1 | 36 MB | mid | medium | — | MIT |
| John | en-US | 1 | 21 MB | mid | medium | — | MIT |
| Justyna_wg_glos | pl-PL | 1 | 67 MB | mid | — | — | MIT |
| Justyna_wg_glos | pl-PL | 1 | 36 MB | mid | — | — | MIT |
| Justyna_wg_glos | pl-PL | 1 | 21 MB | mid | — | — | MIT |
| Kareem | ar-JO | 1 | 67 MB | low | low | — | MIT |
| Kareem | ar-JO | 1 | 36 MB | low | low | — | MIT |
| Kareem | ar-JO | 1 | 21 MB | low | low | — | MIT |
| Kareem | ar-JO | 1 | 67 MB | mid | medium | — | MIT |
| Kareem | ar-JO | 1 | 36 MB | mid | medium | — | MIT |
| Kareem | ar-JO | 1 | 21 MB | mid | medium | — | MIT |
| Karlsson | de-DE | 1 | 67 MB | low | low | — | MIT |
| Karlsson | de-DE | 1 | 36 MB | low | low | — | MIT |
| Karlsson | de-DE | 1 | 21 MB | low | low | — | MIT |
| Kathleen | en-US | 1 | 67 MB | low | low | — | MIT |
| Kathleen | en-US | 1 | 36 MB | low | low | — | MIT |
| Kathleen | en-US | 1 | 21 MB | low | low | — | MIT |
| Kerstin | de-DE | 1 | 67 MB | low | low | — | MIT |
| Kerstin | de-DE | 1 | 36 MB | low | low | — | MIT |
| Kerstin | de-DE | 1 | 21 MB | low | low | — | MIT |
| Kristin | en-US | 1 | 67 MB | mid | medium | — | MIT |
| Kristin | en-US | 1 | 36 MB | mid | medium | — | MIT |
| Kristin | en-US | 1 | 21 MB | mid | medium | — | MIT |
| Kusal | en-US | 1 | 67 MB | mid | medium | — | MIT |
| Kusal | en-US | 1 | 36 MB | mid | medium | — | MIT |
| Kusal | en-US | 1 | 21 MB | mid | medium | — | MIT |
| Lada | uk-UA | 1 | 27 MB | low | x_low | — | MIT |
| Lada | uk-UA | 1 | 17 MB | low | x_low | — | MIT |
| Lada | uk-UA | 1 | 13 MB | low | x_low | — | MIT |
| Lanfrica | sw-CD | 1 | 67 MB | mid | medium | — | MIT |
| Lanfrica | sw-CD | 1 | 36 MB | mid | medium | — | MIT |
| Lanfrica | sw-CD | 1 | 21 MB | mid | medium | — | MIT |
| Lessac | en-US | 1 | 116 MB | high | high | — | MIT |
| Lessac | en-US | 1 | 58 MB | high | high | — | MIT |
| Lessac | en-US | 1 | 35 MB | high | high | — | MIT |
| Lessac | en-US | 1 | 67 MB | low | low | — | MIT |
| Lessac | en-US | 1 | 36 MB | low | low | — | MIT |
| Lessac | en-US | 1 | 21 MB | low | low | — | MIT |
| Lessac | en-US | 1 | 67 MB | mid | medium | — | MIT |
| Lessac | en-US | 1 | 36 MB | mid | medium | — | MIT |
| Lessac | en-US | 1 | 21 MB | mid | medium | — | MIT |
| Lili | sk-SK | 1 | 67 MB | mid | medium | — | MIT |
| Lili | sk-SK | 1 | 36 MB | mid | medium | — | MIT |
| Lili | sk-SK | 1 | 21 MB | mid | medium | — | MIT |
| Lisa | sv-SE | 1 | 67 MB | mid | medium | — | MIT |
| Lisa | sv-SE | 1 | 36 MB | mid | medium | — | MIT |
| Lisa | sv-SE | 1 | 21 MB | mid | medium | — | MIT |
| Ljspeech | en-US | 1 | 116 MB | high | high | — | MIT |
| Ljspeech | en-US | 1 | 59 MB | high | high | — | MIT |
| Ljspeech | en-US | 1 | 34 MB | high | high | — | MIT |
| Ljspeech | en-US | 1 | 67 MB | mid | medium | — | MIT |
| Ljspeech | en-US | 1 | 36 MB | mid | medium | — | MIT |
| Ljspeech | en-US | 1 | 21 MB | mid | medium | — | MIT |
| Maider | eu-ES | 1 | 67 MB | mid | medium | — | MIT |
| Maider | eu-ES | 1 | 36 MB | mid | medium | — | MIT |
| Maider | eu-ES | 1 | 21 MB | mid | medium | — | MIT |
| Marylux | lb-LU | 1 | 67 MB | mid | medium | — | MIT |
| Marylux | lb-LU | 1 | 36 MB | mid | medium | — | MIT |
| Marylux | lb-LU | 1 | 21 MB | mid | medium | — | MIT |
| Mc_speech | pl-PL | 1 | 67 MB | mid | medium | — | MIT |
| Mc_speech | pl-PL | 1 | 36 MB | mid | medium | — | MIT |
| Mc_speech | pl-PL | 1 | 21 MB | mid | medium | — | MIT |
| Meera | ml-IN | 1 | 67 MB | mid | medium | — | MIT |
| Meera | ml-IN | 1 | 36 MB | mid | medium | — | MIT |
| Meera | ml-IN | 1 | 21 MB | mid | medium | — | MIT |
| Meski_wg_glos | pl-PL | 1 | 67 MB | mid | — | — | MIT |
| Meski_wg_glos | pl-PL | 1 | 36 MB | mid | — | — | MIT |
| Meski_wg_glos | pl-PL | 1 | 21 MB | mid | — | — | MIT |
| Mihai | ro-RO | 1 | 67 MB | mid | medium | — | MIT |
| Mihai | ro-RO | 1 | 21 MB | mid | medium | — | MIT |
| Miro | de-DE | 1 | 67 MB | high | — | — | MIT |
| Miro | de-DE | 1 | 36 MB | high | — | — | MIT |
| Miro | de-DE | 1 | 21 MB | high | — | — | MIT |
| Miro | en-GB | 1 | 67 MB | high | — | — | MIT |
| Miro | en-GB | 1 | 36 MB | high | — | — | MIT |
| Miro | en-GB | 1 | 21 MB | high | — | — | MIT |
| Miro | en-US | 1 | 67 MB | high | — | — | MIT |
| Miro | en-US | 1 | 36 MB | high | — | — | MIT |
| Miro | en-US | 1 | 21 MB | high | — | — | MIT |
| Miro | es-ES | 1 | 67 MB | high | — | — | MIT |
| Miro | es-ES | 1 | 36 MB | high | — | — | MIT |
| Miro | es-ES | 1 | 21 MB | high | — | — | MIT |
| Miro | fr-FR | 1 | 67 MB | high | — | — | MIT |
| Miro | fr-FR | 1 | 36 MB | high | — | — | MIT |
| Miro | fr-FR | 1 | 21 MB | high | — | — | MIT |
| Miro | it-IT | 1 | 67 MB | high | — | — | MIT |
| Miro | it-IT | 1 | 36 MB | high | — | — | MIT |
| Miro | it-IT | 1 | 21 MB | high | — | — | MIT |
| Miro | nl-NL | 1 | 67 MB | high | — | — | MIT |
| Miro | nl-NL | 1 | 36 MB | high | — | — | MIT |
| Miro | nl-NL | 1 | 21 MB | high | — | — | MIT |
| Miro | pt-BR | 1 | 67 MB | high | — | — | MIT |
| Miro | pt-BR | 1 | 36 MB | high | — | — | MIT |
| Miro | pt-BR | 1 | 21 MB | high | — | — | MIT |
| Miro | pt-PT | 1 | 67 MB | high | — | — | MIT |
| Miro | pt-PT | 1 | 36 MB | high | — | — | MIT |
| Miro | pt-PT | 1 | 21 MB | high | — | — | MIT |
| Nathalie | nl-BE | 1 | 67 MB | mid | medium | — | MIT |
| Nathalie | nl-BE | 1 | 36 MB | mid | medium | — | MIT |
| Nathalie | nl-BE | 1 | 21 MB | mid | medium | — | MIT |
| Nathalie | nl-BE | 1 | 27 MB | low | x_low | — | MIT |
| Nathalie | nl-BE | 1 | 16 MB | low | x_low | — | MIT |
| Nathalie | nl-BE | 1 | 13 MB | low | x_low | — | MIT |
| Natia | ka-GE | 1 | 67 MB | mid | medium | — | MIT |
| Natia | ka-GE | 1 | 36 MB | mid | medium | — | MIT |
| Natia | ka-GE | 1 | 21 MB | mid | medium | — | MIT |
| News_tts | id-ID | 1 | 67 MB | mid | medium | — | MIT |
| News_tts | id-ID | 1 | 36 MB | mid | medium | — | MIT |
| News_tts | id-ID | 1 | 21 MB | mid | medium | — | MIT |
| Norman | en-US | 1 | 67 MB | mid | medium | — | MIT |
| Norman | en-US | 1 | 36 MB | mid | medium | — | MIT |
| Norman | en-US | 1 | 21 MB | mid | medium | — | MIT |
| Northern_english_male | en-GB | 1 | 67 MB | mid | medium | — | MIT |
| Northern_english_male | en-GB | 1 | 36 MB | mid | medium | — | MIT |
| Northern_english_male | en-GB | 1 | 21 MB | mid | medium | — | MIT |
| Nst | sv-SE | 1 | 67 MB | mid | medium | — | MIT |
| Nst | sv-SE | 1 | 36 MB | mid | medium | — | MIT |
| Nst | sv-SE | 1 | 21 MB | mid | medium | — | MIT |
| Paola | it-IT | 1 | 67 MB | mid | medium | — | MIT |
| Paola | it-IT | 1 | 36 MB | mid | medium | — | MIT |
| Paola | it-IT | 1 | 21 MB | mid | medium | — | MIT |
| Pavoque | de-DE | 1 | 67 MB | low | low | — | MIT |
| Pavoque | de-DE | 1 | 36 MB | low | low | — | MIT |
| Pavoque | de-DE | 1 | 21 MB | low | low | — | MIT |
| Pim | nl-NL | 1 | 67 MB | mid | medium | — | MIT |
| Pim | nl-NL | 1 | 36 MB | mid | medium | — | MIT |
| Pim | nl-NL | 1 | 21 MB | mid | medium | — | MIT |
| Piper (109 speakers) | en-GB | 109 | 80 MB | mid | medium | — | MIT |
| Piper (109 speakers) | en-GB | 109 | 42 MB | mid | medium | — | MIT |
| Piper (109 speakers) | en-GB | 109 | 23 MB | mid | medium | — | MIT |
| Piper (12 speakers) | en-GB | 12 | 80 MB | mid | medium | — | MIT |
| Piper (12 speakers) | en-GB | 12 | 42 MB | mid | medium | — | MIT |
| Piper (12 speakers) | en-GB | 12 | 23 MB | mid | medium | — | MIT |
| Piper (18 speakers) | en-US | 18 | 80 MB | mid | medium | — | MIT |
| Piper (18 speakers) | en-US | 18 | 42 MB | mid | medium | — | MIT |
| Piper (18 speakers) | en-US | 18 | 23 MB | mid | medium | — | MIT |
| Piper (18 speakers) | ne-NP | 18 | 80 MB | mid | medium | — | MIT |
| Piper (18 speakers) | ne-NP | 18 | 42 MB | mid | medium | — | MIT |
| Piper (18 speakers) | ne-NP | 18 | 24 MB | mid | medium | — | MIT |
| Piper (18 speakers) | ne-NP | 18 | 33 MB | low | x_low | — | MIT |
| Piper (18 speakers) | ne-NP | 18 | 20 MB | low | x_low | — | MIT |
| Piper (18 speakers) | ne-NP | 18 | 15 MB | low | x_low | — | MIT |
| Piper (2 speakers) | es-ES | 2 | 80 MB | mid | medium | — | MIT |
| Piper (2 speakers) | es-ES | 2 | 42 MB | mid | medium | — | MIT |
| Piper (2 speakers) | es-ES | 2 | 23 MB | mid | medium | — | MIT |
| Piper (2 speakers) | fr-FR | 2 | 80 MB | mid | medium | — | MIT |
| Piper (2 speakers) | fr-FR | 2 | 42 MB | mid | medium | — | MIT |
| Piper (2 speakers) | fr-FR | 2 | 23 MB | mid | medium | — | MIT |
| Piper (2 speakers) | sr-RS | 2 | 80 MB | mid | medium | — | MIT |
| Piper (2 speakers) | sr-RS | 2 | 42 MB | mid | medium | — | MIT |
| Piper (2 speakers) | sr-RS | 2 | 23 MB | mid | medium | — | MIT |
| Piper (24 speakers) | en-US | 24 | 80 MB | mid | medium | — | MIT |
| Piper (24 speakers) | en-US | 24 | 42 MB | mid | medium | — | MIT |
| Piper (24 speakers) | en-US | 24 | 23 MB | mid | medium | — | MIT |
| Piper (3 speakers) | uk-UA | 3 | 80 MB | mid | medium | — | MIT |
| Piper (3 speakers) | uk-UA | 3 | 42 MB | mid | medium | — | MIT |
| Piper (3 speakers) | uk-UA | 3 | 23 MB | mid | medium | — | MIT |
| Piper (4 speakers) | en-GB | 4 | 80 MB | mid | medium | — | MIT |
| Piper (4 speakers) | en-GB | 4 | 42 MB | mid | medium | — | MIT |
| Piper (4 speakers) | en-GB | 4 | 23 MB | mid | medium | — | MIT |
| Piper (6 speakers) | en-GB | 6 | 80 MB | mid | — | — | MIT |
| Piper (6 speakers) | en-GB | 6 | 42 MB | mid | — | — | MIT |
| Piper (6 speakers) | en-GB | 6 | 24 MB | mid | — | — | MIT |
| Piper (6 speakers) | kk-KZ | 6 | 129 MB | high | high | — | MIT |
| Piper (6 speakers) | kk-KZ | 6 | 65 MB | high | high | — | MIT |
| Piper (6 speakers) | kk-KZ | 6 | 36 MB | high | high | — | MIT |
| Piper (65 speakers) | vi-VN | 65 | 33 MB | low | x_low | — | MIT |
| Piper (65 speakers) | vi-VN | 65 | 20 MB | low | x_low | — | MIT |
| Piper (65 speakers) | vi-VN | 65 | 15 MB | low | x_low | — | MIT |
| Piper (7 speakers) | cy-GB | 7 | 80 MB | mid | medium | — | MIT |
| Piper (7 speakers) | cy-GB | 7 | 42 MB | mid | medium | — | MIT |
| Piper (7 speakers) | cy-GB | 7 | 23 MB | mid | medium | — | MIT |
| Piper (8 speakers) | de-DE | 8 | 80 MB | mid | medium | — | MIT |
| Piper (8 speakers) | de-DE | 8 | 42 MB | mid | medium | — | MIT |
| Piper (8 speakers) | de-DE | 8 | 23 MB | mid | medium | — | MIT |
| Piper (8 speakers) | en-GB | 8 | 80 MB | mid | — | — | MIT |
| Piper (8 speakers) | en-GB | 8 | 42 MB | mid | — | — | MIT |
| Piper (8 speakers) | en-GB | 8 | 24 MB | mid | — | — | MIT |
| Piper (904 speakers) | en-US | 904 | 131 MB | high | high | — | MIT |
| Piper (904 speakers) | en-US | 904 | 66 MB | high | high | — | MIT |
| Piper (904 speakers) | en-US | 904 | 36 MB | high | high | — | MIT |
| Piper (904 speakers) | en-US | 904 | 82 MB | mid | medium | — | MIT |
| Piper (904 speakers) | en-US | 904 | 43 MB | mid | medium | — | MIT |
| Piper (904 speakers) | en-US | 904 | 23 MB | mid | medium | — | MIT |
| Pratham | hi-IN | 1 | 67 MB | mid | medium | — | MIT |
| Pratham | hi-IN | 1 | 36 MB | mid | medium | — | MIT |
| Pratham | hi-IN | 1 | 21 MB | mid | medium | — | MIT |
| Priyamvada | hi-IN | 1 | 67 MB | mid | medium | — | MIT |
| Priyamvada | hi-IN | 1 | 36 MB | mid | medium | — | MIT |
| Priyamvada | hi-IN | 1 | 21 MB | mid | medium | — | MIT |
| Ramona | de-DE | 1 | 67 MB | low | low | — | MIT |
| Ramona | de-DE | 1 | 36 MB | low | low | — | MIT |
| Ramona | de-DE | 1 | 21 MB | low | low | — | MIT |
| Rapunzelina | el-GR | 1 | 67 MB | low | low | — | MIT |
| Rapunzelina | el-GR | 1 | 36 MB | low | low | — | MIT |
| Rapunzelina | el-GR | 1 | 21 MB | low | low | — | MIT |
| Raya | kk-KZ | 1 | 26 MB | low | x_low | — | MIT |
| Raya | kk-KZ | 1 | 16 MB | low | x_low | — | MIT |
| Raya | kk-KZ | 1 | 13 MB | low | x_low | — | MIT |
| Rdh | nl-BE | 1 | 67 MB | mid | medium | — | MIT |
| Rdh | nl-BE | 1 | 26 MB | low | x_low | — | MIT |
| Reza_ibrahim | en-US | 1 | 67 MB | mid | medium | — | MIT |
| Reza_ibrahim | en-US | 1 | 36 MB | mid | medium | — | MIT |
| Reza_ibrahim | en-US | 1 | 21 MB | mid | medium | — | MIT |
| Reza_ibrahim | fa-IR | 1 | 67 MB | mid | medium | — | MIT |
| Reza_ibrahim | fa-IR | 1 | 36 MB | mid | medium | — | MIT |
| Reza_ibrahim | fa-IR | 1 | 21 MB | mid | medium | — | MIT |
| Riccardo | it-IT | 1 | 26 MB | low | x_low | — | MIT |
| Riccardo | it-IT | 1 | 16 MB | low | x_low | — | MIT |
| Riccardo | it-IT | 1 | 13 MB | low | x_low | — | MIT |
| Rohan | hi-IN | 1 | 67 MB | mid | medium | — | MIT |
| Rohan | hi-IN | 1 | 36 MB | mid | medium | — | MIT |
| Rohan | hi-IN | 1 | 21 MB | mid | medium | — | MIT |
| Ronnie | nl-NL | 1 | 67 MB | mid | medium | — | MIT |
| Ronnie | nl-NL | 1 | 36 MB | mid | medium | — | MIT |
| Ronnie | nl-NL | 1 | 21 MB | mid | medium | — | MIT |
| Ruslan | ru-RU | 1 | 67 MB | mid | medium | — | MIT |
| Ruslan | ru-RU | 1 | 36 MB | mid | medium | — | MIT |
| Ruslan | ru-RU | 1 | 21 MB | mid | medium | — | MIT |
| Ryan | en-US | 1 | 116 MB | high | high | — | MIT |
| Ryan | en-US | 1 | 59 MB | high | high | — | MIT |
| Ryan | en-US | 1 | 34 MB | high | high | — | MIT |
| Ryan | en-US | 1 | 67 MB | low | low | — | MIT |
| Ryan | en-US | 1 | 36 MB | low | low | — | MIT |
| Ryan | en-US | 1 | 21 MB | low | low | — | MIT |
| Ryan | en-US | 1 | 67 MB | mid | medium | — | MIT |
| Ryan | en-US | 1 | 36 MB | mid | medium | — | MIT |
| Ryan | en-US | 1 | 21 MB | mid | medium | — | MIT |
| SA_dii | ar-JO | 1 | 67 MB | high | — | — | MIT |
| SA_dii | ar-JO | 1 | 36 MB | high | — | — | MIT |
| SA_dii | ar-JO | 1 | 22 MB | high | — | — | MIT |
| SA_miro | ar-JO | 1 | 67 MB | high | — | — | MIT |
| SA_miro | ar-JO | 1 | 36 MB | high | — | — | MIT |
| SA_miro | ar-JO | 1 | 22 MB | high | — | — | MIT |
| SA_miro_V2 | ar-JO | 1 | 67 MB | high | — | — | MIT |
| SA_miro_V2 | ar-JO | 1 | 36 MB | high | — | — | MIT |
| SA_miro_V2 | ar-JO | 1 | 22 MB | high | — | — | MIT |
| Salka | is-IS | 1 | 67 MB | mid | medium | — | MIT |
| Salka | is-IS | 1 | 36 MB | mid | medium | — | MIT |
| Salka | is-IS | 1 | 20 MB | mid | medium | — | MIT |
| Sam | en-US | 1 | 67 MB | mid | medium | — | MIT |
| Sam | en-US | 1 | 36 MB | mid | medium | — | MIT |
| Sam | en-US | 1 | 21 MB | mid | medium | — | MIT |
| Siwis | fr-FR | 1 | 27 MB | low | low | — | MIT |
| Siwis | fr-FR | 1 | 17 MB | low | low | — | MIT |
| Siwis | fr-FR | 1 | 13 MB | low | low | — | MIT |
| Siwis | fr-FR | 1 | 67 MB | mid | medium | — | MIT |
| Siwis | fr-FR | 1 | 36 MB | mid | medium | — | MIT |
| Siwis | fr-FR | 1 | 21 MB | mid | medium | — | MIT |
| Southern_english_female | en-GB | 1 | 67 MB | low | low | — | MIT |
| Southern_english_female | en-GB | 1 | 36 MB | low | low | — | MIT |
| Southern_english_female | en-GB | 1 | 21 MB | low | low | — | MIT |
| Speaker_0 | en-GB | 1 | 80 MB | mid | — | — | MIT |
| Speaker_0 | en-GB | 1 | 116 MB | mid | — | — | MIT |
| Speaker_0 | en-US | 1 | 67 MB | mid | — | — | MIT |
| Speaker_0 | es | 1 | 67 MB | mid | — | — | MIT |
| Speaker_0 | fa | 1 | 67 MB | mid | — | — | MIT |
| Speaker_0 | fa, en | 1 | 67 MB | mid | — | — | MIT |
| Speaker_0 | tr-TR | 1 | 67 MB | mid | — | — | MIT |
| Speaker_0 | tr-TR | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | tr-TR | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | tr-TR | 1 | 67 MB | mid | — | — | MIT |
| Speaker_0 | tr-TR | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | tr-TR | 1 | 21 MB | mid | — | — | MIT |
| Steinn | is-IS | 1 | 67 MB | mid | medium | — | MIT |
| Steinn | is-IS | 1 | 36 MB | mid | medium | — | MIT |
| Steinn | is-IS | 1 | 20 MB | mid | medium | — | MIT |
| Talesyntese | da-DK | 1 | 67 MB | mid | medium | — | MIT |
| Talesyntese | da-DK | 1 | 36 MB | mid | medium | — | MIT |
| Talesyntese | da-DK | 1 | 21 MB | mid | medium | — | MIT |
| Talesyntese | no-NO | 1 | 67 MB | mid | medium | — | MIT |
| Talesyntese | no-NO | 1 | 36 MB | mid | medium | — | MIT |
| Talesyntese | no-NO | 1 | 21 MB | mid | medium | — | MIT |
| Thorsten | de-DE | 1 | 116 MB | high | high | — | MIT |
| Thorsten | de-DE | 1 | 59 MB | high | high | — | MIT |
| Thorsten | de-DE | 1 | 35 MB | high | high | — | MIT |
| Thorsten | de-DE | 1 | 67 MB | low | low | — | MIT |
| Thorsten | de-DE | 1 | 36 MB | low | low | — | MIT |
| Thorsten | de-DE | 1 | 21 MB | low | low | — | MIT |
| Thorsten | de-DE | 1 | 67 MB | mid | medium | — | MIT |
| Thorsten | de-DE | 1 | 36 MB | mid | medium | — | MIT |
| Thorsten | de-DE | 1 | 21 MB | mid | medium | — | MIT |
| Tjiho | fr-FR | 1 | 67 MB | mid | — | — | MIT |
| Tjiho | fr-FR | 1 | 36 MB | mid | — | — | MIT |
| Tjiho | fr-FR | 1 | 21 MB | mid | — | — | MIT |
| Tjiho | fr-FR | 1 | 67 MB | mid | — | — | MIT |
| Tjiho | fr-FR | 1 | 36 MB | mid | — | — | MIT |
| Tjiho | fr-FR | 1 | 21 MB | mid | — | — | MIT |
| Tjiho | fr-FR | 1 | 67 MB | mid | — | — | MIT |
| Tjiho | fr-FR | 1 | 36 MB | mid | — | — | MIT |
| Tjiho | fr-FR | 1 | 21 MB | mid | — | — | MIT |
| Tom | fr-FR | 1 | 67 MB | mid | medium | — | MIT |
| Tom | fr-FR | 1 | 36 MB | mid | medium | — | MIT |
| Tom | fr-FR | 1 | 21 MB | mid | medium | — | MIT |
| Tugao | pt-PT | 1 | 67 MB | mid | — | — | MIT |
| Tugao | pt-PT | 1 | 36 MB | mid | — | — | MIT |
| Tugao | pt-PT | 1 | 21 MB | mid | — | — | MIT |
| Ugla | is-IS | 1 | 67 MB | mid | medium | — | MIT |
| Ugla | is-IS | 1 | 36 MB | mid | medium | — | MIT |
| Ugla | is-IS | 1 | 20 MB | mid | medium | — | MIT |
| Upc_ona | ca-ES | 1 | 67 MB | mid | medium | — | MIT |
| Upc_ona | ca-ES | 1 | 36 MB | mid | medium | — | MIT |
| Upc_ona | ca-ES | 1 | 21 MB | mid | medium | — | MIT |
| Upc_ona | ca-ES | 1 | 27 MB | low | x_low | — | MIT |
| Upc_ona | ca-ES | 1 | 17 MB | low | x_low | — | MIT |
| Upc_ona | ca-ES | 1 | 13 MB | low | x_low | — | MIT |
| Upc_pau | ca-ES | 1 | 27 MB | low | x_low | — | MIT |
| Upc_pau | ca-ES | 1 | 17 MB | low | x_low | — | MIT |
| Upc_pau | ca-ES | 1 | 13 MB | low | x_low | — | MIT |
| Vais1000 | vi-VN | 1 | 67 MB | mid | medium | — | MIT |
| Vais1000 | vi-VN | 1 | 36 MB | mid | medium | — | MIT |
| Vais1000 | vi-VN | 1 | 22 MB | mid | medium | — | MIT |
| Xiao_ya | zh-CN | 1 | 60 MB | mid | medium | — | MIT |
| Xiao_ya | zh-CN | 1 | 29 MB | mid | medium | — | MIT |
| Xiao_ya | zh-CN | 1 | 14 MB | mid | medium | — | MIT |
| Zenski_wg_glos | pl-PL | 1 | 67 MB | mid | — | — | MIT |
| Zenski_wg_glos | pl-PL | 1 | 36 MB | mid | — | — | MIT |
| Zenski_wg_glos | pl-PL | 1 | 21 MB | mid | — | — | MIT |

## kokoro (7)

Higher-quality multilingual VITS variant (Kokoro-82M). 80–360 MB per voice; English bundles ship 1–50 speakers in a single model.

| Title | Languages | Speakers | Size | Tier | Quality | RTF | License |
|---|---|---:|---:|---|---|---:|---|
| Af_msa | ar | 1 | 83 MB | high | — | — | Apache-2.0 |
| Kokoro (103 speakers) | zh-CN, en-US | 103 | 147 MB | high | — | — | Apache-2.0 |
| Kokoro (103 speakers) | zh-CN, en-US | 103 | 365 MB | high | — | — | Apache-2.0 |
| Kokoro (11 speakers) | en-US | 11 | 320 MB | high | — | — | Apache-2.0 |
| Kokoro (11 speakers) | en-US | 11 | 103 MB | high | — | — | Apache-2.0 |
| Kokoro (54 speakers) | zh-CN, en-US | 54 | 132 MB | high | — | — | Apache-2.0 |
| Kokoro (54 speakers) | zh-CN, en-US | 54 | 350 MB | high | — | — | Apache-2.0 |

## kitten (7)

Tiny English-only VITS distillations tuned for low-end phones. <60 MB, fastest synthesis on the catalog.

| Title | Languages | Speakers | Size | Tier | Quality | RTF | License |
|---|---|---:|---:|---|---|---:|---|
| Kitten (8 speakers) | en-US | 8 | 44 MB | low | — | — | Apache-2.0 |
| Kitten (8 speakers) | en-US | 8 | 157 MB | low | — | — | Apache-2.0 |
| Kitten (8 speakers) | en-US | 8 | 68 MB | low | — | — | Apache-2.0 |
| Kitten (8 speakers) | en-US | 8 | 27 MB | low | — | — | Apache-2.0 |
| Kitten (8 speakers) | en-US | 8 | 27 MB | low | — | — | Apache-2.0 |
| Kitten (8 speakers) | en-US | 8 | 64 MB | low | — | — | Apache-2.0 |
| Kitten (8 speakers) | en-US | 8 | 31 MB | low | — | — | Apache-2.0 |

## matcha (5)

Diffusion-based Matcha-TTS voices. Ships a vocoder side-asset alongside the main weights — Browse handles the dual download.

| Title | Languages | Speakers | Size | Tier | Quality | RTF | License |
|---|---|---:|---:|---|---|---:|---|
| Baker | zh-CN | 1 | 75 MB | high | — | — | MIT |
| En | zh-CN, en-US | 1 | 79 MB | high | — | — | MIT |
| Ljspeech | en-US | 1 | 77 MB | high | — | — | MIT |
| Speaker_0 | fa, en | 1 | 77 MB | high | — | — | MIT |
| Speaker_0 | fa, en | 1 | 77 MB | high | — | — | MIT |

## supertonic (2)

Newest (2026) multilingual model from Supertone. Single 100–200 MB bundle covering ~30 languages × 10 speakers.

| Title | Languages | Speakers | Size | Tier | Quality | RTF | License |
|---|---|---:|---:|---|---|---:|---|
| Speaker_0 | en | 1 | 85 MB | high | — | — | openrail |
| Supertonic (10 speakers) | 31 languages | 10 | 129 MB | high | — | — | openrail |

## zipvoice (4)

Flow-matching voice-cloning model. Accepts a reference clip + transcript and synthesises the target text in the cloned voice.

| Title | Languages | Speakers | Size | Tier | Quality | RTF | License |
|---|---|---:|---:|---|---|---:|---|
| Speaker_0 · 🎤 cloning | zh, en | 1 | 478 MB | mid | — | — | Apache-2.0 |
| Speaker_0 · 🎤 cloning | zh, en | 1 | 109 MB | mid | — | — | Apache-2.0 |
| Speaker_0 · 🎤 cloning | zh, en | 1 | 635 MB | mid | — | — | Apache-2.0 |
| Speaker_0 · 🎤 cloning | zh, en | 1 | 635 MB | mid | — | — | Apache-2.0 |

## pocket (2)

Compact voice-cloning model. Same reference-audio API as ZipVoice but with a smaller voice-embedding cache and lighter weights.

| Title | Languages | Speakers | Size | Tier | Quality | RTF | License |
|---|---|---:|---:|---|---|---:|---|
| Speaker_0 · 🎤 cloning | en | 1 | 168 MB | mid | — | — | Apache-2.0 |
| Speaker_0 · 🎤 cloning | en | 1 | 98 MB | mid | — | — | Apache-2.0 |

## vits (71)

| Title | Languages | Speakers | Size | Tier | Quality | RTF | License |
|---|---|---:|---:|---|---|---:|---|
| Speaker_0 | yue | 1 | 108 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | bg | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | bn | 1 | 108 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | cs | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | da | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | de | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | en | 1 | 115 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | en | 1 | 115 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | en | 1 | 122 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | es | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | et | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | fi | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | fr | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | ga | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | hr | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | lt | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | lv | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | mt | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | nl | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | pl | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | pt | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | ro | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | sk | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | sl | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | sv | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | uk | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | en-US | 1 | 32 MB | low | — | — | Apache-2.0 |
| Speaker_0 | en-US | 1 | 74 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | zh | 1 | 32 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | en | 1 | 109 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | en | 1 | 163 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | zh, en | 1 | 167 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | af-ZA | 1 | 80 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | bn | 1 | 80 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | el-GR | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | es-ES | 1 | 80 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | fa | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | fi-FI | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | gu-IN | 1 | 80 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | hu-HU | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | ko-KO | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | ne-NP | 1 | 80 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | pl-PL | 1 | 80 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | tn-ZA | 1 | 80 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | vi-VN | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | de-DE | 1 | 108 MB | low | — | — | Apache-2.0 |
| Speaker_0 | en | 1 | 108 MB | low | — | — | Apache-2.0 |
| Speaker_0 | fr-FR | 1 | 108 MB | low | — | — | Apache-2.0 |
| Speaker_0 | nan | 1 | 108 MB | low | — | — | Apache-2.0 |
| Speaker_0 | ru | 1 | 108 MB | low | — | — | Apache-2.0 |
| Speaker_0 | es-ES | 1 | 108 MB | low | — | — | Apache-2.0 |
| Speaker_0 | th | 1 | 108 MB | low | — | — | Apache-2.0 |
| Speaker_0 | uk | 1 | 108 MB | low | — | — | Apache-2.0 |
| Speaker_0 | en | 1 | 152 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | zh | 1 | 147 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | zh | 1 | 121 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | zh | 1 | 121 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | zh | 1 | 121 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | zh | 1 | 121 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | zh | 1 | 121 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | zh | 1 | 119 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | zh | 1 | 119 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | zh | 1 | 119 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | zh | 1 | 119 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | zh | 1 | 119 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | zh | 1 | 121 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | zh | 1 | 121 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | zh | 1 | 121 MB | mid | — | — | Apache-2.0 |
| V2 | en-US | 1 | 43 MB | mid | — | — | Apache-2.0 |
| V2 | en-US | 1 | 22 MB | mid | — | — | Apache-2.0 |
| VITS (5 speakers) | zh-CN | 5 | 119 MB | mid | — | — | Apache-2.0 |

---

_Source of truth: [`catalog/v1/models.json`](../catalog/v1/models.json). Each voice entry includes the bundle URL, sha256, sample rate, and per-(speaker, language) audition URLs in the JSON; this page is just the human-readable index._
