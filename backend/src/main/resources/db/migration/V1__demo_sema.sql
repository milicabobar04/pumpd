
-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Schema mydb
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema mydb
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Table `nalog`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `nalog` (
  `idnalog` INT NOT NULL auto_increment,
  `ime` VARCHAR(45) NOT NULL,
  `prezime` VARCHAR(45) NOT NULL,
  `email` VARCHAR(100) NULL,
  `lozinka` VARCHAR(225) NOT NULL,
  PRIMARY KEY (`idnalog`),
  UNIQUE INDEX `email_UNIQUE` (`email` ASC) VISIBLE)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `trener`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `trener` (
  `nalog_idnalog` INT NOT NULL,
  `tjelesnaTezina` DECIMAL(5,2) NULL,
  `visina` DECIMAL(5,2) NULL,
  `opis` VARCHAR(500) NULL,
  PRIMARY KEY (`nalog_idnalog`),
  CONSTRAINT `fk_trener_nalog`
    FOREIGN KEY (`nalog_idnalog`)
    REFERENCES `nalog` (`idnalog`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `clan`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `clan` (
  `nalog_idnalog` INT NOT NULL,
  `tjelesnaTezina` DECIMAL(5,2) NULL,
  `visina` DECIMAL(5,2) NULL,
  `pol` VARCHAR(45) NULL,
  `datumRodjenja` DATE NULL,
  PRIMARY KEY (`nalog_idnalog`),
  CONSTRAINT `fk_clan_nalog1`
    FOREIGN KEY (`nalog_idnalog`)
    REFERENCES `nalog` (`idnalog`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `zaposleni`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `zaposleni` (
  `nalog_idnalog` INT NOT NULL,
  PRIMARY KEY (`nalog_idnalog`),
  CONSTRAINT `fk_zaposleni_nalog1`
    FOREIGN KEY (`nalog_idnalog`)
    REFERENCES `nalog` (`idnalog`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `administrator`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `administrator` (
  `zaposleni_nalog_idnalog` INT NOT NULL,
  PRIMARY KEY (`zaposleni_nalog_idnalog`),
  CONSTRAINT `fk_administrator_zaposleni1`
    FOREIGN KEY (`zaposleni_nalog_idnalog`)
    REFERENCES `zaposleni` (`nalog_idnalog`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `radnik`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `radnik` (
  `zaposleni_nalog_idnalog` INT NOT NULL,
  PRIMARY KEY (`zaposleni_nalog_idnalog`),
  CONSTRAINT `fk_radnik_zaposleni1`
    FOREIGN KEY (`zaposleni_nalog_idnalog`)
    REFERENCES `zaposleni` (`nalog_idnalog`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `clanarina`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `clanarina` (
  `vrsta` ENUM('mjesecna', 'sedmicna', 'godisnja') NOT NULL,
  `cijena` DECIMAL(8,2) NOT NULL,
  `datumPocetka` DATE NOT NULL,
  `datumIsteka` DATE NOT NULL,
  `clan_nalog_idnalog` INT NOT NULL,
  PRIMARY KEY (`clan_nalog_idnalog`),
  CONSTRAINT `fk_clanarina_clan1`
    FOREIGN KEY (`clan_nalog_idnalog`)
    REFERENCES `clan` (`nalog_idnalog`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `problem`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `problem` (
  `idproblem` INT NOT NULL,
  `opis` VARCHAR(500) NULL,
  `status` ENUM('neobradjen', 'u_obradi', 'obradjen') NULL,
  `vrijeme` DATETIME NULL,
  `nalog_idnalog` INT NOT NULL,
  `administrator_zaposleni_nalog_idnalog` INT NULL,
  PRIMARY KEY (`idproblem`),
  INDEX `fk_problem_nalog1_idx` (`nalog_idnalog` ASC) VISIBLE,
  INDEX `fk_problem_administrator1_idx` (`administrator_zaposleni_nalog_idnalog` ASC) VISIBLE,
  CONSTRAINT `fk_problem_nalog1`
    FOREIGN KEY (`nalog_idnalog`)
    REFERENCES `nalog` (`idnalog`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_problem_administrator1`
    FOREIGN KEY (`administrator_zaposleni_nalog_idnalog`)
    REFERENCES `administrator` (`zaposleni_nalog_idnalog`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `obavjestenje`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `obavjestenje` (
  `idobavjestenje` INT NOT NULL,
  `sadrzaj` VARCHAR(1000) NULL,
  `datumVrijeme` DATETIME NULL,
  `nalog_idnalog` INT NOT NULL,
  `nalog_idnalog1` INT NOT NULL,
  PRIMARY KEY (`idobavjestenje`),
  INDEX `fk_obavjestenje_nalog1_idx` (`nalog_idnalog` ASC) VISIBLE,
  INDEX `fk_obavjestenje_nalog2_idx` (`nalog_idnalog1` ASC) VISIBLE,
  CONSTRAINT `fk_obavjestenje_nalog1`
    FOREIGN KEY (`nalog_idnalog`)
    REFERENCES `nalog` (`idnalog`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_obavjestenje_nalog2`
    FOREIGN KEY (`nalog_idnalog1`)
    REFERENCES `nalog` (`idnalog`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `teretana`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `teretana` (
  `idteretana` INT NOT NULL,
  `naziv` VARCHAR(45) NULL,
  `adresa` VARCHAR(200) NULL,
  `vlasnik` VARCHAR(45) NULL,
  `radnoVrijeme` VARCHAR(45) NULL,
  `opis` VARCHAR(500) NULL,
  `kontakt` VARCHAR(45) NULL,
  PRIMARY KEY (`idteretana`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `grupa`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `grupa` (
  `idgrupa` INT NOT NULL auto_increment,
  `naziv` VARCHAR(45) NULL,
  `opis` VARCHAR(500) NULL,
  `maxClanova` TINYINT(10) NULL,
  PRIMARY KEY (`idgrupa`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `termin`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `termin` (
  `idtermin` INT NOT NULL auto_increment,
  `Vrijeme` DATETIME NULL,
  `Trajanje` SMALLINT(20) NULL,
  `sala` VARCHAR(45) NULL,
  `trener_nalog_idnalog` INT NOT NULL,
  `grupa_idgrupa` INT NULL,
  PRIMARY KEY (`idtermin`),
  INDEX `fk_termin_trener1_idx` (`trener_nalog_idnalog` ASC) VISIBLE,
  INDEX `fk_termin_grupa1_idx` (`grupa_idgrupa` ASC) VISIBLE,
  CONSTRAINT `fk_termin_trener1`
    FOREIGN KEY (`trener_nalog_idnalog`)
    REFERENCES `trener` (`nalog_idnalog`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_termin_grupa1`
    FOREIGN KEY (`grupa_idgrupa`)
    REFERENCES `grupa` (`idgrupa`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `recenzija`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `recenzija` (
  `idrecenzija` INT NOT NULL,
  `ocjena` TINYINT(10) NULL,
  `komentar` VARCHAR(500) NULL,
  `trener_nalog_idnalog` INT NOT NULL,
  `datum` DATE NULL,
  `clan_nalog_idnalog` INT NOT NULL,
  PRIMARY KEY (`idrecenzija`),
  INDEX `fk_recenzija_trener1_idx` (`trener_nalog_idnalog` ASC) VISIBLE,
  INDEX `fk_recenzija_clan1_idx` (`clan_nalog_idnalog` ASC) VISIBLE,
  CONSTRAINT `fk_recenzija_trener1`
    FOREIGN KEY (`trener_nalog_idnalog`)
    REFERENCES `trener` (`nalog_idnalog`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_recenzija_clan1`
    FOREIGN KEY (`clan_nalog_idnalog`)
    REFERENCES `clan` (`nalog_idnalog`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `objava`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `objava` (
  `idobjava` INT NOT NULL,
  `sadrzaj` VARCHAR(500) NULL,
  `vrijeme` DATETIME NULL,
  `slikaPath` VARCHAR(255) NULL,
  `trener_nalog_idnalog` INT NOT NULL,
  PRIMARY KEY (`idobjava`),
  INDEX `fk_objava_trener1_idx` (`trener_nalog_idnalog` ASC) VISIBLE,
  CONSTRAINT `fk_objava_trener1`
    FOREIGN KEY (`trener_nalog_idnalog`)
    REFERENCES `trener` (`nalog_idnalog`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `plan_treninga`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `plan_treninga` (
  `idplanTreninga` INT NOT NULL auto_increment,
  `naziv` VARCHAR(45) NULL,
  `opis` VARCHAR(500) NULL,
  `datumKreiranja` DATE NULL,
  `trener_nalog_idnalog` INT NULL,
  `clan_nalog_idnalog` INT NULL,
  PRIMARY KEY (`idplanTreninga`),
  INDEX `fk_plan_treninga_trener1_idx` (`trener_nalog_idnalog` ASC) VISIBLE,
  INDEX `fk_plan_treninga_clan1_idx` (`clan_nalog_idnalog` ASC) VISIBLE,
  CONSTRAINT `fk_plan_treninga_trener1`
    FOREIGN KEY (`trener_nalog_idnalog`)
    REFERENCES `trener` (`nalog_idnalog`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_plan_treninga_clan1`
    FOREIGN KEY (`clan_nalog_idnalog`)
    REFERENCES `clan` (`nalog_idnalog`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;




-- -----------------------------------------------------
-- Table `kategorija_vjezbe`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `kategorija_vjezbe` (
  `idkategorija_vjezbe` INT NOT NULL auto_increment,
  `naziv` VARCHAR(45) NULL,
  PRIMARY KEY (`idkategorija_vjezbe`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `vjezba`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `vjezba` (
  `idvjezba` INT NOT NULL auto_increment,
  `naziv` VARCHAR(45) NULL,
  `opis` VARCHAR(500) NULL,
  `administrator_zaposleni_nalog_idnalog` INT NOT NULL,
  `kategorija_vjezbe_idkategorija_vjezbe` INT NOT NULL,
  PRIMARY KEY (`idvjezba`),
  INDEX `fk_vjezba_administrator1_idx` (`administrator_zaposleni_nalog_idnalog` ASC) VISIBLE,
  INDEX `fk_vjezba_kategorija_vjezbe1_idx` (`kategorija_vjezbe_idkategorija_vjezbe` ASC) VISIBLE,
  CONSTRAINT `fk_vjezba_administrator1`
    FOREIGN KEY (`administrator_zaposleni_nalog_idnalog`)
    REFERENCES `administrator` (`zaposleni_nalog_idnalog`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_vjezba_kategorija_vjezbe1`
    FOREIGN KEY (`kategorija_vjezbe_idkategorija_vjezbe`)
    REFERENCES `kategorija_vjezbe` (`idkategorija_vjezbe`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;




-- -----------------------------------------------------
-- Table `vjezba_u_treningu`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `vjezba_u_treningu` (
  `vjezba_idvjezba` INT NOT NULL,
  `plan_treninga_idplanTreninga` INT NOT NULL,
  `brojSerija` TINYINT(10) NULL,
  `brojPonavljanja` TINYINT(10) NULL,
  `tezina` DECIMAL(5,2) NULL,
  PRIMARY KEY (`vjezba_idvjezba`, `plan_treninga_idplanTreninga`),
  INDEX `fk_vjezba_has_plan_treninga_plan_treninga1_idx` (`plan_treninga_idplanTreninga` ASC) VISIBLE,
  INDEX `fk_vjezba_has_plan_treninga_vjezba1_idx` (`vjezba_idvjezba` ASC) VISIBLE,
  CONSTRAINT `fk_vjezba_has_plan_treninga_vjezba1`
    FOREIGN KEY (`vjezba_idvjezba`)
    REFERENCES `vjezba` (`idvjezba`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_vjezba_has_plan_treninga_plan_treninga1`
    FOREIGN KEY (`plan_treninga_idplanTreninga`)
    REFERENCES `plan_treninga` (`idplanTreninga`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `clan_u_grupi`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `clan_u_grupi` (
  `clan_nalog_idnalog` INT NOT NULL,
  `grupa_idgrupa` INT NOT NULL,
  `datumPristupanja` DATE NULL,
  PRIMARY KEY (`clan_nalog_idnalog`, `grupa_idgrupa`),
  INDEX `fk_clan_has_grupa_grupa1_idx` (`grupa_idgrupa` ASC) VISIBLE,
  INDEX `fk_clan_has_grupa_clan1_idx` (`clan_nalog_idnalog` ASC) VISIBLE,
  CONSTRAINT `fk_clan_has_grupa_clan1`
    FOREIGN KEY (`clan_nalog_idnalog`)
    REFERENCES `clan` (`nalog_idnalog`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_clan_has_grupa_grupa1`
    FOREIGN KEY (`grupa_idgrupa`)
    REFERENCES `grupa` (`idgrupa`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `pracenje`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `pracenje` (
  `trener_nalog_idnalog` INT NOT NULL,
  `clan_nalog_idnalog` INT NOT NULL,
  `DatumPracenja` DATE NULL,
  `status` ENUM('aktivan', 'neaktivan', 'na_cekanju') NULL,
  PRIMARY KEY (`trener_nalog_idnalog`, `clan_nalog_idnalog`),
  INDEX `fk_trener_has_clan_clan1_idx` (`clan_nalog_idnalog` ASC) VISIBLE,
  INDEX `fk_trener_has_clan_trener1_idx` (`trener_nalog_idnalog` ASC) VISIBLE,
  CONSTRAINT `fk_trener_has_clan_trener1`
    FOREIGN KEY (`trener_nalog_idnalog`)
    REFERENCES `trener` (`nalog_idnalog`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_trener_has_clan_clan1`
    FOREIGN KEY (`clan_nalog_idnalog`)
    REFERENCES `clan` (`nalog_idnalog`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `sacuvana_objava`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `sacuvana_objava` (
  `clan_nalog_idnalog` INT NOT NULL,
  `objava_idobjava` INT NOT NULL,
  `datumCuvanja` DATE NULL,
  PRIMARY KEY (`clan_nalog_idnalog`, `objava_idobjava`),
  INDEX `fk_clan_has_objava_objava1_idx` (`objava_idobjava` ASC) VISIBLE,
  INDEX `fk_clan_has_objava_clan1_idx` (`clan_nalog_idnalog` ASC) VISIBLE,
  CONSTRAINT `fk_clan_has_objava_clan1`
    FOREIGN KEY (`clan_nalog_idnalog`)
    REFERENCES `clan` (`nalog_idnalog`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_clan_has_objava_objava1`
    FOREIGN KEY (`objava_idobjava`)
    REFERENCES `objava` (`idobjava`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `clan_na_terminu`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `clan_na_terminu` (
  `termin_idtermin` INT NOT NULL,
  `clan_nalog_idnalog` INT NOT NULL,
  `status` ENUM('prijavljen', 'otkazao', 'prisutan') NULL,
  PRIMARY KEY (`termin_idtermin`, `clan_nalog_idnalog`),
  INDEX `fk_termin_has_clan_clan1_idx` (`clan_nalog_idnalog` ASC) VISIBLE,
  INDEX `fk_termin_has_clan_termin1_idx` (`termin_idtermin` ASC) VISIBLE,
  CONSTRAINT `fk_termin_has_clan_termin1`
    FOREIGN KEY (`termin_idtermin`)
    REFERENCES `termin` (`idtermin`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_termin_has_clan_clan1`
    FOREIGN KEY (`clan_nalog_idnalog`)
    REFERENCES `clan` (`nalog_idnalog`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;

alter table grupa
add column trener_nalog_idnalog int;

alter table grupa
add constraint fk_grupa_trener
foreign key (trener_nalog_idnalog)
references trener(nalog_idnalog);