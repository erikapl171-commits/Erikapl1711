#!/usr/bin/env python3
"""Score measured Android speech-to-subtitle benchmark results.

No transcription/translation/model inference is performed here.
Input must be actual captured measurements, never projected performance.
"""
import argparse
import json
import math
import re
import statistics
from pathlib import Path


def tokens(text):
    return re.findall(r"\w+", text.casefold(), flags=re.UNICODE)


def edit_distance(a, b):
    prev = list(range(len(b) + 1))
    for i, av in enumerate(a, 1):
        cur = [i]
        for j, bv in enumerate(b, 1):
            cur.append(min(cur[-1] + 1, prev[j] + 1, prev[j - 1] + (av != bv)))
        prev = cur
    return prev[-1]


def percentile(values, percent):
    if not values:
        return None
    values = sorted(values)
    at = (len(values) - 1) * percent / 100.0
    low, high = math.floor(at), math.ceil(at)
    return values[low] + (values[high] - values[low]) * (at - low)


def require_number(value, name):
    if not isinstance(value, (int, float)) or isinstance(value, bool) or not math.isfinite(value):
        raise ValueError(f"{name} must be a finite number")
    return float(value)


def score(data):
    if data.get("schema_version") != 1:
        raise ValueError("schema_version must be 1")
    for key in ("device_model", "android_version", "language", "asr_engine", "asr_model", "test_audio_id", "test_audio_sha256"):
        if not isinstance(data.get(key), str) or not data[key].strip():
            raise ValueError(f"{key} must be a nonempty string")
    if data["language"] not in ("lt", "lv", "en"):
        raise ValueError("language must be lt, lv or en for the initial benchmark")
    if not isinstance(data.get("segments"), list) or not data["segments"]:
        raise ValueError("No measured segments. Populate the manifest from a real device; do not score the template.")
    length = require_number(data.get("recording_duration_seconds"), "recording_duration_seconds")
    if length <= 0:
        raise ValueError("recording_duration_seconds must be > 0")
    busy = data.get("inference_busy_seconds")
    if busy is not None:
        busy = require_number(busy, "inference_busy_seconds")
        if busy < 0:
            raise ValueError("inference_busy_seconds must be >= 0")
    seen = set()
    token_total = errors = 0
    first_delays, stable_delays = [], []
    missing_first = missing_stable = 0
    for i, seg in enumerate(data["segments"]):
        if not isinstance(seg, dict):
            raise ValueError(f"segments[{i}] must be an object")
        sid = seg.get("id")
        if not isinstance(sid, str) or not sid.strip() or sid in seen:
            raise ValueError(f"segments[{i}].id must be unique and nonempty")
        seen.add(sid)
        begin = require_number(seg.get("speech_start_s"), f"segments[{i}].speech_start_s")
        end = require_number(seg.get("speech_end_s"), f"segments[{i}].speech_end_s")
        if not (0 <= begin < end <= length):
            raise ValueError(f"segments[{i}] must have 0 <= start < end <= recording duration")
        ref = seg.get("reference_source_text")
        hyp = seg.get("recognized_source_text")
        if not isinstance(ref, str) or not ref.strip() or not isinstance(hyp, str):
            raise ValueError(f"segments[{i}] requires nonempty reference and string hypothesis")
        a = tokens(ref)
        b = tokens(hyp)
        if not a:
            raise ValueError(f"segments[{i}] reference contains no words")
        token_total += len(a)
        errors += edit_distance(a, b)
        first = seg.get("first_english_subtitle_at_s")
        stable = seg.get("stable_english_subtitle_at_s")
        if first is None:
            missing_first += 1
        else:
            first = require_number(first, f"segments[{i}].first_english_subtitle_at_s")
            if first < begin:
                raise ValueError(f"segments[{i}] first subtitle cannot precede speech start")
            first_delays.append(first - end)  # negative means subtitle appeared before phrase ended
        if stable is None:
            missing_stable += 1
        else:
            stable = require_number(stable, f"segments[{i}].stable_english_subtitle_at_s")
            if first is None or stable < first:
                raise ValueError(f"segments[{i}] stable subtitle requires an earlier/equal first subtitle")
            stable_delays.append(stable - end)
    return {
        "device": data["device_model"], "language": data["language"],
        "model": data["asr_model"], "segment_count": len(data["segments"]),
        "word_error_rate": errors / token_total,
        "reference_words": token_total,
        "first_subtitle_missing": missing_first,
        "stable_subtitle_missing": missing_stable,
        "first_subtitle_delay_p50_s": percentile(first_delays, 50),
        "first_subtitle_delay_p95_s": percentile(first_delays, 95),
        "stable_subtitle_delay_p50_s": percentile(stable_delays, 50),
        "stable_subtitle_delay_p95_s": percentile(stable_delays, 95),
        "compute_real_time_factor": None if busy is None else busy / length,
        "note": "Human review of translation meaning, long-run backlog, battery and thermals is still required."
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("manifest", type=Path)
    args = parser.parse_args()
    try:
        print(json.dumps(score(json.loads(args.manifest.read_text(encoding="utf-8"))), ensure_ascii=False, indent=2))
    except (ValueError, KeyError, TypeError, json.JSONDecodeError) as exc:
        parser.exit(2, f"Benchmark rejected: {exc}\n")


if __name__ == "__main__":
    main()
