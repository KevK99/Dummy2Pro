SET FOREIGN_KEY_CHECKS = 0;

-- Drops: erst die abhängigen Tabellen, dann die Basistabellen
DROP TABLE IF EXISTS `run_gap_answer`;
DROP TABLE IF EXISTS `question_progress`;
DROP TABLE IF EXISTS `game_run`;

DROP TABLE IF EXISTS `gap_option`;
DROP TABLE IF EXISTS `gap_field`;
DROP TABLE IF EXISTS `answer_option`;
DROP TABLE IF EXISTS `mc_answer`;            -- falls es noch nicht umbenannt wurde
DROP TABLE IF EXISTS `Question_Theme`;
DROP TABLE IF EXISTS `question`;
DROP TABLE IF EXISTS `question_set`;
DROP TABLE IF EXISTS `theme`;
DROP TABLE IF EXISTS `team`;

DROP TABLE IF EXISTS `users`;

SET FOREIGN_KEY_CHECKS = 1;


-- =========================
-- Analysten-Struktur
-- =========================
CREATE TABLE `team` (
    `team_id` INT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`team_id`)
) ENGINE=InnoDB;

CREATE TABLE `theme` (
     `theme_id` INT NOT NULL AUTO_INCREMENT,
     `name` VARCHAR(255) NOT NULL,
     `description` TEXT NULL,
     PRIMARY KEY (`theme_id`)
) ENGINE=InnoDB;

CREATE TABLE `question_set` (
    `question_set_id` INT NOT NULL AUTO_INCREMENT,
    `team_id` INT NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`question_set_id`),
    KEY `idx_question_set_team_id` (`team_id`),
    CONSTRAINT `fk_question_set_team`
        FOREIGN KEY (`team_id`) REFERENCES `team`(`team_id`)
            ON DELETE CASCADE
            ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `question` (
    `question_id` INT NOT NULL AUTO_INCREMENT,
    `question_set_id` INT NOT NULL,
    `question_type` ENUM('MC','TF','GAP') NOT NULL,
    `start_text` TEXT NULL,
    `image_url` TEXT NULL,
    `end_text` TEXT NULL,
    `allows_multiple` TINYINT(1) NOT NULL DEFAULT 0,
    `points` INT NOT NULL DEFAULT 1,
    PRIMARY KEY (`question_id`),
    KEY `idx_question_question_set` (`question_set_id`),
    KEY `idx_question_type` (`question_type`),
    CONSTRAINT `fk_question_question_set`
        FOREIGN KEY (`question_set_id`) REFERENCES `question_set`(`question_set_id`)
            ON DELETE CASCADE
            ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `Question_Theme` (
    `question_id` INT NOT NULL,
    `theme_id` INT NOT NULL,
    PRIMARY KEY (`question_id`, `theme_id`),
    KEY `idx_question_theme_theme` (`theme_id`),
    CONSTRAINT `fk_qt_question`
      FOREIGN KEY (`question_id`) REFERENCES `question`(`question_id`)
          ON DELETE CASCADE
          ON UPDATE CASCADE,
    CONSTRAINT `fk_qt_theme`
      FOREIGN KEY (`theme_id`) REFERENCES `theme`(`theme_id`)
          ON DELETE RESTRICT
          ON UPDATE CASCADE
) ENGINE=InnoDB;


CREATE TABLE `mc_answer` (
     `answer_id` INT NOT NULL AUTO_INCREMENT,
     `question_id` INT NOT NULL,
     `option_text` TEXT NOT NULL,
     `is_correct` TINYINT(1) NOT NULL DEFAULT 0,
     `option_order` INT NOT NULL,
     PRIMARY KEY (`answer_id`),
     UNIQUE KEY `uq_answer_option_question_order` (`question_id`, `option_order`),
     KEY `idx_answer_option_question` (`question_id`),
     CONSTRAINT `fk_answer_option_question`
         FOREIGN KEY (`question_id`) REFERENCES `question`(`question_id`)
             ON DELETE CASCADE
             ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `gap_field` (
     `gap_id` INT NOT NULL AUTO_INCREMENT,
     `question_id` INT NOT NULL,
     `gap_index` INT NOT NULL,
     `text_before` TEXT NULL,
     `text_after` TEXT NULL,
     PRIMARY KEY (`gap_id`),
     UNIQUE KEY `uq_gap_field_question_index` (`question_id`, `gap_index`),
     KEY `idx_gap_field_question` (`question_id`),
     CONSTRAINT `fk_gap_field_question`
         FOREIGN KEY (`question_id`) REFERENCES `question`(`question_id`)
             ON DELETE CASCADE
             ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `gap_option` (
      `gap_option_id` INT NOT NULL AUTO_INCREMENT,
      `gap_id` INT NOT NULL,
      `option_text` TEXT NOT NULL,
      `is_correct` TINYINT(1) NOT NULL DEFAULT 0,
      `option_order` INT NOT NULL,
      PRIMARY KEY (`gap_option_id`),
      UNIQUE KEY `uq_gap_option_gap_order` (`gap_id`, `option_order`),
      KEY `idx_gap_option_gap` (`gap_id`),
      CONSTRAINT `fk_gap_option_gap`
          FOREIGN KEY (`gap_id`) REFERENCES `gap_field`(`gap_id`)
              ON DELETE CASCADE
              ON UPDATE CASCADE
) ENGINE=InnoDB;


-- =========================
-- Projekt-Erweiterungen
-- =========================

RENAME TABLE mc_answer TO answer_option;

CREATE TABLE `users` (
     `user_id` INT NOT NULL AUTO_INCREMENT,
     `username` VARCHAR(255) NOT NULL,
     `password_hash` VARCHAR(255) NOT NULL,
     `avatar` VARCHAR(100) DEFAULT NULL,
     PRIMARY KEY (`user_id`),
     UNIQUE KEY `uq_users_username` (`username`)
) ENGINE=InnoDB;

CREATE TABLE `game_run` (
    `run_id` INT NOT NULL AUTO_INCREMENT,
    `user_id` INT NOT NULL,
    `started_at` DATETIME NOT NULL,
    `finished_at` DATETIME NULL,
    `display_name` VARCHAR(100) NULL,
    PRIMARY KEY (`run_id`),
    KEY `idx_run_user` (`user_id`),
    CONSTRAINT `fk_run_user`
        FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`)
            ON DELETE CASCADE
            ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `question_progress` (
     `run_id` INT NOT NULL,
     `question_id` INT NOT NULL,
     `status` ENUM('OPEN','CORRECT','WRONG') NOT NULL DEFAULT 'OPEN',
     `answered_at` DATETIME NULL,
     `selected_answer_id` INT NULL,
     PRIMARY KEY (`run_id`, `question_id`),
     KEY `idx_qp_question` (`question_id`),
     KEY `idx_qp_answer` (`selected_answer_id`),
     CONSTRAINT `fk_qp_run`
         FOREIGN KEY (`run_id`) REFERENCES `game_run`(`run_id`)
             ON DELETE CASCADE
             ON UPDATE CASCADE,
     CONSTRAINT `fk_qp_question`
         FOREIGN KEY (`question_id`) REFERENCES `question`(`question_id`)
             ON DELETE CASCADE
             ON UPDATE CASCADE,
     CONSTRAINT `fk_qp_answer`
         FOREIGN KEY (`selected_answer_id`) REFERENCES `answer_option`(`answer_id`)
             ON DELETE SET NULL
             ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `run_gap_answer` (
      `run_id` INT NOT NULL,
      `question_id` INT NOT NULL,
      `gap_id` INT NOT NULL,
      `selected_gap_option_id` INT NOT NULL,
      `answered_at` DATETIME NULL,
      PRIMARY KEY (`run_id`, `question_id`, `gap_id`),
      KEY `idx_rga_question` (`question_id`),
      KEY `idx_rga_gap` (`gap_id`),
      KEY `idx_rga_option` (`selected_gap_option_id`),
      CONSTRAINT `fk_rga_run`
          FOREIGN KEY (`run_id`) REFERENCES `game_run`(`run_id`)
              ON DELETE CASCADE
              ON UPDATE CASCADE,
      CONSTRAINT `fk_rga_question`
          FOREIGN KEY (`question_id`) REFERENCES `question`(`question_id`)
              ON DELETE CASCADE
              ON UPDATE CASCADE,
      CONSTRAINT `fk_rga_gap`
          FOREIGN KEY (`gap_id`) REFERENCES `gap_field`(`gap_id`)
              ON DELETE CASCADE
              ON UPDATE CASCADE,
      CONSTRAINT `fk_rga_gap_option`
          FOREIGN KEY (`selected_gap_option_id`) REFERENCES `gap_option`(`gap_option_id`)
              ON DELETE CASCADE
              ON UPDATE CASCADE
) ENGINE=InnoDB;