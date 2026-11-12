# 차곡 (chagok)

20대 소비습관 개선 가계부. Java 17, Swing 클라이언트, TCP 9000 JSON Lines 서버, MySQL 8.0.

1단계(FR-2~9)는 로그인 없이 테스트 계정 `test01`로 동작한다.

## 모듈

- `chagok-common` 명령 29개, DTO, 오류 코드
- `chagok-server` 소켓 서버, JDBC DAO, 경고·또래 비교
- `chagok-client` SCR-01~04, 설정(경고 기준)

## DB 접속

비밀번호는 코드에 넣지 않는다. `server.properties.sample`만 저장소에 있고, 실제 값은 환경변수 또는 로컬 파일이 덮어쓴다.

| 환경변수 | 의미 |
| --- | --- |
| `CHAGOK_DB_HOST` | DB 호스트. 로컬 `127.0.0.1`, RDS는 엔드포인트 |
| `CHAGOK_DB_PORT` | 포트. 기본 3306 |
| `CHAGOK_DB_NAME` | 데이터베이스 이름. 기본 `chagok` |
| `CHAGOK_DB_USER` | 사용자 |
| `CHAGOK_DB_PASSWORD` | 비밀번호 |
| `CHAGOK_DB_SSL` | `true`면 TLS. RDS에서 사용 |

`CHAGOK_DB_URL`을 주면 JDBC URL을 직접 쓴다. 그렇지 않으면 host/port/name으로 URL을 만든다.

## 로컬 MySQL

```bash
sudo mysql -e "CREATE DATABASE IF NOT EXISTS chagok CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS 'chagok_app'@'localhost' IDENTIFIED BY '비밀번호';
GRANT ALL PRIVILEGES ON chagok.* TO 'chagok_app'@'localhost'; FLUSH PRIVILEGES;"

export CHAGOK_DB_HOST=127.0.0.1
export CHAGOK_DB_PORT=3306
export CHAGOK_DB_USER=chagok_app
export CHAGOK_DB_PASSWORD='비밀번호'
./scripts/apply-db.sh
```

`schema.sql`은 `CREATE DATABASE`가 있으므로, 계정이 그 권한이 없으면 관리자 계정으로 한 번 적용한다.

```bash
cp chagok-server/src/main/resources/server.properties.sample server.properties
# server.properties 의 db.password 를 바꾸거나, 아래 환경변수만 넘긴다.
export CHAGOK_DB_PASSWORD='비밀번호'
./gradlew :chagok-server:run
./gradlew :chagok-client:run
```

시작 화면에서 **시작하기**를 누르면 `test01`(23세)로 들어간다. 2026년 11~12월에는 더미가 있는 2026-10을 먼저 연다. ◀▶로 다른 달을 볼 수 있다.

통합 테스트도 같은 환경변수를 쓴다. 변수가 없으면 DB 테스트는 건너뛴다.

```bash
CHAGOK_DB_PASSWORD='비밀번호' ./gradlew test
```

## RDS에 나중에 적용

이 작업 환경에서는 MFA 때문에 RDS에 접속하지 않았다. 접속이 되면 같은 스크립트에 엔드포인트만 넣으면 된다.

```bash
export CHAGOK_DB_HOST=your-instance.region.rds.amazonaws.com
export CHAGOK_DB_PORT=3306
export CHAGOK_DB_USER=chagok_app
export CHAGOK_DB_PASSWORD='비밀번호'
export CHAGOK_DB_SSL=true
./scripts/apply-db.sh
```

서버를 띄울 때도 같은 변수를 넘긴다. 보안 그룹은 서버를 실행하는 IP만 3306을 허용한다. 클라이언트 PC에는 DB 비밀번호가 없고, `client.properties`의 `server.host`만 서버 주소로 바꾸면 된다.

## 실행 JAR

```bash
./gradlew :chagok-server:shadowJar :chagok-client:shadowJar
java -jar chagok-server/build/libs/chagok-server.jar
java -jar chagok-client/build/libs/chagok-client.jar
```

서버는 프로젝트 루트의 `server.properties`를 읽고, 환경변수가 있으면 그 값이 우선한다.
