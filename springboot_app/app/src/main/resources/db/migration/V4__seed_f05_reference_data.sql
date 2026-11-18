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
    ('SAP', 'Sapiens'),
    ('CYB', 'Cyborg'),
    ('ALN', 'Aliens'),
    ('CLT', 'Cultists'),
    ('RAD', 'Radioactive')
ON CONFLICT (code) DO NOTHING;

INSERT INTO round_configurations (round_name, round_number, max_card_energy, max_round_power, max_cards)
VALUES
    ('ROUND1', 1, 1, 14, 5),
    ('ROUND2', 2, 2, 14, 5),
    ('ROUND3', 3, 3, 14, 5),
    ('ROUND4', 4, 4, 14, 5),
    ('ROUND5', 5, 5, 14, 5),
    ('EQUIPMENT', 6, 6, 10, 5)
ON CONFLICT (round_name) DO NOTHING;
