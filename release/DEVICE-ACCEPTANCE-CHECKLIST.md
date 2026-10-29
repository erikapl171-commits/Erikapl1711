# Stage 8 device acceptance, not yet executed

A test can only be marked PASS after being demonstrated on a physical Android device.

| ID | Procedure | Pass condition | Status |
|---|---|---|---|
| D01 | Assemble and install debug APK with pinned dependencies | App launches without crash | NOT RUN |
| D02 | Deny then grant microphone permission | No unauthorized capture, can start after permission | NOT RUN |
| D03 | Record uninterrupted Latvian speech for 30 minutes | Nonempty, accurate source transcript, no app failure | NOT RUN |
| D04 | Record uninterrupted Lithuanian speech for 30 minutes | Nonempty, accurate source transcript, no app failure | NOT RUN |
| D05 | Record both with deliberately no pauses | English updates during continuous speech | NOT RUN |
| D06 | Measure speech timestamp to stable English caption | p50 and p95 measured, no fabricated latency target | NOT RUN |
| D07 | Check source ASR accuracy against time-aligned references | WER and omission/duplication rates reported | NOT RUN |
| D08 | Judge translation quality with bilingual reviewer | Meaning retained across phrase boundaries | NOT RUN |
| D09 | Start/Stop 20 times, including while model initializes | No crash, no cross-session captions or mic leak | NOT RUN |
| D10 | Lock screen and background app | Recording and speech follow declared product policy | NOT RUN |
| D11 | Connect/disconnect headphones during playback | Route follows declared policy and no privacy surprise | NOT RUN |
| D12 | Disable Wi-Fi after initial model install | Offline processing demonstrated | NOT RUN |
| D13 | Recreate screen and rotate | Correct session status, no caption/audio duplication | NOT RUN |
| D14 | Exhaust audio backlog with fast continuous speech | No unbounded queue or silent unexplained skips | NOT RUN |
| D15 | Export transcript manually | No export without action, correct text in selected share target | NOT RUN |
| D16 | Test on actual user's phone under event noise | Acceptable battery, thermal, speed and intelligibility | NOT RUN |

## Release decision
BLOCKED until build, JNI linkage, ML Kit model, real-device scenarios, security and performance pass. Source-level tests alone are not sufficient.
