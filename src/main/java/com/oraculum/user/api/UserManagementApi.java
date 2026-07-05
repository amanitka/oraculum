package com.oraculum.user.api;

import com.oraculum.user.api.dto.UserDto;

import java.util.List;

public interface UserManagementApi {
    List<UserDto> getAllUsers();
    void createOrUpdateUser(UserDto userDto);
}
