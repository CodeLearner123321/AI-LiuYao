package com.divination.liuyao.service;

import com.divination.liuyao.pojo.entity.User;
import com.divination.liuyao.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    public Optional<User> findByPhoneNumber(String phoneNumber) {
        return userMapper.findByPhoneNumber(phoneNumber);
    }
    
    public Optional<User> findByUserName(String userName) {
        return userMapper.findByUserName(userName);
    }
    
    public Optional<User> findById(Long id) {
        return userMapper.findById(id);
    }
    
    public User save(User user) {
        if (user.getId() == null) {
            userMapper.insert(user);
        } else {
            userMapper.update(user);
        }
        return user;
    }
    
    public void deleteById(Long id) {
        userMapper.deleteById(id);
    }

}