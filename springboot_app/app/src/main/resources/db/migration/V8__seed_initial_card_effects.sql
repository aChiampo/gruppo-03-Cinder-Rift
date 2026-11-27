INSERT INTO effects (code, description)
VALUES
    ('ATTACK_BONUS', 'On entry, the host creature gains the configured attack modifier until it leaves battle.'),
    ('STARTING_DEFENSE_MODIFIER', 'On entry, modify the host creature starting defense, respecting the configured minimum.'),
    ('FIRST_EXCHANGE_ATTACK_BONUS', 'On entry, the host creature gains an attack modifier that expires after its first combat exchange.');

-- Resolve the generated card ID within the initial expansion; never assume seed IDs.
-- A missing or ambiguous card fails instead of silently skipping its effects.
INSERT INTO card_effects (card_id, effect_id, parameters, execution_order)
SELECT (SELECT c.id FROM cards c JOIN expansions expansion ON expansion.id = c.id_expansion
        WHERE c.name = seed.card_name AND expansion.code = 'CR01'),
       e.id, seed.parameters, seed.execution_order
FROM (VALUES
    ('Pulse Blade', 'ATTACK_BONUS', '{"amount":1}'::jsonb, 1),
    ('Recovery Armor', 'STARTING_DEFENSE_MODIFIER', '{"amount":2,"minimum":1}'::jsonb, 1),
    ('Breach Capacitor', 'FIRST_EXCHANGE_ATTACK_BONUS', '{"amount":2}'::jsonb, 1),
    ('Overdrive Harness', 'ATTACK_BONUS', '{"amount":2}'::jsonb, 1),
    ('Overdrive Harness', 'STARTING_DEFENSE_MODIFIER', '{"amount":-1,"minimum":1}'::jsonb, 2),
    ('Bastion Rig', 'ATTACK_BONUS', '{"amount":1}'::jsonb, 1),
    ('Bastion Rig', 'STARTING_DEFENSE_MODIFIER', '{"amount":2,"minimum":1}'::jsonb, 2)
) AS seed(card_name, effect_code, parameters, execution_order)
JOIN effects e ON e.code = seed.effect_code
ORDER BY seed.card_name, seed.execution_order;
