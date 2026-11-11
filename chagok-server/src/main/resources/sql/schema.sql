-- =============================================================
-- 차곡(chagok) 소비습관 개선 가계부  DB 스키마 (MySQL 8.0 / AWS RDS)
-- 문서: 04_DB설계서 v2.0  |  작성: 쬬양(양재모·조범준·조광희)  |  2026-10-30
-- 문자셋: utf8mb4 / 엔진: InnoDB / 시간대: Asia/Seoul(+09:00)
-- 주의: user, transaction 은 MySQL 키워드이므로 항상 백틱(`)으로 감쌈
--       ERD v1의 repeat 컬럼은 MySQL 예약어이므로 repeat_cycle 로 변경함
-- =============================================================
CREATE DATABASE IF NOT EXISTS chagok
  DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_0900_ai_ci;
USE chagok;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS habit_score;
DROP TABLE IF EXISTS goal_deposit;
DROP TABLE IF EXISTS saving_goal;
DROP TABLE IF EXISTS budget;
DROP TABLE IF EXISTS user_setting;
DROP TABLE IF EXISTS `transaction`;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS `user`;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. 사용자 (FR-1, FR-8, FR-9)
CREATE TABLE `user` (
  user_id      INT          NOT NULL AUTO_INCREMENT COMMENT '사용자 식별자',
  login_id     VARCHAR(20)  NOT NULL                COMMENT '로그인 아이디(영문/숫자 4~20자)',
  password     CHAR(64)     NOT NULL                COMMENT 'SHA-256 해시(16진수 64자)',
  user_name    VARCHAR(20)  NOT NULL                COMMENT '사용자 이름(닉네임)',
  age          INT          NOT NULL                COMMENT '나이(만 나이)',
  gender       CHAR(1)      NOT NULL                COMMENT '성별 M/F',
  is_dummy     TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '1=비교용 더미 사용자',
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '가입 일시',
  PRIMARY KEY (user_id),
  UNIQUE KEY uk_user_login_id (login_id),
  KEY idx_user_age_gender (age, gender),
  CONSTRAINT ck_user_age    CHECK (age BETWEEN 14 AND 99),
  CONSTRAINT ck_user_gender CHECK (gender IN ('M','F'))
) ENGINE=InnoDB COMMENT='사용자';

-- 2. 카테고리 (FR-2, FR-5)
CREATE TABLE category (
  category_id        INT          NOT NULL AUTO_INCREMENT COMMENT '카테고리 식별자',
  type               VARCHAR(10)  NOT NULL                COMMENT 'INCOME / EXPENSE',
  name               VARCHAR(20)  NOT NULL                COMMENT '카테고리 이름',
  default_fixed_type VARCHAR(10)  NULL                    COMMENT '입력 폼 기본값 FIXED/VARIABLE (지출만)',
  sort_order         INT          NOT NULL DEFAULT 0      COMMENT '콤보박스 표시 순서',
  PRIMARY KEY (category_id),
  UNIQUE KEY uk_category_type_name (type, name),
  CONSTRAINT ck_category_type  CHECK (type IN ('INCOME','EXPENSE')),
  CONSTRAINT ck_category_fixed CHECK (default_fixed_type IS NULL OR default_fixed_type IN ('FIXED','VARIABLE'))
) ENGINE=InnoDB COMMENT='수입/지출 카테고리';

-- 3. 내역 (FR-2, FR-3, FR-4, FR-11, FR-12)
CREATE TABLE `transaction` (
  transaction_id   INT           NOT NULL AUTO_INCREMENT COMMENT '내역 식별자',
  user_id          INT           NOT NULL                COMMENT '작성 사용자 FK',
  category_id      INT           NOT NULL                COMMENT '카테고리 FK',
  amount           INT           NOT NULL                COMMENT '금액(원, 양수)',
  transaction_date DATE          NOT NULL                COMMENT '거래 날짜',
  fixed_type       VARCHAR(10)   NULL                    COMMENT '지출 시 FIXED/VARIABLE 필수, 수입은 NULL',
  repeat_cycle     VARCHAR(10)   NOT NULL DEFAULT 'NONE' COMMENT 'NONE/DAILY/WEEKLY/MONTHLY/YEARLY',
  memo             VARCHAR(100)  NULL                    COMMENT '메모',
  parent_tx_id     INT           NULL                    COMMENT '반복 원본 내역 FK(자동 생성분만)',
  is_auto          TINYINT(1)    NOT NULL DEFAULT 0      COMMENT '1=반복 규칙으로 자동 생성',
  created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
  updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
  PRIMARY KEY (transaction_id),
  KEY idx_tx_user_date (user_id, transaction_date),
  KEY idx_tx_category (category_id),
  UNIQUE KEY uk_tx_parent_date (parent_tx_id, transaction_date),
  CONSTRAINT fk_tx_user     FOREIGN KEY (user_id)      REFERENCES `user`(user_id)             ON DELETE CASCADE,
  CONSTRAINT fk_tx_category FOREIGN KEY (category_id)  REFERENCES category(category_id)       ON DELETE RESTRICT,
  CONSTRAINT fk_tx_parent   FOREIGN KEY (parent_tx_id) REFERENCES `transaction`(transaction_id) ON DELETE SET NULL,
  CONSTRAINT ck_tx_amount   CHECK (amount BETWEEN 1 AND 100000000),
  CONSTRAINT ck_tx_fixed    CHECK (fixed_type IS NULL OR fixed_type IN ('FIXED','VARIABLE')),
  CONSTRAINT ck_tx_repeat   CHECK (repeat_cycle IN ('NONE','DAILY','WEEKLY','MONTHLY','YEARLY'))
) ENGINE=InnoDB COMMENT='수입/지출 내역';

-- 4. 사용자 설정 (FR-7 경고 기준, FR-8 비교 옵션, FR-10 예산 알림)
CREATE TABLE user_setting (
  user_id             INT         NOT NULL                COMMENT '사용자 PK/FK (1:1)',
  warn_enabled        TINYINT(1)  NOT NULL DEFAULT 1      COMMENT '과소비 경고 사용 여부',
  warn_ratio          INT         NOT NULL DEFAULT 70     COMMENT '경고 기준 유동지출 비율(%)',
  peer_gap_ratio      INT         NOT NULL DEFAULT 10     COMMENT '또래 평균 대비 초과 허용 폭(%p)',
  budget_alert_ratio  INT         NOT NULL DEFAULT 80     COMMENT '예산 사용률 알림 기준(%)',
  compare_same_gender TINYINT(1)  NOT NULL DEFAULT 0      COMMENT '1=같은 성별만 비교',
  updated_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
  PRIMARY KEY (user_id),
  CONSTRAINT fk_setting_user FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
  CONSTRAINT ck_setting_warn   CHECK (warn_ratio BETWEEN 10 AND 100),
  CONSTRAINT ck_setting_gap    CHECK (peer_gap_ratio BETWEEN 0 AND 50),
  CONSTRAINT ck_setting_budget CHECK (budget_alert_ratio BETWEEN 50 AND 100)
) ENGINE=InnoDB COMMENT='사용자별 경고/비교 설정';

-- 5. 월 예산 (FR-10)
CREATE TABLE budget (
  budget_id    INT       NOT NULL AUTO_INCREMENT COMMENT '예산 식별자',
  user_id      INT       NOT NULL                COMMENT '사용자 FK',
  category_id  INT       NOT NULL                COMMENT '지출 카테고리 FK',
  budget_month CHAR(7)   NOT NULL                COMMENT '대상 월 YYYY-MM',
  amount       INT       NOT NULL                COMMENT '예산 금액(원)',
  created_at   DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
  updated_at   DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
  PRIMARY KEY (budget_id),
  UNIQUE KEY uk_budget_user_cat_month (user_id, category_id, budget_month),
  CONSTRAINT fk_budget_user     FOREIGN KEY (user_id)     REFERENCES `user`(user_id)       ON DELETE CASCADE,
  CONSTRAINT fk_budget_category FOREIGN KEY (category_id) REFERENCES category(category_id) ON DELETE RESTRICT,
  CONSTRAINT ck_budget_amount   CHECK (amount BETWEEN 1 AND 100000000),
  CONSTRAINT ck_budget_month    CHECK (budget_month REGEXP '^[0-9]{4}-(0[1-9]|1[0-2])$')
) ENGINE=InnoDB COMMENT='카테고리별 월 예산';

-- 6. 저축 목표 (FR-15)
CREATE TABLE saving_goal (
  goal_id       INT          NOT NULL AUTO_INCREMENT COMMENT '목표 식별자',
  user_id       INT          NOT NULL                COMMENT '사용자 FK',
  goal_name     VARCHAR(30)  NOT NULL                COMMENT '목표 이름',
  target_amount INT          NOT NULL                COMMENT '목표 금액(원)',
  saved_amount  INT          NOT NULL DEFAULT 0      COMMENT '누적 저축액(goal_deposit 합계)',
  start_date    DATE         NOT NULL                COMMENT '시작일',
  target_date   DATE         NOT NULL                COMMENT '목표일',
  status        VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/ACHIEVED/CANCELED',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
  PRIMARY KEY (goal_id),
  KEY idx_goal_user_status (user_id, status),
  CONSTRAINT fk_goal_user     FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
  CONSTRAINT ck_goal_target   CHECK (target_amount BETWEEN 1000 AND 1000000000),
  CONSTRAINT ck_goal_saved    CHECK (saved_amount >= 0),
  CONSTRAINT ck_goal_date     CHECK (target_date >= start_date),
  CONSTRAINT ck_goal_status   CHECK (status IN ('ACTIVE','ACHIEVED','CANCELED'))
) ENGINE=InnoDB COMMENT='저축 목표';

-- 7. 저축 목표 입금 이력 (FR-15)
CREATE TABLE goal_deposit (
  deposit_id   INT           NOT NULL AUTO_INCREMENT COMMENT '입금 이력 식별자',
  goal_id      INT           NOT NULL                COMMENT '저축 목표 FK',
  amount       INT           NOT NULL                COMMENT '입금액(원, 음수=인출)',
  deposit_date DATE          NOT NULL                COMMENT '입금 날짜',
  memo         VARCHAR(50)   NULL                    COMMENT '메모',
  created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 일시',
  PRIMARY KEY (deposit_id),
  KEY idx_deposit_goal (goal_id, deposit_date),
  CONSTRAINT fk_deposit_goal  FOREIGN KEY (goal_id) REFERENCES saving_goal(goal_id) ON DELETE CASCADE,
  CONSTRAINT ck_deposit_amount CHECK (amount <> 0)
) ENGINE=InnoDB COMMENT='저축 목표 입금/인출 이력';

-- 8. 소비습관 점수 이력 (FR-14)
CREATE TABLE habit_score (
  score_id    INT           NOT NULL AUTO_INCREMENT COMMENT '점수 식별자',
  user_id     INT           NOT NULL                COMMENT '사용자 FK',
  score_month CHAR(7)       NOT NULL                COMMENT '대상 월 YYYY-MM',
  score       INT           NOT NULL                COMMENT '총점 0~100',
  grade       CHAR(1)       NOT NULL                COMMENT '등급 A/B/C/D',
  flex_ratio  DECIMAL(5,1)  NOT NULL                COMMENT '유동지출 비율(%)',
  message     VARCHAR(200)  NOT NULL                COMMENT '피드백 메시지',
  created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '계산 일시',
  PRIMARY KEY (score_id),
  UNIQUE KEY uk_score_user_month (user_id, score_month),
  CONSTRAINT fk_score_user  FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
  CONSTRAINT ck_score_range CHECK (score BETWEEN 0 AND 100),
  CONSTRAINT ck_score_grade CHECK (grade IN ('A','B','C','D'))
) ENGINE=InnoDB COMMENT='월별 소비습관 점수';

-- 통계용 뷰: 사용자별 월 수입/고정지출/유동지출 (FR-6, FR-7, FR-8, FR-14)
CREATE OR REPLACE VIEW v_monthly_summary AS
SELECT t.user_id,
       DATE_FORMAT(t.transaction_date, '%Y-%m') AS ym,
       SUM(CASE WHEN c.type = 'INCOME' THEN t.amount ELSE 0 END)                                    AS income,
       SUM(CASE WHEN c.type = 'EXPENSE' AND t.fixed_type = 'FIXED'    THEN t.amount ELSE 0 END)     AS fixed_expense,
       SUM(CASE WHEN c.type = 'EXPENSE' AND t.fixed_type = 'VARIABLE' THEN t.amount ELSE 0 END)     AS variable_expense
  FROM `transaction` t
  JOIN category c ON c.category_id = t.category_id
 GROUP BY t.user_id, DATE_FORMAT(t.transaction_date, '%Y-%m');
