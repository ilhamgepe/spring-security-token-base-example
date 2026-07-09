package com.gepe.bayr.shared.constants;

import java.util.UUID;

public final class SystemUsers {

    private SystemUsers() {
    }

    public static final UUID SYSTEM_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000000");
    public static final String SYSTEM_USER_EMAIL = "system@bayr.app";
}
