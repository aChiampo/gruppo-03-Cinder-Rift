-- Repair the original CR01 snapshot without overwriting later catalog customizations.
WITH initial_factions(card_code, faction_code) AS (
    VALUES
        ('CR01-001', 'CYB'), ('CR01-002', 'SAP'), ('CR01-003', 'RAD'),
        ('CR01-004', 'ALN'), ('CR01-005', 'CLT'), ('CR01-008', 'CYB'),
        ('CR01-009', 'RAD'), ('CR01-010', 'SAP'), ('CR01-011', 'CLT'),
        ('CR01-012', 'ALN'), ('CR01-013', 'CYB'), ('CR01-014', 'RAD'),
        ('CR01-015', 'SAP'), ('CR01-016', 'CLT'), ('CR01-017', 'ALN'),
        ('CR01-018', 'CYB'), ('CR01-019', 'RAD'), ('CR01-020', 'SAP'),
        ('CR01-021', 'CLT'), ('CR01-022', 'ALN'), ('CR01-023', 'CYB'),
        ('CR01-024', 'RAD'), ('CR01-025', 'SAP'), ('CR01-026', 'CLT'),
        ('CR01-027', 'ALN')
)
UPDATE cards c
SET id_faction = f.id
FROM initial_factions initial
JOIN factions f ON f.code = initial.faction_code
WHERE c.code = initial.card_code AND c.id_faction IS NULL
  AND c.id_type = (SELECT id FROM card_types WHERE code = 'CREATURE');

UPDATE cards
SET energy_cost = 0
WHERE code IN ('CR01-006', 'CR01-007', 'CR01-028', 'CR01-029', 'CR01-030')
  AND id_type = (SELECT id FROM card_types WHERE code = 'EQUIPMENT')
  AND energy_cost IS NULL;
