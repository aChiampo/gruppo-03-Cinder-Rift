INSERT INTO card_types (code)
VALUES ('CREATURE'), ('EQUIPMENT')
ON CONFLICT (code) DO NOTHING;

INSERT INTO rarities (code, name, order_index)
VALUES
    ('COMMON', 'Common', 1),
    ('RARE', 'Rare', 2),
    ('EPIC', 'Epic', 3),
    ('LEGENDARY', 'Legendary', 4)
ON CONFLICT (code) DO NOTHING;

INSERT INTO factions (code, name)
VALUES
    ('SAPIENS', 'Sapiens'),
    ('CYBORG', 'Cyborg'),
    ('ALIENS', 'Aliens'),
    ('CULTISTS', 'Cultists'),
    ('RADIOACTIVE', 'Radioactive')
ON CONFLICT (code) DO NOTHING;

INSERT INTO round_configurations (round_name, round_number, max_energy, max_cards)
VALUES
    ('ROUND1', 1, 1, 5),
    ('ROUND2', 2, 2, 5),
    ('ROUND3', 3, 3, 5),
    ('ROUND4', 4, 4, 5),
    ('ROUND5', 5, 5, 5),
    ('EQUIPMENT', 6, 5, 5)
ON CONFLICT (round_name) DO NOTHING;
