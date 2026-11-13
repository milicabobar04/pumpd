-- ---------------------------------------------------------------------
-- Privremena lozinka (11.3, 12.3, 12.7): server generiše lozinku za novi nalog,
-- a korisnik je mora promijeniti (PUT /api/users/my-password).
-- Postojeći nalozi (i seed) dobijaju 0, pa se mogu koristiti bez promjene.
-- ---------------------------------------------------------------------
ALTER TABLE nalog ADD COLUMN moraPromijenitiLozinku BOOLEAN NOT NULL DEFAULT 0;
