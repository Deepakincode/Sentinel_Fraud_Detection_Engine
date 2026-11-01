package com.sentinel.dao;

import com.sentinel.model.User;

public interface UserDAO {
    User findByUsername(String username);
}
