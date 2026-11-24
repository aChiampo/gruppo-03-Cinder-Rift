CREATE TABLE friendships (
    user_a_id int NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_b_id int NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    requested_by int NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    accepted_at timestamptz,
    PRIMARY KEY (user_a_id, user_b_id),
    CONSTRAINT friendships_ordered_pair_check CHECK (user_a_id < user_b_id),
    CONSTRAINT friendships_requester_check CHECK (requested_by IN (user_a_id, user_b_id)),
    CONSTRAINT friendships_status_check CHECK (status IN ('pending', 'accepted')),
    CONSTRAINT friendships_acceptance_check CHECK (
        (status = 'pending' AND accepted_at IS NULL) OR
        (status = 'accepted' AND accepted_at IS NOT NULL))
);
