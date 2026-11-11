package com.chagok.common.protocol;

public enum ErrorCode {
    S000("성공"),
    E100("요청 형식이 올바르지 않습니다"),
    E101("지원하지 않는 명령입니다"),
    E102("필수 항목이 빠졌습니다"),
    E103("값 형식 또는 범위가 올바르지 않습니다"),
    E201("아이디 또는 비밀번호가 올바르지 않습니다"),
    E202("이미 사용 중인 아이디입니다"),
    E203("로그인이 필요합니다"),
    E204("테스트 계정 모드가 꺼져 있음"),
    E205("로그인 시도가 일시적으로 제한되었습니다"),
    E301("대상 데이터 없음"),
    E302("권한이 없습니다"),
    E303("중복 데이터입니다"),
    E304("업무 규칙을 위반했습니다"),
    E401("비교할 같은 연령대 사용자가 부족함"),
    E500("서버 내부 오류"),
    E501("데이터베이스 오류"),
    C901("서버에 연결할 수 없습니다"),
    C902("응답 시간이 초과되었습니다");

    private final String defaultMessage;

    ErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return name();
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public boolean isSuccess() {
        return this == S000;
    }
}
