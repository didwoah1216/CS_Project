package com.chagok.server;

import com.chagok.common.dto.UserDTO;

import java.util.HashSet;
import java.util.Set;

public class ClientSession {
    private UserDTO user;
    private final Set<String> popupMonths = new HashSet<>();

    public boolean isLoggedIn() {
        return user != null;
    }

    public int getUserId() {
        if (user == null) {
            return 0;
        }
        return user.getUserId();
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    /** 해당 월 팝업을 아직 안 띄웠으면 true 를 반환하고 기록한다. */
    public boolean markPopup(String ym) {
        return popupMonths.add(ym);
    }
}
