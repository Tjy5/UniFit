package com.authguard.usersystem.service;

import com.authguard.usersystem.entity.UserAccount;

public interface UserAccountService {
    void register(UserAccount user);
    UserAccount login(String userAccount, String password);
    UserAccount getUserByAccount(String userAccount);
    boolean updatePassword(String userAccount, String oldPassword, String newPassword);
}
