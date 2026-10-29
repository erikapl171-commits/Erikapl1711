"""Synthetic tests for metric correctness. NOT tests of live interpretation."""
import copy
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "benchmarks"))
from score import score, tokens, edit_distance, percentile


BASE = {
    "schema_version": 1, "device_model": "synthetic-device", "android_version": "test",
    "language": "lt", "asr_engine": "test", "asr_model": "test-model",
    "test_audio_id": "synthetic", "test_audio_sha256": "synthetic fixture only",
    "recording_duration_seconds": 30, "inference_busy_seconds": 9,
    "segments": [{"id": "s1", "speech_start_s": 0, "speech_end_s": 5,
        "reference_source_text": "Labas rytas visiems", "recognized_source_text": "Labas rytas visiems",
        "first_english_subtitle_at_s": 4, "stable_english_subtitle_at_s": 6},
        {"id": "s2", "speech_start_s": 6, "speech_end_s": 10,
        "reference_source_text": "Tai labai svarbu", "recognized_source_text": "Tai svarbu",
        "first_english_subtitle_at_s": 11, "stable_english_subtitle_at_s": 13}]
}


class BenchmarkScoreTests(unittest.TestCase):
    def test_exact_and_deletion_metrics(self):
        result = score(copy.deepcopy(BASE))
        self.assertAlmostEqual(result["word_error_rate"], 1/6)
        self.assertEqual(result["reference_words"], 6)
        self.assertAlmostEqual(result["first_subtitle_delay_p50_s"], 0.0)
        self.assertAlmostEqual(result["stable_subtitle_delay_p95_s"], 2.9)
        self.assertAlmostEqual(result["compute_real_time_factor"], .3)

    def test_diacritics_are_not_discarded(self):
        self.assertNotEqual(tokens("ąžuolas"), tokens("azuolas"))
        self.assertEqual(edit_distance(tokens("laba diena"), tokens("laba diena")), 0)

    def test_missing_translation_visible(self):
        data = copy.deepcopy(BASE)
        data["segments"][1]["first_english_subtitle_at_s"] = None
        data["segments"][1]["stable_english_subtitle_at_s"] = None
        result = score(data)
        self.assertEqual(result["first_subtitle_missing"], 1)
        self.assertEqual(result["stable_subtitle_missing"], 1)

    def test_blank_manifest_rejected(self):
        data = copy.deepcopy(BASE)
        data["segments"] = []
        with self.assertRaisesRegex(ValueError, "No measured segments"):
            score(data)

    def test_duplicate_segment_rejected(self):
        data = copy.deepcopy(BASE)
        data["segments"][1]["id"] = "s1"
        with self.assertRaisesRegex(ValueError, "unique"):
            score(data)

    def test_impossible_timing_rejected(self):
        data = copy.deepcopy(BASE)
        data["segments"][0]["stable_english_subtitle_at_s"] = 3
        with self.assertRaisesRegex(ValueError, "earlier/equal"):
            score(data)

    def test_infinite_numeric_timing_rejected(self):
        data = copy.deepcopy(BASE)
        data["segments"][0]["speech_end_s"] = float("inf")
        with self.assertRaisesRegex(ValueError, "finite number"):
            score(data)

    def test_missing_asr_text_counts_as_deletion(self):
        data = copy.deepcopy(BASE)
        data["segments"][0]["recognized_source_text"] = ""
        self.assertAlmostEqual(score(data)["word_error_rate"], 4/6)

    def test_percentile_empty(self):
        self.assertIsNone(percentile([], 95))


if __name__ == "__main__":
    unittest.main()
