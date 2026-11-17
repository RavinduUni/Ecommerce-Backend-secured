-- One account per email. Also makes every sign-in lookup an index lookup.
-- Fails if duplicate emails exist; see the check query in the setup notes.
ALTER TABLE user_entity ADD CONSTRAINT uk_user_entity_email UNIQUE (email);