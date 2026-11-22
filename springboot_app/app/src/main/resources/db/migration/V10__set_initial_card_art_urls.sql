-- Central artwork extracted from the original card fronts in Carte/.
-- Files are served from the frontend's public/img/cards/ directory.
WITH artwork_assets(card_code, card_name, filename) AS (
    VALUES
        ('CR01-001', 'Sentinel K-17', 'sentinel-k-17.webp'),
        ('CR01-002', 'Explorer of the ruins', 'explorer-of-the-ruins.webp'),
        ('CR01-003', 'Slaghound', 'slaghound.webp'),
        ('CR01-004', 'Orion’s Predator', 'orions-predator.webp'),
        ('CR01-005', 'Acolyte of the Fracture', 'acolyte-of-the-fracture.webp'),
        ('CR01-006', 'Pulse Blade', 'pulse-blade.webp'),
        ('CR01-007', 'Recovery Armor', 'recovery-armor.webp'),
        ('CR01-008', 'Scrap Runner', 'scrap-runner.webp'),
        ('CR01-009', 'Ashfang', 'ashfang.webp'),
        ('CR01-010', 'Bunker Lookout', 'bunker-lookout.webp'),
        ('CR01-011', 'Ash Novice', 'ash-novice.webp'),
        ('CR01-012', 'Void Scout', 'void-scout.webp'),
        ('CR01-013', 'Hydraulic Enforcer', 'hydraulic-enforcer.webp'),
        ('CR01-014', 'Cindermaw', 'cindermaw.webp'),
        ('CR01-015', 'Gauss Sharpshooter', 'gauss-sharpshooter.webp'),
        ('CR01-016', 'Eclipse Herald', 'eclipse-herald.webp'),
        ('CR01-017', 'Obsidian Harvester', 'obsidian-harvester.webp'),
        ('CR01-018', 'Siege Warden', 'siege-warden.webp'),
        ('CR01-019', 'Pier Nine Abomination', 'pier-nine-abomination.webp'),
        ('CR01-020', 'Ark Defender', 'ark-defender.webp'),
        ('CR01-021', 'Keeper of the Last Ember', 'keeper-of-the-last-ember.webp'),
        ('CR01-022', 'Quantum Stalker', 'quantum-stalker.webp'),
        ('CR01-023', 'Second Dawn Colossus', 'second-dawn-colossus.webp'),
        ('CR01-024', 'Fallout Behemoth', 'fallout-behemoth.webp'),
        ('CR01-025', 'Last Line Commander', 'last-line-commander.webp'),
        ('CR01-026', 'Voice Beyond the Rift', 'voice-beyond-the-rift.webp'),
        ('CR01-027', 'Harvest Overseer', 'harvest-overseer.webp'),
        ('CR01-028', 'Breach Capacitor', 'breach-capacitor.webp'),
        ('CR01-029', 'Overdrive Harness', 'overdrive-harness.webp'),
        ('CR01-030', 'Bastion Rig', 'bastion-rig.webp')
)
UPDATE cards_arts ca
SET url_card_art = '/img/cards/' || asset.filename
FROM artwork_assets asset
JOIN cards c
    ON c.name = asset.card_name
   AND c.code = asset.card_code
WHERE ca.id_card = c.id
  AND ca.art_code = 'default'
  AND ca.url_card_art IS DISTINCT FROM
      ('/img/cards/' || asset.filename);
