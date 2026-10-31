

DROP TRIGGER IF EXISTS provjeri_br_clanova;
DROP TRIGGER IF EXISTS provjeri_termin;
DROP TRIGGER IF EXISTS provjeri_termin_update;
DROP TRIGGER IF EXISTS provjeri_vjezbu;
DROP TRIGGER IF EXISTS provjeri_vjezbu_update;

DELIMITER $$

-- Grupa ne smije primiti više članova od maxClanova.
CREATE TRIGGER provjeri_br_clanova BEFORE INSERT ON clan_u_grupi
FOR EACH ROW
BEGIN
    DECLARE vTrenutnoClanova INT;
    DECLARE vMaxClanova INT;

    SELECT g.maxClanova INTO vMaxClanova
    FROM grupa g
    WHERE g.idgrupa = NEW.grupa_idgrupa;

    SELECT COUNT(*) INTO vTrenutnoClanova
    FROM clan_u_grupi cug
    WHERE cug.grupa_idgrupa = NEW.grupa_idgrupa;

    IF vTrenutnoClanova >= vMaxClanova THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Grupa je puna';
    END IF;
END$$

-- Novi termin se ne smije preklapati sa drugim terminom istog trenera ili iste sale.
CREATE TRIGGER provjeri_termin BEFORE INSERT ON termin
FOR EACH ROW
BEGIN
    DECLARE vZauzetTrener BOOL;
    DECLARE vZauzetaSala BOOL;

    SELECT COUNT(*) > 0 INTO vZauzetTrener
    FROM termin t
    WHERE t.trener_nalog_idnalog = NEW.trener_nalog_idnalog
      AND NEW.Vrijeme < DATE_ADD(t.Vrijeme, INTERVAL t.Trajanje MINUTE)
      AND DATE_ADD(NEW.Vrijeme, INTERVAL NEW.Trajanje MINUTE) > t.Vrijeme;

    SELECT COUNT(*) > 0 INTO vZauzetaSala
    FROM termin t
    WHERE t.sala = NEW.sala
      AND NEW.Vrijeme < DATE_ADD(t.Vrijeme, INTERVAL t.Trajanje MINUTE)
      AND DATE_ADD(NEW.Vrijeme, INTERVAL NEW.Trajanje MINUTE) > t.Vrijeme;

    IF vZauzetTrener THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Zauzet trener u izabranom terminu';
    END IF;
    IF vZauzetaSala THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Zauzeta sala u izabranom terminu';
    END IF;
END$$

-- Isto pravilo pri izmjeni termina (sam termin se ne računa kao preklapanje).
CREATE TRIGGER provjeri_termin_update BEFORE UPDATE ON termin
FOR EACH ROW
BEGIN
    DECLARE vZauzetTrener BOOL;
    DECLARE vZauzetaSala BOOL;

    SELECT COUNT(*) > 0 INTO vZauzetTrener
    FROM termin t
    WHERE t.idtermin <> OLD.idtermin
      AND t.trener_nalog_idnalog = NEW.trener_nalog_idnalog
      AND NEW.Vrijeme < DATE_ADD(t.Vrijeme, INTERVAL t.Trajanje MINUTE)
      AND DATE_ADD(NEW.Vrijeme, INTERVAL NEW.Trajanje MINUTE) > t.Vrijeme;

    SELECT COUNT(*) > 0 INTO vZauzetaSala
    FROM termin t
    WHERE t.idtermin <> OLD.idtermin
      AND t.sala = NEW.sala
      AND NEW.Vrijeme < DATE_ADD(t.Vrijeme, INTERVAL t.Trajanje MINUTE)
      AND DATE_ADD(NEW.Vrijeme, INTERVAL NEW.Trajanje MINUTE) > t.Vrijeme;

    IF vZauzetTrener THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Zauzet trener u izabranom terminu';
    END IF;
    IF vZauzetaSala THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Zauzeta sala u izabranom terminu';
    END IF;
END$$

-- Serije, ponavljanja i težina ne smiju biti negativni.
CREATE TRIGGER provjeri_vjezbu BEFORE INSERT ON vjezba_u_treningu
FOR EACH ROW
BEGIN
    IF NEW.brojSerija < 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Broj serija ne moze biti negativan';
    END IF;
    IF NEW.brojPonavljanja < 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Broj ponavljanja ne moze biti negativan';
    END IF;
    IF NEW.tezina < 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Tezina ne moze biti negativna';
    END IF;
END$$

CREATE TRIGGER provjeri_vjezbu_update BEFORE UPDATE ON vjezba_u_treningu
FOR EACH ROW
BEGIN
    IF NEW.brojSerija < 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Broj serija ne moze biti negativan';
    END IF;
    IF NEW.brojPonavljanja < 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Broj ponavljanja ne moze biti negativan';
    END IF;
    IF NEW.tezina < 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Tezina ne moze biti negativna';
    END IF;
END$$

DELIMITER ;