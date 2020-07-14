package com.simple.portal.biz.v1.user.service;

import com.simple.portal.biz.v1.user.UserConst;
import com.simple.portal.biz.v1.user.entity.UserEntity;
import com.simple.portal.biz.v1.user.exception.*;
import com.simple.portal.biz.v1.user.repository.UserRepository;
import com.simple.portal.common.Interceptor.JwtUtil;
import com.simple.portal.util.ApiHelper;
import com.simple.portal.util.CustomMailSender;
import com.simple.portal.util.DateFormatUtil;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.io.File;
import java.util.List;

@Slf4j
@Service
public class UserService {
    private UserRepository userRepository;
    private CustomMailSender mailSender;
    private JwtUtil jwtUtil;
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    public void UserController(UserRepository userRepository, CustomMailSender mailSender, JwtUtil jwtUtil, RedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.mailSender = mailSender;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
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

     // 유저의 기본키로 유저 조회
    public UserEntity userFineOneService(Long id) {
        try {
            return userRepository.findById(id).get();
        } catch (Exception e) {
            log.info("[UserService] userFindOneService Error : " + e.getMessage());
            throw new SelectUserFailedException();
        }
    }

    @Transactional
    public void createUserService(UserEntity user, MultipartFile file) {
        try {
            String imgDir = "/E:\\file_test\\" + user.getUserId() + "-profileImg.png";
            file.transferTo(new File(imgDir)); // 해당 경로에 파일 생성

            // 빌더 패턴 적용
            UserEntity insertUser = UserEntity.builder()
                    .userId(user.getUserId())
                    .nickname(user.getNickname())
                    .password(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt())) // 비밀번호
                    .gitAddr(user.getGitAddr())
                    .profileImg(imgDir)
                    .activityScore(user.getActivityScore())
                    .authority(user.getAuthority())
                    .created(DateFormatUtil.makeNowTimeStamp())
                    .updated(DateFormatUtil.makeNowTimeStamp())
                    .build();

            userRepository.save(insertUser);
            try {
                mailSender.sendJoinMail("Okky 회원 가입 완료 메일 !", user.getUserId()); // 회원가입 후 해당 이메일로 인증 메일보냄
            } catch (Exception e) {
                log.info("[UserService] emailSend Error : " + e.getMessage());
                throw new EmailSendFailedException();
            }
        } catch (Exception e) {

            log.info("[UserService] createUserService Error : " + e.getMessage());
            throw new CreateUserFailedException();
        }
    };

    public void updateUserService(UserEntity user, MultipartFile file) {
        try {

            String imgDir = "/E:\\file_test\\" + user.getUserId() + "-profileImg.png";
            file.transferTo(new File(imgDir)); // 해당 경로에 파일 생성
            // 빌더 패턴 적용
            UserEntity updateUser = UserEntity.builder()
                    .id(user.getId())
                    .userId(user.getUserId())
                    .nickname(user.getNickname())
                    .password(user.getPassword()) // 비밀번호
                    .gitAddr(user.getGitAddr())
                    .profileImg(imgDir)
                    .activityScore(user.getActivityScore())
                    .authority(user.getAuthority())
                    .created(user.getCreated())
                    .updated(DateFormatUtil.makeNowTimeStamp())
                    .build();

            userRepository.save(updateUser);
        } catch (Exception e) {
            log.info("[UserService] updateUserService Error : " + e.getMessage());
            throw new UpdateUserFailedException();
        }
    };

    @Transactional
    public void deleteUserService(Long id) {
        try {
            UserEntity deleteUser = userRepository.findById(id).get();
            String imgDir = deleteUser.getProfileImg();

            userRepository.deleteById(id);
            File deleteFile = new File(imgDir);
            if (deleteFile.exists()) { // 프로필 이미지 삭제
                deleteFile.delete();
            };

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

    // 유저 권한 부여
    public Boolean updateUserAuthService(String userId) {
        try {
            userRepository.updateUserAuth(userId);
            return true;
        } catch (Exception e) {
            log.info("[UserService] userAuthService Error : " + e.getMessage());
            return false;
            //throw new UserAuthGrantFailedException();
        }
    }

    //유저 권한 체크
    public char userAuthCheckServie(String userId) {
        try {
            return userRepository.checkUserAuth(userId);
        } catch (Exception e) {
            log.info("[UserService] userAuthCheckService Error : " + e.getMessage());
            throw new UserAuthCheckFailedException();
        }
    }

    // 비밀번호 변경
    public void updateUserPasswordService(Long id, String newPassword) {
        try{
            userRepository.updatePassword(id,  BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        } catch (Exception e) {
            log.info("[UserService] updateUserPassword Error : " + e.getMessage());
            throw new UpdatePasswordFailedException();
        }
    }

    // 비밀번호 찾기 ( = 새로운 비밀번호 전송 )
    @Transactional
    public void findUserPasswordService(Long id, String user_id) {
        try {
            // 랜덤값으로 비밀번호 변경 후 -> 이메일 발송
            String randomValue = ApiHelper.getRandomString(); // 이 값을 메일로 전송
            userRepository.updatePassword(id, BCrypt.hashpw(randomValue, BCrypt.gensalt()));
            try {
                mailSender.sendNewPwMail("신규 비밀번호 안내 !", user_id, randomValue); // 회원가입 후 해당 이메일로 인증 메일보냄
            } catch (Exception e) {
                log.info("[UserService] emailSend Error : " + e.getMessage());
                throw new EmailSendFailedException();
            }
            // 해당 값을 이메일로 발송
        } catch (Exception e) {
            log.info("[UserService] findUserPassword Error : " + e.getMessage());
            throw new FindPasswordFailedException();
        }
    }

    // 팔로우하기
    public void followService(Long followed_id, Long following_id) {
        try {
            // 매 요청마다 operation을 만들어야 되나 ?
            SetOperations<String, String> setOperations = redisTemplate.opsForSet();
            String followerKey = "user:follower:" + followed_id;
            String followingKey = "user:following:" + following_id;
            setOperations.add(followerKey, String.valueOf(following_id));
            setOperations.add(followingKey, String.valueOf(followed_id));
        } catch (Exception e) {
            log.info("[UserService] followService Error : " + e.getMessage());
            throw new FollowFailedException();
        }
    }
}
