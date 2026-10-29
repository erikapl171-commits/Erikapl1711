# Phone feasibility testing protocol

The scoring utility is a preparation tool. It does not generate ASR or translation, and testing it does not establish Android feasibility.

## Test recordings

Collect consented, representative speech in Lithuanian (`lt`) and Latvian (`lv`), including:

1. Clear speech, 2 minutes without artificial gaps (each language).
2. Fast conference talk, 10 minutes without meaningful pauses (each language).
3. 30-minute continuous recording with real room acoustics (one language initially).
4. Accents, proper names, numbers, code-switching, crowd noise and interruptions.
5. At least one unrelated recording for model regression after optimization.

Keep the original human transcript and a human-written English meaning reference separate from generated output. Use reference audio only with proper permission. Do not bundle private speaker recordings in public source archives.

## Candidate comparison

- **Primary:** multilingual whisper.cpp `tiny` vs `base` (and quantized equivalents if verified), transcription task in the chosen source language, followed by ML Kit local translation to English.
- **Alternative:** direct Whisper speech-to-English translation using a multilingual model. The `turbo` model is not suited for the Whisper translation task. Compare its actual English translation quality/latency against the two-step method before selecting.
- **If too slow:** test a language-appropriate streaming ASR candidate only after verifying its precise model's `lt`/`lv` coverage. Another fallback is English-only demonstration, clearly labeled as not satisfying the Latvian/Lithuanian MVP.

## Timings to record

For each human-aligned speech phrase: original speech start and end, first visible translated subtitle time, stable translated subtitle time. Store actual ASR source-language output, and device/model metadata. Compute time against the same monotonic clock; reset timestamps to recording start.

The metric `first_english_subtitle_at_s - speech_end_s` can be negative for a correct provisional translation visible before the full phrase has finished. The evaluator preserves this property rather than assuming silence-separated phrases.

Measure average inference busy time and track per-minute backlog, CPU/memory usage, battery, thermal conditions, and voice playback lag manually. An overall RTF under 1 is necessary but not sufficient to guarantee interactive performance, particularly with overlapped windows.

## Proposed provisional UX targets (not verified)

- First English subtitle while speech is ongoing, not dependent on a silence boundary.
- On a representative device, aim for first-subtitle p50 <= 3 seconds and p95 <= 6 seconds measured relative to the human-aligned phrase end, while also inspecting how many updates happen mid-phrase.
- 10 minutes of uninterrupted input with no increasing processing backlog; test 60-minute endurance before labeling event-ready.
- No repeated or dropped phrases on boundary overlap; no permanently rewritten committed caption.
- English interpretation judged by a bilingual human for meaning, numbers, names and negation. Record explicit misses. WER alone does not establish translation quality.
- Airplane-mode test must pass after all model/voice downloads, with network-only voices excluded.
- Battery and thermal impact measured on physical hardware; no unverified promises.

Thresholds are initial engineering targets rather than guarantees. If the quality/latency tradeoff cannot meet them, document the failure and the cost-free fallback rather than making up performance claims.

## Input format

Fill `manifest.example.json` as a new file with real identifiers, actual SHA-256 of the reference audio, duration, model, and one record per human-aligned speech phrase:

```json
{
  "id": "lt-0001",
  "speech_start_s": 0.0,
  "speech_end_s": 3.9,
  "reference_source_text": "Human-verified original-language words",
  "recognized_source_text": "Actual ASR output",
  "first_english_subtitle_at_s": 3.0,
  "stable_english_subtitle_at_s": 5.1
}
```

The values shown are *schema illustrations, not measurements*. Unknown subtitle times should be `null` to count missing outputs. Scorer rejects unpopulated manifests and impossible timestamps.

Run `python3 benchmarks/score.py my-measured-run.json` from the project root.
