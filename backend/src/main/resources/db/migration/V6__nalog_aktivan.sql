-- ---------------------------------------------------------------------
-- Meko brisanje naloga (11.4, 12.4, 12.8)
-- Uklanjanje naloga ne briše redove, nego postavlja aktivan = 0.
-- Pravilo za sve upite: obrisani nalozi se ne prijavljuju (401), ne pojavljuju
-- se u listama, a rute za njih vraćaju 404. Svaki upit koji čita naloge mora
-- sadržati uslov aktivan = 1.
-- Pri brisanju se email zamjenjuje sa obrisan-<id>@pumpd.invalid, da se
-- oslobodi za novi nalog (email ima UNIQUE indeks).
-- ---------------------------------------------------------------------
ALTER TABLE nalog ADD COLUMN aktivan BOOLEAN NOT NULL DEFAULT 1;
