package com.simple.portal.biz.v1.user.service;

import com.simple.portal.biz.v1.user.UserConst;
import com.simple.portal.biz.v1.user.entity.UserEntity;
import com.simple.portal.biz.v1.user.exception.*;
import com.simple.portal.biz.v1.user.repository.UserRepository;
import com.simple.portal.common.Interceptor.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class UserService {
    private UserRepository userRepository;
    private JwtUtil jwtUtil;

    @Autowired
    public void UserController(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    public List<UserEntity> userFindAllService( ) {
        try {
            return userRepository.findAll();
        }
        catch(Exception e) {
            log.info("[UserService] userFindAllService Error : " + e.getMessage());
            throw new SelectUserFailedException();
        }
    }

    public UserEntity userFineOneService(Long id) {
        try {
            return userRepository.findById(id).get();
        } catch (Exception e) {
            log.info("[UserService] userFindOneService Error : " + e.getMessage());
            throw new SelectUserFailedException();
        }
    }

    public void createUserService(UserEntity user) {
        try {
            user.setCreated(LocalDateTime.now());
            user.setUpdated(LocalDateTime.now());
            user.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt())); // 비밀번호 암호화
            userRepository.save(user);
        } catch (Exception e) {
            log.info("[UserService] createUserService Error : " + e.getMessage());
            throw new CreateUserFailedException();
        }
    };

    public void updateUserService(UserEntity user) {
        try {
            user.setUpdated(LocalDateTime.now());
            userRepository.save(user);
        } catch (Exception e) {
            log.info("[UserService] updateUserService Error : " + e.getMessage());
            throw new UpdateUserFailedException();
        }
    };

    public void deleteUserService(Long id) {
        try {
            userRepository.deleteById(id);
        } catch (Exception e) {
            log.info("[UserService] deleteUserService Error : " + e.getMessage());
            throw new DeleteUserFailedException();
        }
    }

    public Boolean idCheckService(String user_id) {
        try {
            return userRepository.existsUserByUserId(user_id) == true ? true : false;
        } catch (Exception e) {
            log.info("[UserService] idCheckService Error : " + e.getMessage());
            throw new IdCheckFailedException();
        }
    }

    @Transactional
    public String userLoginService(String user_id, String password) { // 성공만 처리하고 나머지 exception 던짐
        try {
            if(!userRepository.existsUserByUserId(user_id)) throw new Exception(UserConst.NO_USER); // 아이디 존재 안함.
            else {
                UserEntity user = userRepository.findByUserId(user_id);
                String pwOrigin = user.getPassword();
                if (BCrypt.checkpw(password, pwOrigin)) return jwtUtil.createToken(user.getUserId());
                else throw new Exception(UserConst.INVALID_PASSWORD); // 비밀번호 오류

            }
        } catch (Exception e) {
            log.info("[UserService] userLoginService Error : " + e.getMessage());
            throw new LoginFailedException(e.getMessage());
    }
    }
}
