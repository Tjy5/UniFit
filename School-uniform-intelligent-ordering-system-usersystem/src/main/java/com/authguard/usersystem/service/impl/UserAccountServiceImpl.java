package com.authguard.usersystem.service.impl;

import com.authguard.usersystem.entity.UserAccount;
import com.authguard.usersystem.mapper.UserAccountMapper;
import com.authguard.usersystem.exception.BizException;
import com.authguard.usersystem.service.UserAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserAccountServiceImpl implements UserAccountService {

    @Autowired
    private UserAccountMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(); // 加密工具

    @Override
    public void register(UserAccount user) {
        if (userMapper.findByAccount(user.getUserAccount()) != null) {
            throw new BizException("用户已存在");
        }

        String encryptedPassword = passwordEncoder.encode(user.getUserPassword());
        user.setUserPassword(encryptedPassword);

        userMapper.insertUser(user);
    }

    @Override
    public UserAccount login(String userAccount, String password) {
        UserAccount user = userMapper.findByAccount(userAccount);
        if (user == null) {
            throw new BizException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(password, user.getUserPassword())) {
            throw new BizException("用户名或密码错误");
        }

        return user;
    }

    @Override
    public UserAccount getUserByAccount(String userAccount) {
        return userMapper.findByAccount(userAccount);
    }

    @Override
    public boolean updatePassword(String userAccount, String oldPassword, String newPassword) {
        UserAccount user = userMapper.findByAccount(userAccount);
        if (user == null) {
            throw new BizException("用户不存在");
        }

        if (!passwordEncoder.matches(oldPassword, user.getUserPassword())) {
            throw new BizException("旧密码错误");
        }

        String encryptedNewPassword = passwordEncoder.encode(newPassword);

        boolean updated = userMapper.updatePassword(userAccount, user.getUserPassword(), encryptedNewPassword) > 0;
        if (!updated) {
            throw new BizException("密码更新失败");
        }
        return true;
    }
}
