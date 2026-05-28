# Voice catalog

*Auto-generated from `catalog/v1/models.json` on 2026-05-28 by `tools/catalog/build_model_list.py` (run from the weekly [catalog-refresh](../.github/workflows/catalog-refresh.yml) workflow). Do not edit by hand — the next refresh will overwrite your changes.*

## Summary

- **630 voices** across **8 model families**
- **134 languages** covered
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
| 25hours_single | vi-VN | 1 | 67 MB | low | — | — | MIT |
| Aivars | lv-LV | 1 | 67 MB | mid | — | — | MIT |
| Alan | en-GB | 1 | 67 MB | low | — | — | MIT |
| Alan | en-GB | 1 | 67 MB | mid | — | — | MIT |
| Alba | en-GB | 1 | 67 MB | mid | — | — | MIT |
| Ald | es-MX | 1 | 67 MB | mid | — | — | MIT |
| Alex | nl-NL | 1 | 67 MB | mid | — | — | MIT |
| Alma | sv-SE | 1 | 67 MB | mid | — | — | MIT |
| Amir | fa-IR | 1 | 67 MB | mid | — | — | MIT |
| Amy | en-US | 1 | 67 MB | low | — | — | MIT |
| Amy | en-US | 1 | 67 MB | mid | — | — | MIT |
| Anna | hu-HU | 1 | 67 MB | mid | — | — | MIT |
| Antton | eu-ES | 1 | 67 MB | mid | — | — | MIT |
| Arjun | ml-IN | 1 | 67 MB | mid | — | — | MIT |
| Artur | sl-SI | 1 | 67 MB | mid | — | — | MIT |
| Bass | pl-PL | 1 | 116 MB | high | — | — | MIT |
| Berfin_renas | ku-TR | 1 | 80 MB | mid | — | — | MIT |
| Berta | hu-HU | 1 | 67 MB | mid | — | — | MIT |
| Bryce | en-US | 1 | 67 MB | mid | — | — | MIT |
| Bui | is-IS | 1 | 67 MB | mid | — | — | MIT |
| Cadu | pt-BR | 1 | 67 MB | mid | — | — | MIT |
| Carlfm | es-ES | 1 | 27 MB | low | — | — | MIT |
| Chaowen | zh-CN | 1 | 60 MB | mid | — | — | MIT |
| Chitwan | ne-NP | 1 | 67 MB | mid | — | — | MIT |
| Claude | es-MX | 1 | 67 MB | high | — | — | MIT |
| Cori | en-GB | 1 | 116 MB | high | — | — | MIT |
| Cori | en-GB | 1 | 67 MB | mid | — | — | MIT |
| Daniela | es-AR | 1 | 116 MB | high | — | — | MIT |
| Danny | en-US | 1 | 67 MB | low | — | — | MIT |
| Darkman | pl-PL | 1 | 67 MB | mid | — | — | MIT |
| Davefx | es-ES | 1 | 67 MB | mid | — | — | MIT |
| Denis | ru-RU | 1 | 67 MB | mid | — | — | MIT |
| Dfki | tr-TR | 1 | 67 MB | mid | — | — | MIT |
| Dii | de-DE | 1 | 67 MB | high | — | — | MIT |
| Dii | en-GB | 1 | 67 MB | high | — | — | MIT |
| Dii | it-IT | 1 | 67 MB | high | — | — | MIT |
| Dii | nl-NL | 1 | 67 MB | high | — | — | MIT |
| Dii | pt-BR | 1 | 67 MB | high | — | — | MIT |
| Dii | pt-PT | 1 | 67 MB | high | — | — | MIT |
| Dmitri | ru-RU | 1 | 67 MB | mid | — | — | MIT |
| Edon | sq-AL | 1 | 67 MB | mid | — | — | MIT |
| Edresson | pt-BR | 1 | 67 MB | low | — | — | MIT |
| Eva_k | de-DE | 1 | 27 MB | low | — | — | MIT |
| Faber | pt-BR | 1 | 67 MB | mid | — | — | MIT |
| Fasih | ur-PK | 1 | 67 MB | mid | — | — | MIT |
| Ganji | fa-IR | 1 | 67 MB | mid | — | — | MIT |
| Ganji_adabi | fa-IR | 1 | 67 MB | mid | — | — | MIT |
| Gilles | fr-FR | 1 | 67 MB | low | — | — | MIT |
| Glados | de-DE | 1 | 116 MB | high | — | — | MIT |
| Glados | de-DE | 1 | 67 MB | low | — | — | MIT |
| Glados | de-DE | 1 | 67 MB | mid | — | — | MIT |
| Glados | en-US | 1 | 116 MB | high | — | — | MIT |
| Glados | es-ES | 1 | 67 MB | mid | — | — | MIT |
| Glados_turret | de-DE | 1 | 116 MB | high | — | — | MIT |
| Glados_turret | de-DE | 1 | 67 MB | low | — | — | MIT |
| Glados_turret | de-DE | 1 | 67 MB | mid | — | — | MIT |
| Gosia | pl-PL | 1 | 67 MB | mid | — | — | MIT |
| Gwryw_gogleddol | cy-GB | 1 | 67 MB | mid | — | — | MIT |
| Gyro | fa-IR | 1 | 67 MB | mid | — | — | MIT |
| Harri | fi-FI | 1 | 67 MB | low | — | — | MIT |
| Harri | fi-FI | 1 | 67 MB | mid | — | — | MIT |
| Hfc_female | en-US | 1 | 67 MB | mid | — | — | MIT |
| Hfc_male | en-US | 1 | 67 MB | mid | — | — | MIT |
| Imre | hu-HU | 1 | 67 MB | mid | — | — | MIT |
| Irina | ru-RU | 1 | 67 MB | mid | — | — | MIT |
| Iseke | kk-KZ | 1 | 27 MB | low | — | — | MIT |
| Jarvis_wg_glos | pl-PL | 1 | 67 MB | mid | — | — | MIT |
| Jeff | pt-BR | 1 | 67 MB | mid | — | — | MIT |
| Jenny_dioco | en-GB | 1 | 67 MB | mid | — | — | MIT |
| Jirka | cs-CZ | 1 | 67 MB | low | — | — | MIT |
| Jirka | cs-CZ | 1 | 67 MB | mid | — | — | MIT |
| Joe | en-US | 1 | 67 MB | mid | — | — | MIT |
| John | en-US | 1 | 67 MB | mid | — | — | MIT |
| Justyna_wg_glos | pl-PL | 1 | 67 MB | mid | — | — | MIT |
| Kareem | ar-JO | 1 | 67 MB | low | — | — | MIT |
| Kareem | ar-JO | 1 | 67 MB | mid | — | — | MIT |
| Karlsson | de-DE | 1 | 67 MB | low | — | — | MIT |
| Kathleen | en-US | 1 | 67 MB | low | — | — | MIT |
| Kerstin | de-DE | 1 | 67 MB | low | — | — | MIT |
| Kristin | en-US | 1 | 67 MB | mid | — | — | MIT |
| Kusal | en-US | 1 | 67 MB | mid | — | — | MIT |
| Lada | uk-UA | 1 | 27 MB | low | — | — | MIT |
| Lanfrica | sw-CD | 1 | 67 MB | mid | — | — | MIT |
| Lessac | en-US | 1 | 116 MB | high | — | — | MIT |
| Lessac | en-US | 1 | 67 MB | low | — | — | MIT |
| Lessac | en-US | 1 | 67 MB | mid | — | — | MIT |
| Lili | sk-SK | 1 | 67 MB | mid | — | — | MIT |
| Lisa | sv-SE | 1 | 67 MB | mid | — | — | MIT |
| Ljspeech | en-US | 1 | 116 MB | high | — | — | MIT |
| Ljspeech | en-US | 1 | 67 MB | mid | — | — | MIT |
| Maider | eu-ES | 1 | 67 MB | mid | — | — | MIT |
| Marylux | lb-LU | 1 | 67 MB | mid | — | — | MIT |
| Mc_speech | pl-PL | 1 | 67 MB | mid | — | — | MIT |
| Meera | ml-IN | 1 | 67 MB | mid | — | — | MIT |
| Meski_wg_glos | pl-PL | 1 | 67 MB | mid | — | — | MIT |
| Mihai | ro-RO | 1 | 67 MB | mid | — | — | MIT |
| Miro | de-DE | 1 | 67 MB | high | — | — | MIT |
| Miro | en-GB | 1 | 67 MB | high | — | — | MIT |
| Miro | en-US | 1 | 67 MB | high | — | — | MIT |
| Miro | es-ES | 1 | 67 MB | high | — | — | MIT |
| Miro | fr-FR | 1 | 67 MB | high | — | — | MIT |
| Miro | it-IT | 1 | 67 MB | high | — | — | MIT |
| Miro | nl-NL | 1 | 67 MB | high | — | — | MIT |
| Miro | pt-BR | 1 | 67 MB | high | — | — | MIT |
| Miro | pt-PT | 1 | 67 MB | high | — | — | MIT |
| Nathalie | nl-BE | 1 | 67 MB | mid | — | — | MIT |
| Nathalie | nl-BE | 1 | 27 MB | low | — | — | MIT |
| Natia | ka-GE | 1 | 67 MB | mid | — | — | MIT |
| News_tts | id-ID | 1 | 67 MB | mid | — | — | MIT |
| Norman | en-US | 1 | 67 MB | mid | — | — | MIT |
| Northern_english_male | en-GB | 1 | 67 MB | mid | — | — | MIT |
| Nst | sv-SE | 1 | 67 MB | mid | — | — | MIT |
| Paola | it-IT | 1 | 67 MB | mid | — | — | MIT |
| Pavoque | de-DE | 1 | 67 MB | low | — | — | MIT |
| Pim | nl-NL | 1 | 67 MB | mid | — | — | MIT |
| Piper (109 speakers) | en-GB | 109 | 80 MB | mid | — | — | MIT |
| Piper (12 speakers) | en-GB | 12 | 80 MB | mid | — | — | MIT |
| Piper (18 speakers) | en-US | 18 | 80 MB | mid | — | — | MIT |
| Piper (18 speakers) | ne-NP | 18 | 80 MB | mid | — | — | MIT |
| Piper (18 speakers) | ne-NP | 18 | 33 MB | low | — | — | MIT |
| Piper (2 speakers) | es-ES | 2 | 80 MB | mid | — | — | MIT |
| Piper (2 speakers) | fr-FR | 2 | 80 MB | mid | — | — | MIT |
| Piper (2 speakers) | sr-RS | 2 | 80 MB | mid | — | — | MIT |
| Piper (24 speakers) | en-US | 24 | 80 MB | mid | — | — | MIT |
| Piper (3 speakers) | uk-UA | 3 | 80 MB | mid | — | — | MIT |
| Piper (4 speakers) | en-GB | 4 | 80 MB | mid | — | — | MIT |
| Piper (6 speakers) | en-GB | 6 | 80 MB | mid | — | — | MIT |
| Piper (6 speakers) | kk-KZ | 6 | 129 MB | high | — | — | MIT |
| Piper (65 speakers) | vi-VN | 65 | 33 MB | low | — | — | MIT |
| Piper (7 speakers) | cy-GB | 7 | 80 MB | mid | — | — | MIT |
| Piper (8 speakers) | de-DE | 8 | 80 MB | mid | — | — | MIT |
| Piper (8 speakers) | en-GB | 8 | 80 MB | mid | — | — | MIT |
| Piper (904 speakers) | en-US | 904 | 131 MB | high | — | — | MIT |
| Piper (904 speakers) | en-US | 904 | 82 MB | mid | — | — | MIT |
| Pratham | hi-IN | 1 | 67 MB | mid | — | — | MIT |
| Priyamvada | hi-IN | 1 | 67 MB | mid | — | — | MIT |
| Ramona | de-DE | 1 | 67 MB | low | — | — | MIT |
| Rapunzelina | el-GR | 1 | 67 MB | low | — | — | MIT |
| Raya | kk-KZ | 1 | 26 MB | low | — | — | MIT |
| Reza_ibrahim | en-US | 1 | 67 MB | mid | — | — | MIT |
| Reza_ibrahim | fa-IR | 1 | 67 MB | mid | — | — | MIT |
| Riccardo | it-IT | 1 | 26 MB | low | — | — | MIT |
| Rohan | hi-IN | 1 | 67 MB | mid | — | — | MIT |
| Ronnie | nl-NL | 1 | 67 MB | mid | — | — | MIT |
| Ruslan | ru-RU | 1 | 67 MB | mid | — | — | MIT |
| Ryan | en-US | 1 | 116 MB | high | — | — | MIT |
| Ryan | en-US | 1 | 67 MB | low | — | — | MIT |
| Ryan | en-US | 1 | 67 MB | mid | — | — | MIT |
| SA_dii | ar-JO | 1 | 67 MB | high | — | — | MIT |
| SA_miro | ar-JO | 1 | 67 MB | high | — | — | MIT |
| SA_miro_V2 | ar-JO | 1 | 67 MB | high | — | — | MIT |
| Salka | is-IS | 1 | 67 MB | mid | — | — | MIT |
| Sam | en-US | 1 | 67 MB | mid | — | — | MIT |
| Siwis | fr-FR | 1 | 27 MB | low | — | — | MIT |
| Siwis | fr-FR | 1 | 67 MB | mid | — | — | MIT |
| Southern_english_female | en-GB | 1 | 67 MB | low | — | — | MIT |
| Speaker_0 | ar, jo, sa | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | ar, jo, sa | 1 | 22 MB | mid | — | — | MIT |
| Speaker_0 | ar, jo, sa | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | ar, jo, sa | 1 | 22 MB | mid | — | — | MIT |
| Speaker_0 | ar, jo, sa | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | ar, jo, sa | 1 | 22 MB | mid | — | — | MIT |
| Speaker_0 | ar, jo | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | ar, jo | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | ar, jo | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | ar, jo | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | ca, es | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | ca, es | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | ca, es | 1 | 17 MB | mid | — | — | MIT |
| Speaker_0 | ca, es | 1 | 13 MB | mid | — | — | MIT |
| Speaker_0 | ca, es | 1 | 17 MB | mid | — | — | MIT |
| Speaker_0 | ca, es | 1 | 13 MB | mid | — | — | MIT |
| Speaker_0 | cs, cz | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | cs, cz | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | cs, cz | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | cs, cz | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | cy, gb, bu | 1 | 42 MB | mid | — | — | MIT |
| Speaker_0 | cy, gb, bu | 1 | 23 MB | mid | — | — | MIT |
| Speaker_0 | cy, gb | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | cy, gb | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | da, dk | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | da, dk | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 17 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 13 MB | mid | — | — | MIT |
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
| Speaker_0 | de, de | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 59 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 35 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 42 MB | mid | — | — | MIT |
| Speaker_0 | de, de | 1 | 23 MB | mid | — | — | MIT |
| Speaker_0 | el, gr | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | el, gr | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 42 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 23 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 59 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 35 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 42 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 23 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 42 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 24 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 80 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 42 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 24 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 116 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 42 MB | mid | — | — | MIT |
| Speaker_0 | en, gb | 1 | 23 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 42 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 23 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 67 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 58 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 35 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 42 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 23 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 58 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 35 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 66 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 43 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 23 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 59 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 34 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 59 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 34 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | en, us | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | es | 1 | 67 MB | mid | — | — | MIT |
| Speaker_0 | es, ar | 1 | 59 MB | mid | — | — | MIT |
| Speaker_0 | es, ar | 1 | 35 MB | mid | — | — | MIT |
| Speaker_0 | es, es | 1 | 17 MB | mid | — | — | MIT |
| Speaker_0 | es, es | 1 | 13 MB | mid | — | — | MIT |
| Speaker_0 | es, es | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | es, es | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | es, es | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | es, es | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | es, es | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | es, es | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | es, es | 1 | 42 MB | mid | — | — | MIT |
| Speaker_0 | es, es | 1 | 23 MB | mid | — | — | MIT |
| Speaker_0 | es, mx | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | es, mx | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | es, mx | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | es, mx | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | eu, es | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | eu, es | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | eu, es | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | eu, es | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | fa | 1 | 67 MB | mid | — | — | MIT |
| Speaker_0 | fa, ir | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | fa, ir | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | fa, ir | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | fa, ir | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | fa, ir | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | fa, ir | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | fa, ir | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | fa, ir | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | fa, ir | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | fa, ir | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | fa, en | 1 | 67 MB | mid | — | — | MIT |
| Speaker_0 | fi, fi | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | fi, fi | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | fi, fi | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | fi, fi | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | fr, fr | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | fr, fr | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | fr, fr | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | fr, fr | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | fr, fr | 1 | 17 MB | mid | — | — | MIT |
| Speaker_0 | fr, fr | 1 | 13 MB | mid | — | — | MIT |
| Speaker_0 | fr, fr | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | fr, fr | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | fr, fr | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | fr, fr | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | fr, fr | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | fr, fr | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | fr, fr | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | fr, fr | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | fr, fr | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | fr, fr | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | fr, fr | 1 | 42 MB | mid | — | — | MIT |
| Speaker_0 | fr, fr | 1 | 23 MB | mid | — | — | MIT |
| Speaker_0 | hi, in | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | hi, in | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | hi, in | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | hi, in | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | hi, in | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | hi, in | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | hu, hu | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | hu, hu | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | hu, hu | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | hu, hu | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | hu, hu | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | hu, hu | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | id, id | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | id, id | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | is, is | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | is, is | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | is, is | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | is, is | 1 | 20 MB | mid | — | — | MIT |
| Speaker_0 | is, is | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | is, is | 1 | 20 MB | mid | — | — | MIT |
| Speaker_0 | is, is | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | is, is | 1 | 20 MB | mid | — | — | MIT |
| Speaker_0 | it, it | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | it, it | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | it, it | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | it, it | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | it, it | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | it, it | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | it, it | 1 | 16 MB | mid | — | — | MIT |
| Speaker_0 | it, it | 1 | 13 MB | mid | — | — | MIT |
| Speaker_0 | ka, ge | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | ka, ge | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | kk, kz | 1 | 16 MB | mid | — | — | MIT |
| Speaker_0 | kk, kz | 1 | 13 MB | mid | — | — | MIT |
| Speaker_0 | kk, kz | 1 | 65 MB | mid | — | — | MIT |
| Speaker_0 | kk, kz | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | kk, kz | 1 | 16 MB | mid | — | — | MIT |
| Speaker_0 | kk, kz | 1 | 13 MB | mid | — | — | MIT |
| Speaker_0 | ku, tr | 1 | 42 MB | mid | — | — | MIT |
| Speaker_0 | ku, tr | 1 | 24 MB | mid | — | — | MIT |
| Speaker_0 | lb, lu | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | lb, lu | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | lv, lv | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | lv, lv | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | ml, in | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | ml, in | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | ml, in | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | ml, in | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | ne, np | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | ne, np | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | ne, np | 1 | 42 MB | mid | — | — | MIT |
| Speaker_0 | ne, np | 1 | 24 MB | mid | — | — | MIT |
| Speaker_0 | ne, np | 1 | 20 MB | mid | — | — | MIT |
| Speaker_0 | ne, np | 1 | 15 MB | mid | — | — | MIT |
| Speaker_0 | nl, be | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | nl, be | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | nl, be | 1 | 16 MB | mid | — | — | MIT |
| Speaker_0 | nl, be | 1 | 13 MB | mid | — | — | MIT |
| Speaker_0 | nl, be | 1 | 67 MB | mid | — | — | MIT |
| Speaker_0 | nl, be | 1 | 26 MB | low | — | — | MIT |
| Speaker_0 | nl, nl | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | nl, nl | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | nl, nl | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | nl, nl | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | nl, nl | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | nl, nl | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | nl, nl | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | nl, nl | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | nl, nl | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | nl, nl | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | no, no | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | no, no | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | pl, pl | 1 | 59 MB | mid | — | — | MIT |
| Speaker_0 | pl, pl | 1 | 35 MB | mid | — | — | MIT |
| Speaker_0 | pl, pl | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | pl, pl | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | pl, pl | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | pl, pl | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | pl, pl, wg | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | pl, pl, wg | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | pl, pl, wg | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | pl, pl, wg | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | pl, pl, mc | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | pl, pl, mc | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | pl, pl, wg | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | pl, pl, wg | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | pl, pl, wg | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | pl, pl, wg | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | pt, br | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | pt, br | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | pt, br | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | pt, br | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | pt, br | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | pt, br | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | pt, br | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | pt, br | 1 | 21 MB | mid | — | — | MIT |
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
| Speaker_0 | ro, ro | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | ru, ru | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | ru, ru | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | ru, ru | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | ru, ru | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | ru, ru | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | ru, ru | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | ru, ru | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | ru, ru | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | sk, sk | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | sk, sk | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | sl, si | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | sl, si | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | sq, al | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | sq, al | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | sr, rs | 1 | 42 MB | mid | — | — | MIT |
| Speaker_0 | sr, rs | 1 | 23 MB | mid | — | — | MIT |
| Speaker_0 | sv, se | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | sv, se | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | sv, se | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | sv, se | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | sv, se | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | sv, se | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | sw, cd | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | sw, cd | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | tr, tr | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | tr, tr | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | tr, tr | 1 | 67 MB | mid | — | — | MIT |
| Speaker_0 | tr, tr | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | tr, tr | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | tr, tr | 1 | 67 MB | mid | — | — | MIT |
| Speaker_0 | tr, tr | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | tr, tr | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | uk, ua | 1 | 17 MB | mid | — | — | MIT |
| Speaker_0 | uk, ua | 1 | 13 MB | mid | — | — | MIT |
| Speaker_0 | uk, ua | 1 | 42 MB | mid | — | — | MIT |
| Speaker_0 | uk, ua | 1 | 23 MB | mid | — | — | MIT |
| Speaker_0 | ur, pk | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | ur, pk | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | vi, vn | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | vi, vn | 1 | 21 MB | mid | — | — | MIT |
| Speaker_0 | vi, vn | 1 | 36 MB | mid | — | — | MIT |
| Speaker_0 | vi, vn | 1 | 22 MB | mid | — | — | MIT |
| Speaker_0 | vi, vn | 1 | 20 MB | mid | — | — | MIT |
| Speaker_0 | vi, vn | 1 | 15 MB | mid | — | — | MIT |
| Speaker_0 | zh, cn | 1 | 29 MB | mid | — | — | MIT |
| Speaker_0 | zh, cn | 1 | 14 MB | mid | — | — | MIT |
| Speaker_0 | zh, cn | 1 | 67 MB | mid | — | — | MIT |
| Speaker_0 | zh, cn, ya | 1 | 29 MB | mid | — | — | MIT |
| Speaker_0 | zh, cn, ya | 1 | 14 MB | mid | — | — | MIT |
| Steinn | is-IS | 1 | 67 MB | mid | — | — | MIT |
| Talesyntese | da-DK | 1 | 67 MB | mid | — | — | MIT |
| Talesyntese | no-NO | 1 | 67 MB | mid | — | — | MIT |
| Thorsten | de-DE | 1 | 116 MB | high | — | — | MIT |
| Thorsten | de-DE | 1 | 67 MB | low | — | — | MIT |
| Thorsten | de-DE | 1 | 67 MB | mid | — | — | MIT |
| Tjiho | fr-FR | 1 | 67 MB | mid | — | — | MIT |
| Tjiho | fr-FR | 1 | 67 MB | mid | — | — | MIT |
| Tjiho | fr-FR | 1 | 67 MB | mid | — | — | MIT |
| Tom | fr-FR | 1 | 67 MB | mid | — | — | MIT |
| Tugao | pt-PT | 1 | 67 MB | mid | — | — | MIT |
| Ugla | is-IS | 1 | 67 MB | mid | — | — | MIT |
| Upc_ona | ca-ES | 1 | 67 MB | mid | — | — | MIT |
| Upc_ona | ca-ES | 1 | 27 MB | low | — | — | MIT |
| Upc_pau | ca-ES | 1 | 27 MB | low | — | — | MIT |
| Vais1000 | vi-VN | 1 | 67 MB | mid | — | — | MIT |
| Xiao_ya | zh-CN | 1 | 60 MB | mid | — | — | MIT |
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
| Baker | zh-CN | 1 | 75 MB | high | — | — | CC-BY-4.0 |
| En | zh-CN, en-US | 1 | 79 MB | high | — | — | CC-BY-4.0 |
| Ljspeech | en-US | 1 | 77 MB | high | — | — | CC-BY-4.0 |
| Speaker_0 | fa, en | 1 | 77 MB | high | — | — | CC-BY-4.0 |
| Speaker_0 | fa, en | 1 | 77 MB | high | — | — | CC-BY-4.0 |

## supertonic (2)

Newest (2026) multilingual model from Supertone. Single 100–200 MB bundle covering ~30 languages × 10 speakers.

| Title | Languages | Speakers | Size | Tier | Quality | RTF | License |
|---|---|---:|---:|---|---|---:|---|
| Speaker_0 | en | 1 | 85 MB | high | — | — | CC-BY-NC-4.0 |
| Supertonic (10 speakers) | 31 languages | 10 | 129 MB | high | — | — | CC-BY-NC-4.0 |

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
| Speaker_0 | en | 1 | 108 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | en | 1 | 108 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | en | 1 | 108 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | en | 1 | 108 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | en | 1 | 108 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | en | 1 | 108 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | en | 1 | 108 MB | mid | — | — | Apache-2.0 |
| Speaker_0 | en | 1 | 108 MB | mid | — | — | Apache-2.0 |
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
