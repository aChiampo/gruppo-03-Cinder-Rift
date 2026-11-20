INSERT INTO effects (code, description)
VALUES
    ('ATTACK_BONUS', 'On entry, the host creature gains the configured attack modifier until it leaves battle.'),
    ('STARTING_DEFENSE_MODIFIER', 'On entry, modify the host creature starting defense, respecting the configured minimum.'),
    ('FIRST_EXCHANGE_ATTACK_BONUS', 'On entry, the host creature gains an attack modifier that expires after its first combat exchange.');

-- Resolve identities through stable codes, including on databases with existing rows.
INSERT INTO card_effects (card_id, effect_id, parameters, execution_order)
SELECT c.id, e.id, seed.parameters, seed.execution_order
FROM (VALUES
    ('CR01-006', 'ATTACK_BONUS', '{"amount":1}'::jsonb, 1),
    ('CR01-007', 'STARTING_DEFENSE_MODIFIER', '{"amount":2,"minimum":1}'::jsonb, 1),
    ('CR01-028', 'FIRST_EXCHANGE_ATTACK_BONUS', '{"amount":2}'::jsonb, 1),
    ('CR01-029', 'ATTACK_BONUS', '{"amount":2}'::jsonb, 1),
    ('CR01-029', 'STARTING_DEFENSE_MODIFIER', '{"amount":-1,"minimum":1}'::jsonb, 2),
    ('CR01-030', 'ATTACK_BONUS', '{"amount":1}'::jsonb, 1),
    ('CR01-030', 'STARTING_DEFENSE_MODIFIER', '{"amount":2,"minimum":1}'::jsonb, 2)
) AS seed(card_code, effect_code, parameters, execution_order)
JOIN cards c ON c.code = seed.card_code
JOIN effects e ON e.code = seed.effect_code
ORDER BY c.code, seed.execution_order;
