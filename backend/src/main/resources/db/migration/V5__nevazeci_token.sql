-- Lista nevažećih (odjavljenih) JWT tokena, 1.2 POST /api/auth/logout.
-- Čuva se samo jti tokena i trenutak isteka; nakon isteka red se može obrisati
-- jer bi token ionako bio odbijen zbog roka važenja.
CREATE TABLE IF NOT EXISTS nevazeci_token (
    jti    VARCHAR(36) NOT NULL,
    istice DATETIME(6) NOT NULL,
    PRIMARY KEY (jti),
    INDEX idx_nevazeci_token_istice (istice ASC)
) ENGINE = InnoDB;
