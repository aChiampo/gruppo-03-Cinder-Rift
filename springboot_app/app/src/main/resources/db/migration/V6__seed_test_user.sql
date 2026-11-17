-- Requested demo account; BCrypt matches the application's password encoder.
-- Fail on an existing email/nickname instead of replacing or granting cards to an account.
INSERT INTO users (nickname, email, hash_password, id_role)
VALUES ('test@email.com', 'test@email.com',
        '$2a$10$cEwQ2OAcwj3Ozk/GM6kLLOm9/OP/GTLUVEdYCBRc7lDzU6kvj7.Bm',
        (SELECT id FROM roles WHERE code = 'USER'));

-- Ranking, registration time, deletion state and currency balances use F01 defaults.
INSERT INTO inventory (id_user)
SELECT id FROM users WHERE email = 'test@email.com';

INSERT INTO collections (id_user, id_card, id_card_art, owned_replica, tradable_replica)
SELECT u.id, c.id, a.id_art, 1, 1
FROM users u
CROSS JOIN cards c
JOIN expansions e ON e.id = c.id_expansion
JOIN cards_arts a ON a.id_card = c.id AND a.art_code = 'default'
WHERE u.email = 'test@email.com' AND e.code = 'CR01'
  AND c.code BETWEEN 'CR01-001' AND 'CR01-030';

-- Sessions and password-reset tokens are created by the authentication flows.
