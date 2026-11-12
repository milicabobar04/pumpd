-- ---------------------------------------------------------------------
-- 7.5: član ima najviše jednu recenziju po treneru; nova recenzija zamjenjuje
-- staru (isti reviewId). UNIQUE indeks sprečava dva reda i kod dvostrukog klika.
-- ---------------------------------------------------------------------
ALTER TABLE recenzija
    ADD UNIQUE INDEX recenzija_trener_clan_UNIQUE (trener_nalog_idnalog, clan_nalog_idnalog);
