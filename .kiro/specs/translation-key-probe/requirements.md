# Requirements Document

## Introduction

This document specifies the requirements for implementing Translation Key Resolution Probing exploit detection in the GrimnatorAC anti-cheat system. The feature exploits Minecraft's client-side localization mechanism to detect hacked clients (Meteor Client, Wurst, Item Scroller, Freecam, etc.) by probing whether the client can resolve translation keys associated with known mods.

The exploit mechanism works by sending translation keys embedded in GUI packets (signs, anvils, books) and observing whether the client echoes back resolved text instead of the original key. When a hacked client has a mod installed, its localization engine resolves mod-specific keys; legitimate vanilla clients return the keys unchanged.

## Glossary

- **Translation_Key_Probe_Check**: The anti-cheat check that detects hacked clients via translation key resolution probing
- **Probe_Manager**: The component responsible for managing probe sessions and tracking client responses
- **Translation_Key**: A localization string identifier (e.g., "key.meteor-client.open-gui") used in Minecraft's i18n system
- **Probe_Packet**: A server-to-client packet containing translation keys designed to trigger client-side resolution
- **Resolution_Response**: The text echoed back by the client after receiving a probe packet
- **Client_Fingerprint**: A unique identifier derived from the set of translation keys a client successfully resolves
- **Probe_Session**: A time-bounded period during which the server sends probe packets and collects client responses
- **Known_Mod_Database**: A registry of translation keys associated with known hacked clients and mods
- **Resource_Pack_Bypass**: An attempt by the client to avoid detection by overriding mod translation keys with resource packs

## Requirements

### Requirement 1: Translation Key Database Management

**User Story:** As a server administrator, I want the system to maintain a database of known mod translation keys, so that the anti-cheat can probe for specific hacked clients.

#### Acceptance Criteria

1. THE Known_Mod_Database SHALL store translation keys mapped to mod identifiers
2. THE Known_Mod_Database SHALL include keys for Meteor Client ("key.meteor-client.open-gui")
3. THE Known_Mod_Database SHALL include keys for Item Scroller ("itemscroller.gui.button.config_gui.generic")
4. THE Known_Mod_Database SHALL include keys for Freecam ("key.freecam.toggle")
5. THE Known_Mod_Database SHALL include keys for Accurate Block Placement ("text.autoconfig.accurateblockplacement.title")
6. THE Known_Mod_Database SHALL support runtime addition of new translation keys
7. THE Known_Mod_Database SHALL support hot-reload from configuration files

### Requirement 2: Probe Packet Generation

**User Story:** As a detection system, I want to generate probe packets containing translation keys, so that I can test whether the client resolves mod-specific localization strings.

#### Acceptance Criteria

1. WHEN initiating a probe session, THE Probe_Manager SHALL select translation keys from the Known_Mod_Database
2. THE Probe_Manager SHALL embed translation keys in GUI packets using the JSON text format {"translate": "key"}
3. THE Probe_Manager SHALL support sign edit packets as a probe delivery mechanism
4. THE Probe_Manager SHALL support anvil rename packets as a probe delivery mechanism
5. THE Probe_Manager SHALL support book text packets as a probe delivery mechanism
6. WHEN generating probe packets, THE Probe_Manager SHALL randomize the delivery mechanism to avoid pattern detection
7. THE Probe_Manager SHALL limit probe frequency to avoid detection by hacked clients

### Requirement 3: Client Response Collection

**User Story:** As a detection system, I want to capture client responses to probe packets, so that I can determine whether translation keys were resolved.

#### Acceptance Criteria

1. WHEN a client receives a probe packet, THE Translation_Key_Probe_Check SHALL monitor subsequent C2S packets for resolution responses
2. THE Translation_Key_Probe_Check SHALL capture sign update packets (C2SUpdateSignPacket) as potential responses
3. THE Translation_Key_Probe_Check SHALL capture item rename packets (C2SRenameItemPacket) as potential responses
4. THE Translation_Key_Probe_Check SHALL capture book edit packets (C2SEditBookPacket) as potential responses
5. WHEN a client echoes resolved text instead of the original key, THE Translation_Key_Probe_Check SHALL record a positive resolution
6. THE Translation_Key_Probe_Check SHALL implement a timeout window for collecting responses after probe delivery
7. THE Translation_Key_Probe_Check SHALL associate responses with specific probe sessions using correlation identifiers

### Requirement 4: Translation Key Resolution Detection

**User Story:** As a detection system, I want to determine whether a client successfully resolved a translation key, so that I can identify the presence of specific mods.

#### Acceptance Criteria

1. WHEN comparing probe input and client response, THE Translation_Key_Probe_Check SHALL detect if the response differs from the original key
2. THE Translation_Key_Probe_Check SHALL ignore case sensitivity when comparing keys and responses
3. THE Translation_Key_Probe_Check SHALL ignore whitespace variations when comparing keys and responses
4. WHEN a response matches the original key exactly, THE Translation_Key_Probe_Check SHALL record a negative resolution
5. WHEN a response contains translated text, THE Translation_Key_Probe_Check SHALL record a positive resolution with the mod identifier
6. THE Translation_Key_Probe_Check SHALL handle partial resolutions where only some keys are resolved
7. THE Translation_Key_Probe_Check SHALL account for network latency when correlating probes and responses

### Requirement 5: Hacked Client Detection Logic

**User Story:** As a server administrator, I want the system to flag players using hacked clients, so that I can take appropriate moderation action.

#### Acceptance Criteria

1. WHEN a client resolves two or more mod-specific translation keys, THE Translation_Key_Probe_Check SHALL flag the player for using a hacked client
2. THE Translation_Key_Probe_Check SHALL accumulate silent violations before alerting staff
3. THE Translation_Key_Probe_Check SHALL implement a configurable threshold for staff alerts (default 3 positive resolutions)
4. WHEN flagging a player, THE Translation_Key_Probe_Check SHALL include the detected mod identifiers in the alert
5. THE Translation_Key_Probe_Check SHALL implement violation decay for clean gameplay periods
6. THE Translation_Key_Probe_Check SHALL exempt creative mode and spectator mode players from detection
7. THE Translation_Key_Probe_Check SHALL implement ping compensation to avoid false positives from high-latency connections

### Requirement 6: Client Fingerprinting

**User Story:** As a server administrator, I want the system to generate unique fingerprints for detected clients, so that I can track players across sessions and identify specific mod configurations.

#### Acceptance Criteria

1. WHEN a client resolves translation keys, THE Translation_Key_Probe_Check SHALL generate a Client_Fingerprint from the set of resolved keys
2. THE Client_Fingerprint SHALL be computed as a hash of resolved mod identifiers sorted alphabetically
3. THE Translation_Key_Probe_Check SHALL store Client_Fingerprints persistently across server restarts
4. THE Translation_Key_Probe_Check SHALL associate Client_Fingerprints with player UUIDs
5. WHEN a player reconnects with a different Client_Fingerprint, THE Translation_Key_Probe_Check SHALL alert staff to potential mod configuration changes
6. THE Translation_Key_Probe_Check SHALL provide an API to query Client_Fingerprints by player UUID
7. THE Translation_Key_Probe_Check SHALL support fingerprint blacklisting for known malicious configurations

### Requirement 7: Resource Pack Bypass Detection

**User Story:** As a detection system, I want to detect attempts to bypass probing via resource packs, so that sophisticated cheaters cannot evade detection.

#### Acceptance Criteria

1. WHEN initiating a probe session, THE Probe_Manager SHALL send control probes with vanilla translation keys
2. WHEN a client fails to resolve vanilla keys correctly, THE Translation_Key_Probe_Check SHALL suspect resource pack interference
3. THE Translation_Key_Probe_Check SHALL send multiple probe waves with different key combinations to detect resource pack patterns
4. WHEN a client selectively resolves only vanilla keys but not mod keys, THE Translation_Key_Probe_Check SHALL flag potential bypass behavior
5. THE Translation_Key_Probe_Check SHALL compare resolution patterns across multiple probe sessions to identify resource pack manipulation
6. THE Translation_Key_Probe_Check SHALL implement a confidence score for resource pack bypass detection
7. WHEN bypass confidence exceeds a configurable threshold, THE Translation_Key_Probe_Check SHALL alert staff with bypass indicators

### Requirement 8: Probe Session Management

**User Story:** As a detection system, I want to manage probe sessions efficiently, so that probing does not degrade server performance or player experience.

#### Acceptance Criteria

1. THE Probe_Manager SHALL limit concurrent probe sessions to one per player
2. THE Probe_Manager SHALL implement a minimum interval between probe sessions (default 5 minutes)
3. WHEN a player joins the server, THE Probe_Manager SHALL schedule an initial probe session after a delay (default 30 seconds)
4. THE Probe_Manager SHALL prioritize probing new players over returning players
5. THE Probe_Manager SHALL abort probe sessions when players disconnect
6. THE Probe_Manager SHALL clean up expired probe sessions to prevent memory leaks
7. THE Probe_Manager SHALL implement a configurable global rate limit for probe packet generation

### Requirement 9: Configuration and Tunability

**User Story:** As a server administrator, I want to configure probe detection parameters, so that I can tune the system for my server's specific needs.

#### Acceptance Criteria

1. THE Translation_Key_Probe_Check SHALL support configuration of the silent violation threshold
2. THE Translation_Key_Probe_Check SHALL support configuration of the violation decay interval
3. THE Translation_Key_Probe_Check SHALL support configuration of the probe timeout window
4. THE Translation_Key_Probe_Check SHALL support configuration of the minimum probe session interval
5. THE Translation_Key_Probe_Check SHALL support configuration of the alert confidence threshold
6. THE Translation_Key_Probe_Check SHALL support hot-reload of all configuration parameters
7. THE Translation_Key_Probe_Check SHALL provide default configuration values that work for typical servers

### Requirement 10: Integration with Existing Anti-Cheat Infrastructure

**User Story:** As a developer, I want the probe check to integrate seamlessly with the existing GrimnatorAC check framework, so that it follows established patterns and conventions.

#### Acceptance Criteria

1. THE Translation_Key_Probe_Check SHALL extend the Check base class
2. THE Translation_Key_Probe_Check SHALL implement the PacketCheck interface
3. THE Translation_Key_Probe_Check SHALL use the CheckData annotation with appropriate metadata
4. THE Translation_Key_Probe_Check SHALL invoke flagAndAlert() when violations reach the threshold
5. THE Translation_Key_Probe_Check SHALL respect player.disableGrim for debugging
6. THE Translation_Key_Probe_Check SHALL implement onReload() for configuration hot-reload
7. THE Translation_Key_Probe_Check SHALL follow the same violation accumulation pattern as TriggerBot and AutoAnchor checks

