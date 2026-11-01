package com.an.admit_one.service;

import com.an.admit_one.dto.response.MeResponse;
import com.an.admit_one.entity.User;
import com.an.admit_one.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {


    public MeResponse getUser(UserPrincipal principal) {

        User user = principal.getUser();

        return new MeResponse(
                user.getId(), user.getFullName(), user.getEmail(),
                user.getPhone(), user.getRole().name());
    }
}
