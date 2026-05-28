# Voice catalog

*Auto-generated from `catalog/v1/models.json` on 2026-05-28 by `tools/catalog/build_model_list.py` (run from the weekly [catalog-refresh](../.github/workflows/catalog-refresh.yml) workflow). Do not edit by hand — the next refresh will overwrite your changes.*

## Summary

- **630 voices** across **8 model families**
- **136 languages** covered
- Bundle size: 13–635 MB (median 36 MB)
- **6 voices** support reference-audio cloning

### By family

| Family | Voices | Notes |
|---|---:|---|
| **piper** | 536 | Compact VITS-based voices from the rhasspy/piper project. 10–60 MB per voice, sub-second on a 2020+ phone, ~70 languages covered. |
| **kokoro** | 6 | Higher-quality multilingual VITS variant (Kokoro-82M). 80–360 MB per voice; English bundles ship 1–50 speakers in a single model. |
| **kitten** | 7 | Tiny English-only VITS distillations tuned for low-end phones. <60 MB, fastest synthesis on the catalog. |
| **matcha** | 5 | Diffusion-based Matcha-TTS voices. Ships a vocoder side-asset alongside the main weights — Browse handles the dual download. |
| **supertonic** | 2 | Newest (2026) multilingual model from Supertone. Single 100–200 MB bundle covering ~30 languages × 10 speakers. |
| **zipvoice** | 4 | Flow-matching voice-cloning model. Accepts a reference clip + transcript and synthesises the target text in the cloned voice. |
| **pocket** | 2 | Compact voice-cloning model. Same reference-audio API as ZipVoice but with a smaller voice-embedding cache and lighter weights. |
| **vits** | 68 | _(family blurb pending — add to `FAMILY_BLURB`)_ |

## piper (536)

Compact VITS-based voices from the rhasspy/piper project. 10–60 MB per voice, sub-second on a 2020+ phone, ~70 languages covered.

| Title | Languages | Speakers | Size | Tier | Quality | RTF | License |
|---|---|---:|---:|---|---|---:|---|
| 25hours_single | vi-VN | 1 | 67 MB | low | low | — | MIT |
| 25hours_single | vi, vn | 1 | 36 MB | mid | low | — | MIT |
| 25hours_single | vi, vn | 1 | 21 MB | mid | low | — | MIT |
| Aivars | lv-LV | 1 | 67 MB | mid | medium | — | MIT |
| Aivars | lv, lv | 1 | 36 MB | mid | medium | — | MIT |
| Aivars | lv, lv | 1 | 21 MB | mid | medium | — | MIT |
| Alan | en-GB | 1 | 67 MB | low | low | — | MIT |
| Alan | en, gb | 1 | 36 MB | mid | low | — | MIT |
| Alan | en, gb | 1 | 21 MB | mid | low | — | MIT |
| Alan | en-GB | 1 | 67 MB | mid | medium | — | MIT |
| Alan | en, gb | 1 | 36 MB | mid | medium | — | MIT |
| Alan | en, gb | 1 | 21 MB | mid | medium | — | MIT |
| Alba | en-GB | 1 | 67 MB | mid | medium | — | MIT |
| Alba | en, gb | 1 | 36 MB | mid | medium | — | MIT |
| Alba | en, gb | 1 | 21 MB | mid | medium | — | MIT |
| Ald | es-MX | 1 | 67 MB | mid | medium | — | MIT |
| Ald | es, mx | 1 | 36 MB | mid | medium | — | MIT |
| Ald | es, mx | 1 | 21 MB | mid | medium | — | MIT |
| Alex | nl-NL | 1 | 67 MB | mid | medium | — | MIT |
| Alex | nl, nl | 1 | 36 MB | mid | medium | — | MIT |
| Alex | nl, nl | 1 | 21 MB | mid | medium | — | MIT |
| Alma | sv-SE | 1 | 67 MB | mid | medium | — | MIT |
| Alma | sv, se | 1 | 36 MB | mid | medium | — | MIT |
| Alma | sv, se | 1 | 21 MB | mid | medium | — | MIT |
| Amir | fa-IR | 1 | 67 MB | mid | medium | — | MIT |
| Amir | fa, ir | 1 | 36 MB | mid | medium | — | MIT |
| Amir | fa, ir | 1 | 21 MB | mid | medium | — | MIT |
| Amy | en-US | 1 | 67 MB | low | low | — | MIT |
| Amy | en, us | 1 | 36 MB | mid | low | — | MIT |
| Amy | en, us | 1 | 21 MB | mid | low | — | MIT |
| Amy | en-US | 1 | 67 MB | mid | medium | — | MIT |
| Amy | en, us | 1 | 36 MB | mid | medium | — | MIT |
| Amy | en, us | 1 | 21 MB | mid | medium | — | MIT |
| Anna | hu-HU | 1 | 67 MB | mid | medium | — | MIT |
| Anna | hu, hu | 1 | 36 MB | mid | medium | — | MIT |
| Anna | hu, hu | 1 | 21 MB | mid | medium | — | MIT |
| Antton | eu-ES | 1 | 67 MB | mid | medium | — | MIT |
| Antton | eu, es | 1 | 36 MB | mid | medium | — | MIT |
| Antton | eu, es | 1 | 21 MB | mid | medium | — | MIT |
| Arjun | ml-IN | 1 | 67 MB | mid | medium | — | MIT |
| Arjun | ml, in | 1 | 36 MB | mid | medium | — | MIT |
| Arjun | ml, in | 1 | 21 MB | mid | medium | — | MIT |
| Artur | sl-SI | 1 | 67 MB | mid | medium | — | MIT |
| Artur | sl, si | 1 | 36 MB | mid | medium | — | MIT |
| Artur | sl, si | 1 | 21 MB | mid | medium | — | MIT |
| Bass | pl-PL | 1 | 116 MB | high | high | — | MIT |
| Bass | pl, pl | 1 | 59 MB | mid | high | — | MIT |
| Bass | pl, pl | 1 | 35 MB | mid | high | — | MIT |
| Berfin_renas | ku-TR | 1 | 80 MB | mid | medium | — | MIT |
| Berta | hu-HU | 1 | 67 MB | mid | medium | — | MIT |
| Berta | hu, hu | 1 | 36 MB | mid | medium | — | MIT |
| Berta | hu, hu | 1 | 21 MB | mid | medium | — | MIT |
| Bryce | en-US | 1 | 67 MB | mid | medium | — | MIT |
| Bryce | en, us | 1 | 36 MB | mid | medium | — | MIT |
| Bryce | en, us | 1 | 21 MB | mid | medium | — | MIT |
| Bui | is-IS | 1 | 67 MB | mid | medium | — | MIT |
| Bui | is, is | 1 | 36 MB | mid | medium | — | MIT |
| Bui | is, is | 1 | 21 MB | mid | medium | — | MIT |
| Cadu | pt-BR | 1 | 67 MB | mid | medium | — | MIT |
| Cadu | pt, br | 1 | 36 MB | mid | medium | — | MIT |
| Cadu | pt, br | 1 | 21 MB | mid | medium | — | MIT |
| Carlfm | es-ES | 1 | 27 MB | low | x_low | — | MIT |
| Carlfm | es, es | 1 | 17 MB | mid | x_low | — | MIT |
| Carlfm | es, es | 1 | 13 MB | mid | x_low | — | MIT |
| Chaowen | zh-CN | 1 | 60 MB | mid | medium | — | MIT |
| Chaowen | zh, cn | 1 | 29 MB | mid | medium | — | MIT |
| Chaowen | zh, cn | 1 | 14 MB | mid | medium | — | MIT |
| Chitwan | ne-NP | 1 | 67 MB | mid | medium | — | MIT |
| Chitwan | ne, np | 1 | 36 MB | mid | medium | — | MIT |
| Chitwan | ne, np | 1 | 21 MB | mid | medium | — | MIT |
| Claude | es-MX | 1 | 67 MB | high | high | — | MIT |
| Claude | es, mx | 1 | 36 MB | mid | high | — | MIT |
| Claude | es, mx | 1 | 21 MB | mid | high | — | MIT |
| Cori | en-GB | 1 | 116 MB | high | high | — | MIT |
| Cori | en, gb | 1 | 59 MB | mid | high | — | MIT |
| Cori | en, gb | 1 | 35 MB | mid | high | — | MIT |
| Cori | en-GB | 1 | 67 MB | mid | medium | — | MIT |
| Cori | en, gb | 1 | 36 MB | mid | medium | — | MIT |
| Cori | en, gb | 1 | 21 MB | mid | medium | — | MIT |
| Daniela | es-AR | 1 | 116 MB | high | high | — | MIT |
| Daniela | es, ar | 1 | 59 MB | mid | high | — | MIT |
| Daniela | es, ar | 1 | 35 MB | mid | high | — | MIT |
| Danny | en-US | 1 | 67 MB | low | low | — | MIT |
| Danny | en, us | 1 | 36 MB | mid | low | — | MIT |
| Danny | en, us | 1 | 21 MB | mid | low | — | MIT |
| Darkman | pl-PL | 1 | 67 MB | mid | medium | — | MIT |
| Darkman | pl, pl | 1 | 36 MB | mid | medium | — | MIT |
| Darkman | pl, pl | 1 | 21 MB | mid | medium | — | MIT |
| Davefx | es-ES | 1 | 67 MB | mid | medium | — | MIT |
| Davefx | es, es | 1 | 36 MB | mid | medium | — | MIT |
| Davefx | es, es | 1 | 21 MB | mid | medium | — | MIT |
| Denis | ru-RU | 1 | 67 MB | mid | medium | — | MIT |
| Denis | ru, ru | 1 | 36 MB | mid | medium | — | MIT |
| Denis | ru, ru | 1 | 21 MB | mid | medium | — | MIT |
| Dfki | tr-TR | 1 | 67 MB | mid | medium | — | MIT |
| Dfki | tr, tr | 1 | 36 MB | mid | medium | — | MIT |
| Dfki | tr, tr | 1 | 21 MB | mid | medium | — | MIT |
| Dii | de-DE | 1 | 67 MB | high | — | — | MIT |
| Dii | en-GB | 1 | 67 MB | high | — | — | MIT |
| Dii | it-IT | 1 | 67 MB | high | — | — | MIT |
| Dii | nl-NL | 1 | 67 MB | high | — | — | MIT |
| Dii | pt-BR | 1 | 67 MB | high | — | — | MIT |
| Dii | pt-PT | 1 | 67 MB | high | — | — | MIT |
| Dmitri | ru-RU | 1 | 67 MB | mid | medium | — | MIT |
| Dmitri | ru, ru | 1 | 36 MB | mid | medium | — | MIT |
| Dmitri | ru, ru | 1 | 21 MB | mid | medium | — | MIT |
| Edon | sq-AL | 1 | 67 MB | mid | medium | — | MIT |
| Edon | sq, al | 1 | 36 MB | mid | medium | — | MIT |
| Edon | sq, al | 1 | 21 MB | mid | medium | — | MIT |
| Edresson | pt-BR | 1 | 67 MB | low | low | — | MIT |
| Edresson | pt, br | 1 | 36 MB | mid | low | — | MIT |
| Edresson | pt, br | 1 | 21 MB | mid | low | — | MIT |
| Eva_k | de-DE | 1 | 27 MB | low | x_low | — | MIT |
| Eva_k | de, de | 1 | 17 MB | mid | x_low | — | MIT |
| Eva_k | de, de | 1 | 13 MB | mid | x_low | — | MIT |
| Faber | pt-BR | 1 | 67 MB | mid | medium | — | MIT |
| Faber | pt, br | 1 | 36 MB | mid | medium | — | MIT |
| Faber | pt, br | 1 | 21 MB | mid | medium | — | MIT |
| Fasih | ur-PK | 1 | 67 MB | mid | medium | — | MIT |
| Fasih | ur, pk | 1 | 36 MB | mid | medium | — | MIT |
| Fasih | ur, pk | 1 | 21 MB | mid | medium | — | MIT |
| Ganji | fa-IR | 1 | 67 MB | mid | medium | — | MIT |
| Ganji | fa, ir | 1 | 36 MB | mid | medium | — | MIT |
| Ganji | fa, ir | 1 | 21 MB | mid | medium | — | MIT |
| Ganji_adabi | fa-IR | 1 | 67 MB | mid | medium | — | MIT |
| Ganji_adabi | fa, ir | 1 | 36 MB | mid | medium | — | MIT |
| Ganji_adabi | fa, ir | 1 | 21 MB | mid | medium | — | MIT |
| Gilles | fr-FR | 1 | 67 MB | low | low | — | MIT |
| Gilles | fr, fr | 1 | 36 MB | mid | low | — | MIT |
| Gilles | fr, fr | 1 | 21 MB | mid | low | — | MIT |
| Glados | de-DE | 1 | 116 MB | high | — | — | MIT |
| Glados | de-DE | 1 | 67 MB | low | — | — | MIT |
| Glados | de-DE | 1 | 67 MB | mid | — | — | MIT |
| Glados | en-US | 1 | 116 MB | high | — | — | MIT |
| Glados | es-ES | 1 | 67 MB | mid | — | — | MIT |
| Glados_turret | de-DE | 1 | 116 MB | high | — | — | MIT |
| Glados_turret | de-DE | 1 | 67 MB | low | — | — | MIT |
| Glados_turret | de-DE | 1 | 67 MB | mid | — | — | MIT |
| Gosia | pl-PL | 1 | 67 MB | mid | medium | — | MIT |
| Gosia | pl, pl | 1 | 36 MB | mid | medium | — | MIT |
| Gosia | pl, pl | 1 | 21 MB | mid | medium | — | MIT |
| Gwryw_gogleddol | cy-GB | 1 | 67 MB | mid | medium | — | MIT |
| Gwryw_gogleddol | cy, gb | 1 | 36 MB | mid | medium | — | MIT |
| Gwryw_gogleddol | cy, gb | 1 | 21 MB | mid | medium | — | MIT |
| Gyro | fa-IR | 1 | 67 MB | mid | medium | — | MIT |
| Gyro | fa, ir | 1 | 36 MB | mid | medium | — | MIT |
| Gyro | fa, ir | 1 | 21 MB | mid | medium | — | MIT |
| Harri | fi-FI | 1 | 67 MB | low | low | — | MIT |
| Harri | fi, fi | 1 | 36 MB | mid | low | — | MIT |
| Harri | fi, fi | 1 | 21 MB | mid | low | — | MIT |
| Harri | fi-FI | 1 | 67 MB | mid | medium | — | MIT |
| Harri | fi, fi | 1 | 36 MB | mid | medium | — | MIT |
| Harri | fi, fi | 1 | 21 MB | mid | medium | — | MIT |
| Hfc_female | en-US | 1 | 67 MB | mid | medium | — | MIT |
| Hfc_female | en, us | 1 | 36 MB | mid | medium | — | MIT |
| Hfc_female | en, us | 1 | 21 MB | mid | medium | — | MIT |
| Hfc_male | en-US | 1 | 67 MB | mid | medium | — | MIT |
| Hfc_male | en, us | 1 | 36 MB | mid | medium | — | MIT |
| Hfc_male | en, us | 1 | 21 MB | mid | medium | — | MIT |
| Huayan | zh, cn | 1 | 67 MB | mid | medium | — | MIT |
| Imre | hu-HU | 1 | 67 MB | mid | medium | — | MIT |
| Imre | hu, hu | 1 | 36 MB | mid | medium | — | MIT |
| Imre | hu, hu | 1 | 21 MB | mid | medium | — | MIT |
| Irina | ru-RU | 1 | 67 MB | mid | medium | — | MIT |
| Irina | ru, ru | 1 | 36 MB | mid | medium | — | MIT |
| Irina | ru, ru | 1 | 21 MB | mid | medium | — | MIT |
| Iseke | kk-KZ | 1 | 27 MB | low | x_low | — | MIT |
| Iseke | kk, kz | 1 | 16 MB | mid | x_low | — | MIT |
| Iseke | kk, kz | 1 | 13 MB | mid | x_low | — | MIT |
| Jarvis_wg_glos | pl-PL | 1 | 67 MB | mid | — | — | MIT |
| Jeff | pt-BR | 1 | 67 MB | mid | medium | — | MIT |
| Jeff | pt, br | 1 | 36 MB | mid | medium | — | MIT |
| Jeff | pt, br | 1 | 21 MB | mid | medium | — | MIT |
| Jenny_dioco | en-GB | 1 | 67 MB | mid | medium | — | MIT |
| Jenny_dioco | en, gb | 1 | 36 MB | mid | medium | — | MIT |
| Jenny_dioco | en, gb | 1 | 21 MB | mid | medium | — | MIT |
| Jirka | cs-CZ | 1 | 67 MB | low | low | — | MIT |
| Jirka | cs, cz | 1 | 36 MB | mid | low | — | MIT |
| Jirka | cs, cz | 1 | 21 MB | mid | low | — | MIT |
| Jirka | cs-CZ | 1 | 67 MB | mid | medium | — | MIT |
| Jirka | cs, cz | 1 | 36 MB | mid | medium | — | MIT |
| Jirka | cs, cz | 1 | 21 MB | mid | medium | — | MIT |
| Joe | en-US | 1 | 67 MB | mid | medium | — | MIT |
| Joe | en, us | 1 | 36 MB | mid | medium | — | MIT |
| Joe | en, us | 1 | 21 MB | mid | medium | — | MIT |
| John | en-US | 1 | 67 MB | mid | medium | — | MIT |
| John | en, us | 1 | 36 MB | mid | medium | — | MIT |
| John | en, us | 1 | 21 MB | mid | medium | — | MIT |
| Justyna_wg_glos | pl-PL | 1 | 67 MB | mid | — | — | MIT |
| Kareem | ar-JO | 1 | 67 MB | low | low | — | MIT |
| Kareem | ar, jo | 1 | 36 MB | mid | low | — | MIT |
| Kareem | ar, jo | 1 | 21 MB | mid | low | — | MIT |
| Kareem | ar-JO | 1 | 67 MB | mid | medium | — | MIT |
| Kareem | ar, jo | 1 | 36 MB | mid | medium | — | MIT |
| Kareem | ar, jo | 1 | 21 MB | mid | medium | — | MIT |
| Karlsson | de-DE | 1 | 67 MB | low | low | — | MIT |
| Karlsson | de, de | 1 | 36 MB | mid | low | — | MIT |
| Karlsson | de, de | 1 | 21 MB | mid | low | — | MIT |
| Kathleen | en-US | 1 | 67 MB | low | low | — | MIT |
| Kathleen | en, us | 1 | 36 MB | mid | low | — | MIT |
| Kathleen | en, us | 1 | 21 MB | mid | low | — | MIT |
| Kerstin | de-DE | 1 | 67 MB | low | low | — | MIT |
| Kerstin | de, de | 1 | 36 MB | mid | low | — | MIT |
| Kerstin | de, de | 1 | 21 MB | mid | low | — | MIT |
| Kristin | en-US | 1 | 67 MB | mid | medium | — | MIT |
| Kristin | en, us | 1 | 36 MB | mid | medium | — | MIT |
| Kristin | en, us | 1 | 21 MB | mid | medium | — | MIT |
| Kusal | en-US | 1 | 67 MB | mid | medium | — | MIT |
| Kusal | en, us | 1 | 36 MB | mid | medium | — | MIT |
| Kusal | en, us | 1 | 21 MB | mid | medium | — | MIT |
| Lada | uk-UA | 1 | 27 MB | low | x_low | — | MIT |
| Lada | uk, ua | 1 | 17 MB | mid | x_low | — | MIT |
| Lada | uk, ua | 1 | 13 MB | mid | x_low | — | MIT |
| Lanfrica | sw-CD | 1 | 67 MB | mid | medium | — | MIT |
| Lanfrica | sw, cd | 1 | 36 MB | mid | medium | — | MIT |
| Lanfrica | sw, cd | 1 | 21 MB | mid | medium | — | MIT |
| Lessac | en-US | 1 | 116 MB | high | high | — | MIT |
| Lessac | en, us | 1 | 58 MB | mid | high | — | MIT |
| Lessac | en, us | 1 | 35 MB | mid | high | — | MIT |
| Lessac | en-US | 1 | 67 MB | low | low | — | MIT |
| Lessac | en, us | 1 | 36 MB | mid | low | — | MIT |
| Lessac | en, us | 1 | 21 MB | mid | low | — | MIT |
| Lessac | en-US | 1 | 67 MB | mid | medium | — | MIT |
| Lessac | en, us | 1 | 36 MB | mid | medium | — | MIT |
| Lessac | en, us | 1 | 21 MB | mid | medium | — | MIT |
| Lili | sk-SK | 1 | 67 MB | mid | medium | — | MIT |
| Lili | sk, sk | 1 | 36 MB | mid | medium | — | MIT |
| Lili | sk, sk | 1 | 21 MB | mid | medium | — | MIT |
| Lisa | sv-SE | 1 | 67 MB | mid | medium | — | MIT |
| Lisa | sv, se | 1 | 36 MB | mid | medium | — | MIT |
| Lisa | sv, se | 1 | 21 MB | mid | medium | — | MIT |
| Ljspeech | en-US | 1 | 116 MB | high | high | — | MIT |
| Ljspeech | en, us | 1 | 59 MB | mid | high | — | MIT |
| Ljspeech | en, us | 1 | 34 MB | mid | high | — | MIT |
| Ljspeech | en-US | 1 | 67 MB | mid | medium | — | MIT |
| Ljspeech | en, us | 1 | 36 MB | mid | medium | — | MIT |
| Ljspeech | en, us | 1 | 21 MB | mid | medium | — | MIT |
| Maider | eu-ES | 1 | 67 MB | mid | medium | — | MIT |
| Maider | eu, es | 1 | 36 MB | mid | medium | — | MIT |
| Maider | eu, es | 1 | 21 MB | mid | medium | — | MIT |
| Marylux | lb-LU | 1 | 67 MB | mid | medium | — | MIT |
| Marylux | lb, lu | 1 | 36 MB | mid | medium | — | MIT |
| Marylux | lb, lu | 1 | 21 MB | mid | medium | — | MIT |
| Mc_speech | pl-PL | 1 | 67 MB | mid | medium | — | MIT |
| Mc_speech | pl, pl, mc | 1 | 36 MB | mid | medium | — | MIT |
| Mc_speech | pl, pl, mc | 1 | 21 MB | mid | medium | — | MIT |
| Meera | ml-IN | 1 | 67 MB | mid | medium | — | MIT |
| Meera | ml, in | 1 | 36 MB | mid | medium | — | MIT |
| Meera | ml, in | 1 | 21 MB | mid | medium | — | MIT |
| Meski_wg_glos | pl-PL | 1 | 67 MB | mid | — | — | MIT |
| Mihai | ro-RO | 1 | 67 MB | mid | medium | — | MIT |
| Mihai | ro, ro | 1 | 21 MB | mid | medium | — | MIT |
| Miro | de-DE | 1 | 67 MB | high | — | — | MIT |
| Miro | en-GB | 1 | 67 MB | high | — | — | MIT |
| Miro | en-US | 1 | 67 MB | high | — | — | MIT |
| Miro | es-ES | 1 | 67 MB | high | — | — | MIT |
| Miro | fr-FR | 1 | 67 MB | high | — | — | MIT |
| Miro | it-IT | 1 | 67 MB | high | — | — | MIT |
| Miro | nl-NL | 1 | 67 MB | high | — | — | MIT |
| Miro | pt-BR | 1 | 67 MB | high | — | — | MIT |
| Miro | pt-PT | 1 | 67 MB | high | — | — | MIT |
| Nathalie | nl-BE | 1 | 67 MB | mid | medium | — | MIT |
| Nathalie | nl, be | 1 | 36 MB | mid | medium | — | MIT |
| Nathalie | nl, be | 1 | 21 MB | mid | medium | — | MIT |
| Nathalie | nl-BE | 1 | 27 MB | low | x_low | — | MIT |
| Nathalie | nl, be | 1 | 16 MB | mid | x_low | — | MIT |
| Nathalie | nl, be | 1 | 13 MB | mid | x_low | — | MIT |
| Natia | ka-GE | 1 | 67 MB | mid | medium | — | MIT |
| Natia | ka, ge | 1 | 36 MB | mid | medium | — | MIT |
| Natia | ka, ge | 1 | 21 MB | mid | medium | — | MIT |
| News_tts | id-ID | 1 | 67 MB | mid | medium | — | MIT |
| News_tts | id, id | 1 | 36 MB | mid | medium | — | MIT |
| News_tts | id, id | 1 | 21 MB | mid | medium | — | MIT |
| Norman | en-US | 1 | 67 MB | mid | medium | — | MIT |
| Norman | en, us | 1 | 36 MB | mid | medium | — | MIT |
| Norman | en, us | 1 | 21 MB | mid | medium | — | MIT |
| Northern_english_male | en-GB | 1 | 67 MB | mid | medium | — | MIT |
| Northern_english_male | en, gb | 1 | 36 MB | mid | medium | — | MIT |
| Northern_english_male | en, gb | 1 | 21 MB | mid | medium | — | MIT |
| Nst | sv-SE | 1 | 67 MB | mid | medium | — | MIT |
| Nst | sv, se | 1 | 36 MB | mid | medium | — | MIT |
| Nst | sv, se | 1 | 21 MB | mid | medium | — | MIT |
| Paola | it-IT | 1 | 67 MB | mid | medium | — | MIT |
| Paola | it, it | 1 | 36 MB | mid | medium | — | MIT |
| Paola | it, it | 1 | 21 MB | mid | medium | — | MIT |
| Pavoque | de-DE | 1 | 67 MB | low | low | — | MIT |
| Pavoque | de, de | 1 | 36 MB | mid | low | — | MIT |
| Pavoque | de, de | 1 | 21 MB | mid | low | — | MIT |
| Pim | nl-NL | 1 | 67 MB | mid | medium | — | MIT |
| Pim | nl, nl | 1 | 36 MB | mid | medium | — | MIT |
| Pim | nl, nl | 1 | 21 MB | mid | medium | — | MIT |
| Piper (109 speakers) | en-GB | 109 | 80 MB | mid | medium | — | MIT |
| Piper (12 speakers) | en-GB | 12 | 80 MB | mid | medium | — | MIT |
| Piper (18 speakers) | en-US | 18 | 80 MB | mid | medium | — | MIT |
| Piper (18 speakers) | ne-NP | 18 | 80 MB | mid | medium | — | MIT |
| Piper (18 speakers) | ne-NP | 18 | 33 MB | low | x_low | — | MIT |
| Piper (2 speakers) | es-ES | 2 | 80 MB | mid | medium | — | MIT |
| Piper (2 speakers) | fr-FR | 2 | 80 MB | mid | medium | — | MIT |
| Piper (2 speakers) | sr-RS | 2 | 80 MB | mid | medium | — | MIT |
| Piper (24 speakers) | en-US | 24 | 80 MB | mid | medium | — | MIT |
| Piper (3 speakers) | uk-UA | 3 | 80 MB | mid | medium | — | MIT |
| Piper (4 speakers) | en-GB | 4 | 80 MB | mid | medium | — | MIT |
| Piper (6 speakers) | en-GB | 6 | 80 MB | mid | — | — | MIT |
| Piper (6 speakers) | kk-KZ | 6 | 129 MB | high | high | — | MIT |
| Piper (65 speakers) | vi-VN | 65 | 33 MB | low | x_low | — | MIT |
| Piper (7 speakers) | cy-GB | 7 | 80 MB | mid | medium | — | MIT |
| Piper (8 speakers) | de-DE | 8 | 80 MB | mid | medium | — | MIT |
| Piper (8 speakers) | en-GB | 8 | 80 MB | mid | — | — | MIT |
| Piper (904 speakers) | en-US | 904 | 131 MB | high | high | — | MIT |
| Piper (904 speakers) | en-US | 904 | 82 MB | mid | medium | — | MIT |
| Pratham | hi-IN | 1 | 67 MB | mid | medium | — | MIT |
| Pratham | hi, in | 1 | 36 MB | mid | medium | — | MIT |
| Pratham | hi, in | 1 | 21 MB | mid | medium | — | MIT |
| Priyamvada | hi-IN | 1 | 67 MB | mid | medium | — | MIT |
| Priyamvada | hi, in | 1 | 36 MB | mid | medium | — | MIT |
| Priyamvada | hi, in | 1 | 21 MB | mid | medium | — | MIT |
| Ramona | de-DE | 1 | 67 MB | low | low | — | MIT |
| Ramona | de, de | 1 | 36 MB | mid | low | — | MIT |
| Ramona | de, de | 1 | 21 MB | mid | low | — | MIT |
| Rapunzelina | el-GR | 1 | 67 MB | low | low | — | MIT |
| Rapunzelina | el, gr | 1 | 36 MB | mid | low | — | MIT |
| Rapunzelina | el, gr | 1 | 21 MB | mid | low | — | MIT |
| Raya | kk-KZ | 1 | 26 MB | low | x_low | — | MIT |
| Raya | kk, kz | 1 | 16 MB | mid | x_low | — | MIT |
| Raya | kk, kz | 1 | 13 MB | mid | x_low | — | MIT |
| Rdh | nl, be | 1 | 67 MB | mid | medium | — | MIT |
| Rdh | nl, be | 1 | 26 MB | low | x_low | — | MIT |
| Reza_ibrahim | en-US | 1 | 67 MB | mid | medium | — | MIT |
| Reza_ibrahim | en, us | 1 | 36 MB | mid | medium | — | MIT |
| Reza_ibrahim | en, us | 1 | 21 MB | mid | medium | — | MIT |
| Reza_ibrahim | fa-IR | 1 | 67 MB | mid | medium | — | MIT |
| Reza_ibrahim | fa, ir | 1 | 36 MB | mid | medium | — | MIT |
| Reza_ibrahim | fa, ir | 1 | 21 MB | mid | medium | — | MIT |
| Riccardo | it-IT | 1 | 26 MB | low | x_low | — | MIT |
| Riccardo | it, it | 1 | 16 MB | mid | x_low | — | MIT |
| Riccardo | it, it | 1 | 13 MB | mid | x_low | — | MIT |
| Rohan | hi-IN | 1 | 67 MB | mid | medium | — | MIT |
| Rohan | hi, in | 1 | 36 MB | mid | medium | — | MIT |
| Rohan | hi, in | 1 | 21 MB | mid | medium | — | MIT |
| Ronnie | nl-NL | 1 | 67 MB | mid | medium | — | MIT |
| Ronnie | nl, nl | 1 | 36 MB | mid | medium | — | MIT |
| Ronnie | nl, nl | 1 | 21 MB | mid | medium | — | MIT |
| Ruslan | ru-RU | 1 | 67 MB | mid | medium | — | MIT |
| Ruslan | ru, ru | 1 | 36 MB | mid | medium | — | MIT |
| Ruslan | ru, ru | 1 | 21 MB | mid | medium | — | MIT |
| Ryan | en-US | 1 | 116 MB | high | high | — | MIT |
| Ryan | en, us | 1 | 59 MB | mid | high | — | MIT |
| Ryan | en, us | 1 | 34 MB | mid | high | — | MIT |
| Ryan | en-US | 1 | 67 MB | low | low | — | MIT |
| Ryan | en, us | 1 | 36 MB | mid | low | — | MIT |
| Ryan | en, us | 1 | 21 MB | mid | low | — | MIT |
| Ryan | en-US | 1 | 67 MB | mid | medium | — | MIT |
| Ryan | en, us | 1 | 36 MB | mid | medium | — | MIT |
| Ryan | en, us | 1 | 21 MB | mid | medium | — | MIT |
| SA_dii | ar-JO | 1 | 67 MB | high | — | — | MIT |
| SA_miro | ar-JO | 1 | 67 MB | high | — | — | MIT |
| SA_miro_V2 | ar-JO | 1 | 67 MB | high | — | — | MIT |
| Salka | is-IS | 1 | 67 MB | mid | medium | — | MIT |
| Salka | is, is | 1 | 36 MB | mid | medium | — | MIT |
| Salka | is, is | 1 | 20 MB | mid | medium | — | MIT |
| Sam | en-US | 1 | 67 MB | mid | medium | — | MIT |
| Sam | en, us | 1 | 36 MB | mid | medium | — | MIT |
| Sam | en, us | 1 | 21 MB | mid | medium | — | MIT |
| Siwis | fr-FR | 1 | 27 MB | low | low | — | MIT |
| Siwis | fr, fr | 1 | 17 MB | mid | low | — | MIT |
| Siwis | fr, fr | 1 | 13 MB | mid | low | — | MIT |
| Siwis | fr-FR | 1 | 67 MB | mid | medium | — | MIT |
| Siwis | fr, fr | 1 | 36 MB | mid | medium | — | MIT |
| Siwis | fr, fr | 1 | 21 MB | mid | medium | — | MIT |
| Southern_english_female | en-GB | 1 | 67 MB | low | low | — | MIT |
| Southern_english_female | en, gb | 1 | 36 MB | mid | low | — | MIT |
| Southern_english_female | en, gb | 1 | 21 MB | mid | low | — | MIT |
| Speaker_0 | ar, jo, sa | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | ar, jo, sa | 1 | 22 MB | mid | — | — | MIT |
| Speaker_0 | ar, jo, sa | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | ar, jo, sa | 1 | 22 MB | mid | — | — | MIT |
| Speaker_0 | ar, jo, sa | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | ar, jo, sa | 1 | 22 MB | mid | — | — | MIT |
| Speaker_0 | cy, gb, bu | 1 | 42 MB | mid | medium | — | MIT |
| Speaker_0 | cy, gb, bu | 1 | 23 MB | mid | medium | — | MIT |
| Speaker_0 | de, de | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 58 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 35 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 58 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 35 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 42 MB | mid | medium | — | MIT |
| Speaker_0 | de, de | 1 | 23 MB | mid | medium | — | MIT |
| Speaker_0 | en, gb | 1 | 42 MB | mid | medium | — | MIT |
| Speaker_0 | en, gb | 1 | 23 MB | mid | medium | — | MIT |
| Speaker_0 | en, gb | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 42 MB | mid | medium | — | MIT |
| Speaker_0 | en, gb | 1 | 23 MB | mid | medium | — | MIT |
| Speaker_0 | en, gb | 1 | 42 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 24 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 80 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 42 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 24 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 116 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 42 MB | mid | medium | — | MIT |
| Speaker_0 | en, gb | 1 | 23 MB | mid | medium | — | MIT |
| Speaker_0 | en, us | 1 | 42 MB | mid | medium | — | MIT |
| Speaker_0 | en, us | 1 | 23 MB | mid | medium | — | MIT |
| Speaker_0 | en, us | 1 | 67 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 58 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 35 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 42 MB | mid | medium | — | MIT |
| Speaker_0 | en, us | 1 | 23 MB | mid | medium | — | MIT |
| Speaker_0 | en, us | 1 | 66 MB | mid | high | — | MIT |
| Speaker_0 | en, us | 1 | 36 MB | mid | high | — | MIT |
| Speaker_0 | en, us | 1 | 43 MB | mid | medium | — | MIT |
| Speaker_0 | en, us | 1 | 23 MB | mid | medium | — | MIT |
| Speaker_0 | en, us | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | es | 1 | 67 MB | mid | — | — | MIT |
| Speaker_0 | es, es | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | es, es | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | es, es | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | es, es | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | es, es | 1 | 42 MB | mid | medium | — | MIT |
| Speaker_0 | es, es | 1 | 23 MB | mid | medium | — | MIT |
| Speaker_0 | fa | 1 | 67 MB | mid | — | — | MIT |
| Speaker_0 | fa, en | 1 | 67 MB | mid | — | — | MIT |
| Speaker_0 | fr, fr | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | fr, fr | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | fr, fr | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | fr, fr | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | fr, fr | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | fr, fr | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | fr, fr | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | fr, fr | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | fr, fr | 1 | 42 MB | mid | medium | — | MIT |
| Speaker_0 | fr, fr | 1 | 23 MB | mid | medium | — | MIT |
| Speaker_0 | it, it | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | it, it | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | it, it | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | it, it | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | kk, kz | 1 | 65 MB | mid | high | — | MIT |
| Speaker_0 | kk, kz | 1 | 36 MB | mid | high | — | MIT |
| Speaker_0 | ku, tr | 1 | 42 MB | mid | medium | — | MIT |
| Speaker_0 | ku, tr | 1 | 24 MB | mid | medium | — | MIT |
| Speaker_0 | ne, np | 1 | 42 MB | mid | medium | — | MIT |
| Speaker_0 | ne, np | 1 | 24 MB | mid | medium | — | MIT |
| Speaker_0 | ne, np | 1 | 20 MB | mid | x_low | — | MIT |
| Speaker_0 | ne, np | 1 | 15 MB | mid | x_low | — | MIT |
| Speaker_0 | nl, nl | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | nl, nl | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | nl, nl | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | nl, nl | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | pl, pl, wg | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | pl, pl, wg | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | pl, pl, wg | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | pl, pl, wg | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | pl, pl, wg | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | pl, pl, wg | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | pl, pl, wg | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | pl, pl, wg | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | pt, br | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | pt, br | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | pt, br | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | pt, br | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | pt, pt | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | pt, pt | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | pt, pt | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | pt, pt | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | pt, pt | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | pt, pt | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | sr, rs | 1 | 42 MB | mid | medium | — | MIT |
| Speaker_0 | sr, rs | 1 | 23 MB | mid | medium | — | MIT |
| Speaker_0 | tr, tr | 1 | 67 MB | mid | — | — | MIT |
| Speaker_0 | tr, tr | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | tr, tr | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | tr, tr | 1 | 67 MB | mid | — | — | MIT |
| Speaker_0 | tr, tr | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | tr, tr | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | uk, ua | 1 | 42 MB | mid | medium | — | MIT |
| Speaker_0 | uk, ua | 1 | 23 MB | mid | medium | — | MIT |
| Speaker_0 | vi, vn | 1 | 20 MB | mid | x_low | — | MIT |
| Speaker_0 | vi, vn | 1 | 15 MB | mid | x_low | — | MIT |
| Steinn | is-IS | 1 | 67 MB | mid | medium | — | MIT |
| Steinn | is, is | 1 | 36 MB | mid | medium | — | MIT |
| Steinn | is, is | 1 | 20 MB | mid | medium | — | MIT |
| Talesyntese | da-DK | 1 | 67 MB | mid | medium | — | MIT |
| Talesyntese | da, dk | 1 | 36 MB | mid | medium | — | MIT |
| Talesyntese | da, dk | 1 | 21 MB | mid | medium | — | MIT |
| Talesyntese | no-NO | 1 | 67 MB | mid | medium | — | MIT |
| Talesyntese | no, no | 1 | 36 MB | mid | medium | — | MIT |
| Talesyntese | no, no | 1 | 21 MB | mid | medium | — | MIT |
| Thorsten | de-DE | 1 | 116 MB | high | high | — | MIT |
| Thorsten | de, de | 1 | 59 MB | mid | high | — | MIT |
| Thorsten | de, de | 1 | 35 MB | mid | high | — | MIT |
| Thorsten | de-DE | 1 | 67 MB | low | low | — | MIT |
| Thorsten | de, de | 1 | 36 MB | mid | low | — | MIT |
| Thorsten | de, de | 1 | 21 MB | mid | low | — | MIT |
| Thorsten | de-DE | 1 | 67 MB | mid | medium | — | MIT |
| Thorsten | de, de | 1 | 36 MB | mid | medium | — | MIT |
| Thorsten | de, de | 1 | 21 MB | mid | medium | — | MIT |
| Tjiho | fr-FR | 1 | 67 MB | mid | — | — | MIT |
| Tjiho | fr-FR | 1 | 67 MB | mid | — | — | MIT |
| Tjiho | fr-FR | 1 | 67 MB | mid | — | — | MIT |
| Tom | fr-FR | 1 | 67 MB | mid | medium | — | MIT |
| Tom | fr, fr | 1 | 36 MB | mid | medium | — | MIT |
| Tom | fr, fr | 1 | 21 MB | mid | medium | — | MIT |
| Tugao | pt-PT | 1 | 67 MB | mid | — | — | MIT |
| Ugla | is-IS | 1 | 67 MB | mid | medium | — | MIT |
| Ugla | is, is | 1 | 36 MB | mid | medium | — | MIT |
| Ugla | is, is | 1 | 20 MB | mid | medium | — | MIT |
| Upc_ona | ca-ES | 1 | 67 MB | mid | medium | — | MIT |
| Upc_ona | ca, es | 1 | 36 MB | mid | medium | — | MIT |
| Upc_ona | ca, es | 1 | 21 MB | mid | medium | — | MIT |
| Upc_ona | ca-ES | 1 | 27 MB | low | x_low | — | MIT |
| Upc_ona | ca, es | 1 | 17 MB | mid | x_low | — | MIT |
| Upc_ona | ca, es | 1 | 13 MB | mid | x_low | — | MIT |
| Upc_pau | ca-ES | 1 | 27 MB | low | x_low | — | MIT |
| Upc_pau | ca, es | 1 | 17 MB | mid | x_low | — | MIT |
| Upc_pau | ca, es | 1 | 13 MB | mid | x_low | — | MIT |
| Vais1000 | vi-VN | 1 | 67 MB | mid | medium | — | MIT |
| Vais1000 | vi, vn | 1 | 36 MB | mid | medium | — | MIT |
| Vais1000 | vi, vn | 1 | 22 MB | mid | medium | — | MIT |
| Xiao_ya | zh-CN | 1 | 60 MB | mid | medium | — | MIT |
| Xiao_ya | zh, cn, ya | 1 | 29 MB | mid | medium | — | MIT |
| Xiao_ya | zh, cn, ya | 1 | 14 MB | mid | medium | — | MIT |
| Zenski_wg_glos | pl-PL | 1 | 67 MB | mid | — | — | MIT |

## kokoro (6)

Higher-quality multilingual VITS variant (Kokoro-82M). 80–360 MB per voice; English bundles ship 1–50 speakers in a single model.

| Title | Languages | Speakers | Size | Tier | Quality | RTF | License |
|---|---|---:|---:|---|---|---:|---|
| Kokoro (103 speakers) | zh-CN, en-US | 103 | 365 MB | high | — | — | Apache-2.0 |
| Kokoro (11 speakers) | en-US | 11 | 320 MB | high | — | — | Apache-2.0 |
| Kokoro (53 speakers) | zh-CN, en-US | 53 | 349 MB | high | — | — | Apache-2.0 |
| Speaker_0 | en | 1 | 103 MB | high | — | — | Apache-2.0 |
| Speaker_0 | en | 1 | 132 MB | high | — | — | Apache-2.0 |
| Speaker_0 | en | 1 | 147 MB | high | — | — | Apache-2.0 |

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

## vits (68)

| Title | Languages | Speakers | Size | Tier | Quality | RTF | License |
|---|---|---:|---:|---|---|---:|---|
| Speaker_0 | hf | 1 | 108 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | bg, cv | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | bn | 1 | 108 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | cs, cv | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | da, cv | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | de | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | en | 1 | 115 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | en | 1 | 115 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | en | 1 | 122 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | es | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | et, cv | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | fi | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | fr | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | ga, cv | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | hr, cv | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | lt, cv | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | lv, cv | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | mt, cv | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | nl | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | pl | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | pt, cv | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | ro, cv | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | sk, cv | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | sl, cv | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | sv, cv | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | uk | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | en, us | 1 | 32 MB | low | — | — | Apache-2.0 |
| Speaker_0 | en, us | 1 | 74 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | zh | 1 | 32 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | en | 1 | 109 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | en | 1 | 163 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | zh, en | 1 | 167 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | af, za | 1 | 80 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | bn | 1 | 80 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | el, gr | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | es, es | 1 | 80 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | fa | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | fi, fi | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | gu, in | 1 | 80 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | hu, hu | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | ko, ko | 1 | 67 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | ne, np, ne | 1 | 80 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | pl, pl | 1 | 80 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | tn, za | 1 | 80 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | vi, vn | 1 | 67 MB | mid | — | — | Apache-2.0 |
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
| Speaker_0 | zh, hf | 1 | 121 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | zh, hf | 1 | 121 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | zh, hf | 1 | 121 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | zh, hf | 1 | 121 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | zh, hf | 1 | 121 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | zh, hf | 1 | 119 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | zh, hf | 1 | 119 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | zh, hf | 1 | 119 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | zh, hf | 1 | 119 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | zh, hf | 1 | 119 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | zh, hf | 1 | 121 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | zh, hf | 1 | 121 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | zh, hf | 1 | 121 MB | mid | — | — | Apache-2.0 |

---

_Source of truth: [`catalog/v1/models.json`](../catalog/v1/models.json). Each voice entry includes the bundle URL, sha256, sample rate, and per-(speaker, language) audition URLs in the JSON; this page is just the human-readable index._
