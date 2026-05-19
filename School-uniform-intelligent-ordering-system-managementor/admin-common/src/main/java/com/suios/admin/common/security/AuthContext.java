package com.suios.admin.common.security;

public interface AuthContext {

    String getCurrentUsername();

    Long getCurrentUserId();
}
