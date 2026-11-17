-- Snapshot of cinder-rift-cards.csv: only the original CR01-001 through CR01-030 cards.
-- Keep database-generated numeric IDs; preserve the editorial IDs separately.
ALTER TABLE cards
    ADD COLUMN code varchar UNIQUE,
    ADD COLUMN transfer_and_discard varchar,
    ADD COLUMN duration varchar,
    ADD COLUMN flavor_text varchar;

-- The CSV contains filenames, not hosted artwork URLs.
ALTER TABLE cards_arts ADD COLUMN source_image varchar;

INSERT INTO expansions (code, name, is_active)
VALUES ('CR01', 'Set Dimostrativo', true)
ON CONFLICT (code) DO NOTHING;

INSERT INTO factions (code, name)
VALUES ('NEUTRAL', 'Neutral')
ON CONFLICT (code) DO NOTHING;

CREATE TEMPORARY TABLE initial_set_cards (
    code varchar PRIMARY KEY,
    name varchar NOT NULL,
    type_code varchar NOT NULL,
    faction_name varchar NOT NULL,
    rarity_code varchar NOT NULL,
    energy_cost integer,
    power integer,
    value_atk integer,
    value_def integer,
    description varchar,
    transfer_and_discard varchar,
    duration varchar,
    flavor_text varchar,
    source_image varchar
) ON COMMIT DROP;

INSERT INTO initial_set_cards VALUES
    ('CR01-001', 'Sentinel K-17', 'CREATURE', 'Cyborg', 'COMMON', '1', '2', '3', '5', NULL, NULL, NULL, 'The city has fallen. His orders to stand guard have not.', 'Carta Sentinel K-17_ spada e cuore-4.png'),
    ('CR01-002', 'Explorer of the ruins', 'CREATURE', 'Sapiens', 'COMMON', '1', '2', '3', '5', NULL, NULL, NULL, 'Every ruin holds a way out.', 'Esploratore delle rovine-1.png'),
    ('CR01-003', 'Slaghound', 'CREATURE', 'Radioactive monsters', 'COMMON', '1', '2', '4', '4', NULL, NULL, NULL, 'Where the earth burns, he finds a trail.', 'Icone metalliche d’argento_ spada e cuore.png'),
    ('CR01-004', 'Orion’s Predator', 'CREATURE', 'Aliens', 'COMMON', '1', '2', '5', '3', NULL, NULL, NULL, 'For him, this world has only just begun.', 'Carta Orion’s Predator_ spada e cuore-2.png'),
    ('CR01-005', 'Acolyte of the Fracture', 'CREATURE', 'Cultists', 'COMMON', '1', '2', '4', '4', NULL, NULL, NULL, 'In the silence of the Fracture, someone responds.', 'Acolito della Frattura, forza e cuore-3.png'),
    ('CR01-006', 'Pulse Blade', 'EQUIPMENT', 'Neutral', 'COMMON', NULL, '1', NULL, NULL, 'The equipped creature gains +1 attack.', 'Transfers to the next creature. Discard at the end of the round.', 'ROUND', 'A fragment of technology. A tangible advantage.', 'Carta collezionabile dimostrativa-7.png'),
    ('CR01-007', 'Recovery Armor', 'EQUIPMENT', 'Neutral', 'COMMON', NULL, '1', NULL, NULL, 'Each creature enters with +2 starting defense. Does not heal the active creature.', 'Transfers to the next creature. Discard at the end of the round.', 'ROUND', 'Nothing is lost as long as it can protect you.', 'Carta collezionabile con set dimostrativo-5.png'),
    ('CR01-008', 'Scrap Runner', 'CREATURE', 'Cyborg', 'COMMON', '1', '2', '2', '3', NULL, NULL, NULL, 'It carries spare parts through streets where nothing living dares to run.', 'Corriere cibernetico tra le rovine-1.png'),
    ('CR01-009', 'Ashfang', 'CREATURE', 'Radioactive monsters', 'COMMON', '1', '2', '3', '2', NULL, NULL, NULL, 'It learned to hunt by following the warmth beneath the ash.', 'Ashfang, predatore delle ceneri-1.png'),
    ('CR01-010', 'Bunker Lookout', 'CREATURE', 'Sapiens', 'COMMON', '1', '2', '2', '3', NULL, NULL, NULL, 'Beyond the hatch lies a dead city. Someone still has to watch it.', 'Sentinella del bunker all’alba-1.png'),
    ('CR01-011', 'Ash Novice', 'CREATURE', 'Cultists', 'COMMON', '1', '2', '3', '2', NULL, NULL, NULL, 'His first prayer was a cough. The voices answered anyway.', 'Novizio della cenere-1.png'),
    ('CR01-012', 'Void Scout', 'CREATURE', 'Aliens', 'COMMON', '1', '2', '3', '2', NULL, NULL, NULL, 'It crossed a dying universe to find a world that had only begun to bleed.', 'Esploratore alieno nelle rovine-1.png'),
    ('CR01-013', 'Hydraulic Enforcer', 'CREATURE', 'Cyborg', 'COMMON', '3', '2', '4', '6', NULL, NULL, NULL, 'Its orders are older than the ruins. Its fists still enforce them.', 'Enforcer idraulico tra rovine industriali-3.png'),
    ('CR01-014', 'Cindermaw', 'CREATURE', 'Radioactive monsters', 'RARE', '3', '3', '7', '4', NULL, NULL, NULL, 'Every meal feeds the mutation. The hunger never changes.', 'Cindermaw, mutante del canale tossico-2.png'),
    ('CR01-015', 'Gauss Sharpshooter', 'CREATURE', 'Sapiens', 'COMMON', '3', '2', '6', '4', NULL, NULL, NULL, 'One clean shot costs less Cobalt than a funeral.', 'Cecchina Gauss tra le rovine-2.png'),
    ('CR01-016', 'Eclipse Herald', 'CREATURE', 'Cultists', 'EPIC', '3', '4', '5', '7', NULL, NULL, NULL, 'She remembers the three minuttes when the sun went dark. She speaks as though they never ended.', 'Profetessa dell’eclissi viola-2.png'),
    ('CR01-017', 'Obsidian Harvester', 'CREATURE', 'Aliens', 'COMMON', '3', '2', '5', '5', NULL, NULL, NULL, 'It does not recognize borders. Only deposits.', 'Mietitore d_ossidiana-3.png'),
    ('CR01-018', 'Siege Warden', 'CREATURE', 'Cyborg', 'EPIC', '4', '4', '5', '9', NULL, NULL, NULL, 'Built to seal the reactor gates, it now decides who deserves to pass.', 'Guardiano corazzato del reattore-4.png'),
    ('CR01-019', 'Pier Nine Abomination', 'CREATURE', 'Radioactive monsters', 'RARE', '4', '3', '8', '5', NULL, NULL, NULL, 'The evacuation records list no survivors. The docks tell a different story.', 'Abominio radioattivo del Molo Nove-3.png'),
    ('CR01-020', 'Ark Defender', 'CREATURE', 'Sapiens', 'COMMON', '4', '2', '4', '8', NULL, NULL, NULL, 'The armor is patched. The promise behind it is not.', 'Difensore dell_Arca-4.png'),
    ('CR01-021', 'Keeper of the Last Ember', 'CREATURE', 'Cultists', 'COMMON', '4', '2', '6', '6', NULL, NULL, NULL, 'When the last brazier dies, he believes the gods will stop listening.', 'Custode dell’ultima brace-4.png'),
    ('CR01-022', 'Quantum Stalker', 'CREATURE', 'Aliens', 'RARE', '4', '3', '7', '6', NULL, NULL, NULL, 'Its reflection reaches the ruins a moment before its body.', 'Cacciatore quantico nelle rovine-4.png'),
    ('CR01-023', 'Second Dawn Colossus', 'CREATURE', 'Cyborg', 'LEGENDARY', '5', '5', '8', '8', NULL, NULL, NULL, 'The Consortium promised a second dawn. Something beneath the city still intends to deliver it.', 'Colosso cibernetico della Seconda Alba-2.png'),
    ('CR01-024', 'Fallout Behemoth', 'CREATURE', 'Radioactive monsters', 'RARE', '5', '2', '9', '5', NULL, NULL, NULL, 'The ground does not tremble before it. It breaks.', 'Behemoth radioattivo tra rovine industriali-4.png'),
    ('CR01-025', 'Last Line Commander', 'CREATURE', 'Sapiens', 'COMMON', '5', '2', '6', '8', NULL, NULL, NULL, 'There is no reserve force coming. There is only the line, and those who hold it.', 'Comandante dell’ultima linea-3.png'),
    ('CR01-026', 'Voice Beyond the Rift', 'CREATURE', 'Cultists', 'RARE', '5', '2', '7', '7', NULL, NULL, NULL, 'No one has seen what speaks beneath the veil. Everyone nearby has heard their own name.', 'Oracolo del velo oltre la frattura-3.png'),
    ('CR01-027', 'Harvest Overseer', 'CREATURE', 'Aliens', 'RARE', '5', '2', '8', '6', NULL, NULL, NULL, 'To the stranded fleet, this world is neither home nor enemy. It is the price of departure.', 'Sovrintendente alieno del raccolto-2.png'),
    ('CR01-028', 'Breach Capacitor', 'EQUIPMENT', 'Neutral', 'COMMON', NULL, '2', NULL, NULL, 'During each equipped creature’s first combat exchange, it gains +2 attack.', 'Transfers to the next creature. Discard at the end of the round.', 'ROUND', 'One stored spark. One violent opening.', 'Condensatore di breccia-1.png'),
    ('CR01-029', 'Overdrive Harness', 'EQUIPMENT', 'Neutral', 'RARE', NULL, '3', NULL, NULL, 'Each equipped creature gains +2 attack and enters with 1 less starting defense, to a minimum of 1.', 'Transfers to the next creature. Discard at the end of the round.', 'ROUND', 'Every surge leaves something behind.', 'Imbracatura Overdrive da combattimento-3.png'),
    ('CR01-030', 'Bastion Rig', 'EQUIPMENT', 'Neutral', 'RARE', NULL, '3', NULL, NULL, 'Each equipped creature gains +1 attack and enters with +2 starting defense. Does not heal the active creature.', 'Transfers to the next creature. Discard at the end of the round.', 'ROUND', 'Built from the ruins. Made to hold the line.', 'Bastion rig corazzato da officina-2.png');

INSERT INTO cards (code, name, id_expansion, id_type, id_faction, id_rarity,
                   energy_cost, power, value_atk, value_def, description,
                   transfer_and_discard, duration, flavor_text)
SELECT s.code, s.name,
       (SELECT id FROM expansions WHERE code = 'CR01'),
       (SELECT id FROM card_types WHERE code = s.type_code),
       (SELECT id FROM factions WHERE code = CASE s.faction_name
           WHEN 'Radioactive monsters' THEN 'RADIOACTIVE'
           ELSE upper(s.faction_name) END),
       (SELECT id FROM rarities WHERE code = s.rarity_code),
       s.energy_cost, s.power, s.value_atk, s.value_def, s.description,
       s.transfer_and_discard, s.duration, s.flavor_text
FROM initial_set_cards s
ORDER BY s.code;

-- Even cards without an image need an artwork identity for collection/deck ownership.
INSERT INTO cards_arts (id_card, art_code, source_image)
SELECT c.id, 'default', s.source_image
FROM initial_set_cards s JOIN cards c ON c.code = s.code
ORDER BY s.code;
