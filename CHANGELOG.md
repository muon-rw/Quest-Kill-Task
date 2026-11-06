## 0.4.1
- Fix Iron's Spellbooks compat referencing an outdated version

## 0.4.0
- Clean up application logic a bit for better compatibility
- Fix kills not counting when the killing blow has no attacker (such as spells that deal part of their damage through an attacker-less damage source), even after players damaged the mob

## 0.3.1
- Add guard clause in case other mods somehow add null damage sources

## 0.3.0
- Added Heal contribution tracking for Spell Engine (Fabric) and Iron's Spellbooks (NeoForge)

## 0.2.0
- Fix an issue with entity death checks that could cause kill tasks to attempt to be processed on the client, and lead to frozen quest progress
- Fix an issue with task caching that could cause the kill task list to desync between the server and the client

# 0.1.0
- Initial Release