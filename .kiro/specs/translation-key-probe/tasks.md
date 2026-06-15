# Implementation Plan: Translation Key Probe

## Overview

This implementation plan converts the Translation Key Probe design into a series of incremental coding tasks. The approach follows a bottom-up strategy: build core data structures first (database, models), then implement packet handling components, integrate session management, and finally wire everything into the check framework. Each task builds on previous work to ensure continuous integration with no orphaned code.

## Tasks

- [ ] 1. Set up core data structures and interfaces
  - [x] 1.1 Implement KnownModDatabase class
    - Create `com.grimnatorac.checks.impl.exploit.TranslationKeyProbe.KnownModDatabase` class
    - Implement in-memory `Map<String, Set<String>>` for mod key registry
    - Add methods: `getKeysForMod()`, `getModIdForKey()`, `addModKey()`, `getAllKeys()`
    - Implement `loadFromConfig()` to read from YAML configuration
    - Initialize default mod keys (Meteor Client, Item Scroller, Freecam, Accurate Block Placement)
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7_
  
  - [ ]* 1.2 Write property test for database key addition round-trip
    - **Property 1: Database Key Addition Round-Trip**
    - **Validates: Requirements 1.6**
    - Verify that added keys can be retrieved correctly
  
  - [-] 1.3 Create ProbeSession data model
    - Create `ProbeSession` inner class with fields: `sessionId`, `playerUuid`, `startTimeMs`, `mechanism`, `keyToModId`, `receivedResponses`
    - Implement `isExpired(long timeoutMs)` method
    - Implement `recordResponse(String response)` method
    - _Requirements: 8.1, 3.7_
  
  - [x] 1.4 Create ClientFingerprint data model
    - Create `ClientFingerprint` static inner class with fields: `hash`, `resolvedModIds`, `firstDetectionMs`, `lastDetectionMs`
    - Implement static `compute(Set<String> modIds)` method using SHA256 hash
    - Sort mod IDs alphabetically before hashing for determinism
    - _Requirements: 6.1, 6.2_
  
  - [ ]* 1.5 Write property test for fingerprint determinism
    - **Property 19: Fingerprint Determinism**
    - **Validates: Requirements 6.1, 6.2**
    - Verify that same mod sets produce identical fingerprints

- [ ] 2. Implement resolution detection logic
  - [~] 2.1 Create ResolutionDetector class
    - Create `com.grimnatorac.checks.impl.exploit.TranslationKeyProbe.ResolutionDetector` class
    - Define `ResolutionResult` enum: `POSITIVE_RESOLUTION`, `NEGATIVE_RESOLUTION`, `INCONCLUSIVE`
    - Implement `normalizeText(String text)` method (lowercase, trim, collapse spaces, remove color codes)
    - _Requirements: 4.1, 4.2, 4.3_
  
  - [~] 2.2 Implement detectResolution method
    - Create `detectResolution(String originalKey, String clientResponse, int playerPing)` method
    - Compare normalized original key and normalized response
    - Return `POSITIVE_RESOLUTION` if different, `NEGATIVE_RESOLUTION` if same
    - Account for ping-based timeout adjustment
    - _Requirements: 4.1, 4.4, 4.5, 4.7, 5.7_
  
  - [ ]* 2.3 Write property tests for resolution detection
    - **Property 6: Positive Resolution Detection**
    - **Validates: Requirements 3.5, 4.1, 4.5**
    - **Property 9: Case-Insensitive Comparison**
    - **Validates: Requirements 4.2**
    - **Property 10: Whitespace-Insensitive Comparison**
    - **Validates: Requirements 4.3**
    - **Property 11: Negative Resolution Detection**
    - **Validates: Requirements 4.4**
  
  - [~] 2.4 Implement identifyMod method
    - Create `identifyMod(String resolvedKey)` method in ResolutionDetector
    - Query KnownModDatabase to find which mod owns the resolved key
    - Return Optional<String> with mod identifier
    - _Requirements: 4.5_
  
  - [ ]* 2.5 Write property test for partial resolution detection
    - **Property 12: Partial Resolution Detection**
    - **Validates: Requirements 4.6**
    - Verify that subset of keys resolving produces correct detection count

- [ ] 3. Implement probe packet generation
  - [~] 3.1 Create ProbePacketGenerator class
    - Create `com.grimnatorac.checks.impl.exploit.TranslationKeyProbe.ProbePacketGenerator` class
    - Define `ProbeDeliveryMechanism` enum: `SIGN_EDIT`, `ANVIL_RENAME`, `BOOK_EDIT`
    - Implement `selectRandomMechanism()` method using Random
    - _Requirements: 2.6_
  
  - [~] 3.2 Implement packet construction methods
    - Create `constructSignPacket(String translationKey)` method
    - Create `constructAnvilPacket(String translationKey)` method
    - Create `constructBookPacket(Set<String> translationKeys)` method
    - All methods must embed keys in JSON format: `{"translate": "key"}`
    - Use GrimnatorAC's packet wrapper utilities for packet construction
    - _Requirements: 2.2, 2.3, 2.4, 2.5_
  
  - [ ]* 3.3 Write property test for JSON probe packet format
    - **Property 3: JSON Probe Packet Format**
    - **Validates: Requirements 2.2**
    - Verify all generated packets contain correct JSON structure
  
  - [~] 3.4 Implement sendProbe method
    - Create `sendProbe(GrimPlayer player, ProbeSession session, Set<String> translationKeys)` method
    - Select random delivery mechanism
    - Construct appropriate packet with translation keys
    - Send packet via GrimnatorAC's packet sending API
    - Record sent keys in session's `keyToModId` map
    - _Requirements: 2.1, 2.6_
  
  - [ ]* 3.5 Write property test for delivery mechanism randomization
    - **Property 4: Delivery Mechanism Randomization**
    - **Validates: Requirements 2.6**
    - Verify uniform distribution across 100+ generations (chi-squared test)

- [~] 4. Checkpoint - Ensure core components compile
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 5. Implement response collection
  - [~] 5.1 Create ResponseCollector class
    - Create `com.grimnatorac.checks.impl.exploit.TranslationKeyProbe.ResponseCollector` class
    - Maintain `Map<UUID, ProbeSession>` for pending sessions indexed by session ID
    - Implement `registerPendingSession(ProbeSession session)` method
    - Implement `getSessionForPlayer(UUID playerUuid)` method
    - _Requirements: 3.1, 3.7_
  
  - [~] 5.2 Implement packet handler methods
    - Create `handleSignUpdate(UUID playerUuid, WrapperPlayClientUpdateSign packet)` method
    - Create `handleItemRename(UUID playerUuid, WrapperPlayClientRenameItem packet)` method
    - Create `handleBookEdit(UUID playerUuid, WrapperPlayClientEditBook packet)` method
    - Extract response text from each packet type
    - Find active session for player
    - Record response in session via `recordResponse()`
    - _Requirements: 3.2, 3.3, 3.4_
  
  - [ ]* 5.3 Write property test for response timeout window
    - **Property 7: Response Timeout Window**
    - **Validates: Requirements 3.6**
    - Verify responses outside timeout window are not associated
  
  - [ ]* 5.4 Write property test for session response correlation
    - **Property 8: Session Response Correlation**
    - **Validates: Requirements 3.7**
    - Verify responses for one session don't correlate with another

- [ ] 6. Implement session management
  - [~] 6.1 Create ProbeSessionManager class
    - Create `com.grimnatorac.checks.impl.exploit.TranslationKeyProbe.ProbeSessionManager` class
    - Maintain `Map<UUID, ProbeSession>` for active sessions
    - Maintain `Map<UUID, Long>` for last probe time (rate limiting)
    - Maintain `Queue<UUID>` for probe priority queue
    - _Requirements: 8.1, 8.2_
  
  - [~] 6.2 Implement session lifecycle methods
    - Create `startProbeSession(GrimPlayer player)` method
    - Check if player already has active session (return empty if yes)
    - Check rate limiting via `canProbe()` method
    - Select translation keys from KnownModDatabase
    - Create new ProbeSession with random UUID
    - Register session with ResponseCollector
    - Invoke ProbePacketGenerator to send probe
    - _Requirements: 2.1, 8.1, 8.2_
  
  - [~] 6.3 Implement scheduling and cleanup methods
    - Create `scheduleInitialProbe(GrimPlayer player, long delayMs)` method
    - Create `abortSession(UUID playerUuid)` method for player disconnect handling
    - Create `cleanupExpiredSessions()` method to remove timed-out sessions
    - Create `canProbe(GrimPlayer player)` method checking minimum interval
    - _Requirements: 8.3, 8.5, 8.6_
  
  - [ ]* 6.4 Write property tests for session management
    - **Property 5: Probe Rate Limiting**
    - **Validates: Requirements 2.7, 8.2**
    - **Property 28: Single Concurrent Session Per Player**
    - **Validates: Requirements 8.1**
    - **Property 30: Disconnect Session Abortion**
    - **Validates: Requirements 8.5**
  
  - [~] 6.4 Implement probe prioritization logic
    - Modify `startProbeSession()` to check player's first-join status
    - Prioritize new players in probe queue over returning players
    - Implement queue sorting by join time and player status
    - _Requirements: 8.4_
  
  - [ ]* 6.5 Write property test for new player prioritization
    - **Property 29: New Player Prioritization**
    - **Validates: Requirements 8.4**

- [ ] 7. Implement client fingerprinting
  - [~] 7.1 Create ClientFingerprintTracker class
    - Create `com.grimnatorac.checks.impl.exploit.TranslationKeyProbe.ClientFingerprintTracker` class
    - Maintain `Map<UUID, ClientFingerprint>` for in-memory fingerprints
    - Implement `recordFingerprint(UUID playerUuid, Set<String> detectedMods)` method
    - Implement `getFingerprint(UUID playerUuid)` method
    - Implement `hasFingerprintChanged(UUID playerUuid, Set<String> newMods)` method
    - _Requirements: 6.1, 6.4, 6.5_
  
  - [~] 7.2 Implement persistence methods
    - Create `loadFromPersistence()` method to read fingerprints.json file
    - Create `saveToPersistence()` method to write fingerprints.json file
    - Use JSON format: `{ "uuid": { "hash": "...", "resolvedModIds": [...], ... } }`
    - Handle file I/O errors gracefully (log and continue with in-memory only)
    - _Requirements: 6.3_
  
  - [ ]* 7.3 Write property tests for fingerprinting
    - **Property 20: Fingerprint UUID Association**
    - **Validates: Requirements 6.4**
    - **Property 21: Fingerprint Change Detection**
    - **Validates: Requirements 6.5**
  
  - [~] 7.4 Implement fingerprint blacklisting
    - Add `Set<String>` field for blacklisted fingerprint hashes
    - Implement `isBlacklisted(ClientFingerprint fingerprint)` method
    - Load blacklist from configuration on startup
    - _Requirements: 6.7_
  
  - [ ]* 7.5 Write property test for blacklist immediate detection
    - **Property 22: Blacklist Immediate Detection**
    - **Validates: Requirements 6.7**

- [~] 8. Checkpoint - Ensure all components integrate
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 9. Implement bypass detection logic
  - [~] 9.1 Add vanilla key support to KnownModDatabase
    - Add method `getVanillaKeys()` returning set of standard Minecraft translation keys
    - Include keys like "key.inventory", "key.forward", "key.back" for control probes
    - _Requirements: 7.1_
  
  - [~] 9.2 Modify ProbePacketGenerator to include vanilla keys
    - Update `sendProbe()` to always include at least one vanilla key in probe set
    - Ensure vanilla keys are randomly distributed across probe packets
    - _Requirements: 7.1_
  
  - [ ]* 9.3 Write property test for vanilla control probe inclusion
    - **Property 23: Vanilla Control Probe Inclusion**
    - **Validates: Requirements 7.1**
  
  - [~] 9.4 Implement bypass detection in ResolutionDetector
    - Add field `bypassSuspicionFlag` to ProbeSession
    - When vanilla key resolution fails, set bypass suspicion flag
    - Implement `computeBypassConfidence(ProbeSession session)` method
    - Score based on: vanilla key failures, selective resolution patterns, multiple probe waves
    - _Requirements: 7.2, 7.3, 7.4, 7.6_
  
  - [ ]* 9.5 Write property tests for bypass detection
    - **Property 24: Vanilla Key Anomaly Detection**
    - **Validates: Requirements 7.2**
    - **Property 25: Selective Resolution Bypass Detection**
    - **Validates: Requirements 7.4**
    - **Property 26: Bypass Confidence Scoring**
    - **Validates: Requirements 7.6**
  
  - [~] 9.6 Implement bypass alerting logic
    - Add configurable bypass confidence threshold
    - When confidence exceeds threshold, generate staff alert with bypass indicators
    - Include "[BYPASS-SUSPECTED]" tag in alert message
    - _Requirements: 7.7_
  
  - [ ]* 9.7 Write property test for confidence threshold alerting
    - **Property 27: Confidence Threshold Alerting**
    - **Validates: Requirements 7.7**

- [ ] 10. Implement main TranslationKeyProbeCheck class
  - [~] 10.1 Create TranslationKeyProbeCheck class with framework integration
    - Create `com.grimnatorac.checks.impl.exploit.TranslationKeyProbeCheck` class
    - Extend `Check` base class
    - Implement `PacketCheck` interface
    - Add `@CheckData` annotation with: name="TranslationKeyProbe", stableKey="grimnatorac.exploit.translation_key_probe", description, decay=0, setback=Integer.MAX_VALUE, experimental=false
    - Initialize all component instances: KnownModDatabase, ProbeSessionManager, ResponseCollector, ResolutionDetector, ClientFingerprintTracker
    - _Requirements: 10.1, 10.2, 10.3_
  
  - [~] 10.2 Implement configuration loading
    - Add config fields: `silentViolationThreshold`, `violationDecayIntervalMs`, `probeTimeoutMs`, `minProbeIntervalMs`
    - Implement `onReload(ConfigManager config)` method to load config from YAML
    - Set default values: silentViolationThreshold=3, minProbeIntervalMs=300000, probeTimeoutMs=10000, violationDecayIntervalMs=300000
    - Call `KnownModDatabase.loadFromConfig()` during reload
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 10.6_
  
  - [~] 10.3 Implement onPacketReceive method
    - Override `onPacketReceive(PacketReceiveEvent event)` from PacketCheck
    - Route C2S sign update packets to ResponseCollector.handleSignUpdate()
    - Route C2S item rename packets to ResponseCollector.handleItemRename()
    - Route C2S book edit packets to ResponseCollector.handleBookEdit()
    - _Requirements: 3.1, 3.2, 3.3, 3.4_
  
  - [~] 10.4 Implement violation detection and flagging
    - After response collection, correlate responses with probe sessions
    - Invoke ResolutionDetector.detectResolution() for each response
    - Accumulate positive resolutions per session
    - When positive resolutions ≥ 2, flag player for hacked client
    - Track silent violation count per player
    - When silent violations reach threshold, invoke `flagAndAlert()` with mod identifiers
    - _Requirements: 5.1, 5.2, 5.4, 10.4_
  
  - [ ]* 10.5 Write property tests for violation logic
    - **Property 13: Ping-Compensated Correlation**
    - **Validates: Requirements 4.7, 5.7**
    - **Property 14: Two-Key Flagging Threshold**
    - **Validates: Requirements 5.1**
    - **Property 15: Silent Violation Accumulation**
    - **Validates: Requirements 5.2**
    - **Property 16: Alert Content Completeness**
    - **Validates: Requirements 5.4**
  
  - [~] 10.6 Implement violation decay
    - Track last clean gameplay timestamp per player
    - In periodic check (via server tick event), compare current time with last violation time
    - If time since last violation ≥ decay interval, decrement violation count
    - _Requirements: 5.5_
  
  - [ ]* 10.7 Write property test for violation decay
    - **Property 17: Violation Decay Over Time**
    - **Validates: Requirements 5.5**
  
  - [~] 10.8 Implement game mode exemptions
    - Before initiating probe session, check player game mode
    - Skip probing if player is in CREATIVE or SPECTATOR mode
    - Do not record violations for exempted game modes
    - _Requirements: 5.6_
  
  - [ ]* 10.9 Write property test for game mode exemption
    - **Property 18: Gamemode Exemption**
    - **Validates: Requirements 5.6**
  
  - [~] 10.10 Implement debugging support
    - Respect `player.disableGrim` flag for debugging
    - Skip all probe sessions and violations for players with debugging enabled
    - _Requirements: 10.5_

- [ ] 11. Wire player lifecycle events
  - [~] 11.1 Implement player join handling
    - Listen for player join events in GrimnatorAC event system
    - Schedule initial probe session via `ProbeSessionManager.scheduleInitialProbe()` with 30-second delay
    - Load player fingerprint from ClientFingerprintTracker on join
    - _Requirements: 8.3_
  
  - [~] 11.2 Implement player disconnect handling
    - Listen for player disconnect events
    - Abort active probe sessions via `ProbeSessionManager.abortSession()`
    - Save player fingerprint to persistence if updated
    - _Requirements: 8.5_
  
  - [~] 11.3 Implement periodic cleanup task
    - Register scheduled task to run `ProbeSessionManager.cleanupExpiredSessions()` every 30 seconds
    - Invoke `ClientFingerprintTracker.saveToPersistence()` every 5 minutes
    - _Requirements: 8.6_

- [ ] 12. Implement global rate limiting
  - [~] 12.1 Add global probe counter
    - Add `AtomicInteger` field to track probes sent in current time window
    - Add sliding window tracker (e.g., last 60 seconds)
    - Implement `checkGlobalRateLimit()` method returning boolean
    - _Requirements: 8.7_
  
  - [~] 12.2 Integrate rate limiting into sendProbe
    - Before sending probe packet, check global rate limit
    - If limit exceeded, defer probe to next available window
    - Log rate limit hits for monitoring
    - _Requirements: 8.7_
  
  - [ ]* 12.3 Write property test for global rate limit enforcement
    - **Property 32: Global Rate Limit Enforcement**
    - **Validates: Requirements 8.7**

- [~] 13. Final checkpoint - Integration testing
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 14. Add configuration file and documentation
  - [~] 14.1 Create YAML configuration template
    - Create default configuration section in GrimnatorAC's config.yml
    - Add all tunable parameters with comments explaining each value
    - Include Known_Mod_Database entries with example mod keys
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7_
  
  - [~] 14.2 Write inline code documentation
    - Add Javadoc comments to all public methods
    - Document component responsibilities
    - Add usage examples in class-level Javadoc
    - Document error handling behavior

- [ ] 15. End-to-end integration tests
  - [ ]* 15.1 Write integration test for sign delivery mechanism
    - Simulate full workflow: generate probe → send sign packet → handle sign update → verify detection
    - _Requirements: 2.3, 3.2_
  
  - [ ]* 15.2 Write integration test for anvil delivery mechanism
    - Simulate full workflow: generate probe → send anvil packet → handle rename → verify detection
    - _Requirements: 2.4, 3.3_
  
  - [ ]* 15.3 Write integration test for book delivery mechanism
    - Simulate full workflow: generate probe → send book packet → handle edit → verify detection
    - _Requirements: 2.5, 3.4_
  
  - [ ]* 15.4 Write integration test for multi-player concurrent sessions
    - Start probe sessions for 10 concurrent players
    - Verify sessions remain isolated
    - Verify no response cross-contamination
    - _Requirements: 8.1, 3.7_
  
  - [ ]* 15.5 Write integration test for bypass detection
    - Send probe with vanilla + mod keys
    - Simulate client resolving only vanilla keys
    - Verify bypass suspicion flag is set and alert fires
    - _Requirements: 7.1, 7.2, 7.4, 7.7_

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP delivery
- All property-based tests should use jqwik framework with minimum 100 iterations
- Each task explicitly references requirements for full traceability
- Checkpoints ensure incremental validation and provide opportunities for user feedback
- Property tests validate universal correctness properties from design document
- Unit tests and integration tests complement property tests for comprehensive coverage
- All code must follow existing GrimnatorAC code style and package structure
- Component classes are nested within TranslationKeyProbeCheck for encapsulation
- Error handling follows graceful degradation pattern (log, fallback, continue)

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.3", "1.4"] },
    { "id": 1, "tasks": ["1.2", "1.5", "2.1", "3.1"] },
    { "id": 2, "tasks": ["2.2", "3.2"] },
    { "id": 3, "tasks": ["2.3", "2.4", "3.3", "3.4"] },
    { "id": 4, "tasks": ["2.5", "3.5", "5.1"] },
    { "id": 5, "tasks": ["5.2", "6.1"] },
    { "id": 6, "tasks": ["5.3", "5.4", "6.2"] },
    { "id": 7, "tasks": ["6.3", "6.4", "7.1"] },
    { "id": 8, "tasks": ["6.5", "7.2"] },
    { "id": 9, "tasks": ["7.3", "7.4", "9.1"] },
    { "id": 10, "tasks": ["7.5", "9.2"] },
    { "id": 11, "tasks": ["9.3", "9.4"] },
    { "id": 12, "tasks": ["9.5", "9.6"] },
    { "id": 13, "tasks": ["9.7", "10.1"] },
    { "id": 14, "tasks": ["10.2", "10.3"] },
    { "id": 15, "tasks": ["10.4"] },
    { "id": 16, "tasks": ["10.5", "10.6"] },
    { "id": 17, "tasks": ["10.7", "10.8"] },
    { "id": 18, "tasks": ["10.9", "10.10", "11.1"] },
    { "id": 19, "tasks": ["11.2", "11.3", "12.1"] },
    { "id": 20, "tasks": ["12.2"] },
    { "id": 21, "tasks": ["12.3", "14.1"] },
    { "id": 22, "tasks": ["14.2", "15.1"] },
    { "id": 23, "tasks": ["15.2", "15.3"] },
    { "id": 24, "tasks": ["15.4", "15.5"] }
  ]
}
```
