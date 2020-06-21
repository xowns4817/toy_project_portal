package com.simple.portal.biz.v1.user.api;

import com.simple.portal.biz.v1.user.UserConst;
import com.simple.portal.biz.v1.user.entity.UserEntity;
import com.simple.portal.biz.v1.user.service.UserService;
import com.simple.portal.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Slf4j
@RestController
@RequestMapping("/v1/api/user")
public class UserController {

    private UserService userService;
    private ApiResponse apiResponse;

    @Autowired
    public void UserController(UserService userService, ApiResponse apiResponse) {
        this.userService = userService;
        this.apiResponse = apiResponse;
    }

    //전체 유저 조회
    @GetMapping("")
    public ResponseEntity<?> userFindAll( ) {
        log.info("[GET] /user/ - /userFindAll/");

        apiResponse.setMsg(UserConst.SUCCESS_SELECT_USER);
        apiResponse.setBody(userService.userFindAllService());
        return new ResponseEntity(apiResponse, HttpStatus.OK);
    };

    //특정 유저 조회
    @GetMapping("/{id}")
    public ResponseEntity<?> userFindOne(@PathVariable @NotNull Long id) {
        log.info("[GET] /user/{id}" + id + "/userFindOne/");

        apiResponse.setMsg(UserConst.SUCCESS_SELECT_USER);
        apiResponse.setBody(userService.userFineOneService(id));
        return new ResponseEntity(apiResponse, HttpStatus.OK);
    }

    // 유저 등록 ( 회원 가입 )
    @PostMapping("")
    public ResponseEntity<ApiResponse> userCreate(@Valid @RequestBody UserEntity user, BindingResult bindingResult) {
        log.info("[POST] /user/ userCreateAPI" + "[RequestBody] " + user.toString());

        // client가 요청 잘못했을때 (파라미터 ) - 400
        if(bindingResult.hasErrors()) {
            String errMsg = bindingResult.getAllErrors().get(0).getDefaultMessage(); // 첫번째 에러로 출력
            apiResponse.setCode("400");
            apiResponse.setMsg(UserConst.ERROR_PARAMS);
            apiResponse.setBody(errMsg);
            return new ResponseEntity(apiResponse, HttpStatus.BAD_REQUEST);
        }

        userService.createUserService(user);
        apiResponse.setMsg(UserConst.SUCCESS_CREATE_USER);
        apiResponse.setBody("");
        return  new ResponseEntity(apiResponse, HttpStatus.OK);
    }

    //유저 수정
    @PutMapping("")
    public ResponseEntity<ApiResponse> userUpdate(@Valid @RequestBody UserEntity user, BindingResult bindingResult) {
        log.info("[PUT] /user/ userUpdateApi" + "[RequestBody] " + user);

        // client가 요청 잘못했을때 (파라미터 ) - 400
        if(bindingResult.hasErrors()) {
            String errMsg = bindingResult.getAllErrors().get(0).getDefaultMessage(); // 첫번째 에러로 출력
            apiResponse.setCode("400");
            apiResponse.setMsg(UserConst.ERROR_PARAMS);
            apiResponse.setBody(errMsg);
            return new ResponseEntity(apiResponse, HttpStatus.BAD_REQUEST);
        }

        userService.updateUserService(user);
        apiResponse.setMsg(UserConst.FAILED_UPDATE_USER);
        apiResponse.setBody("");
        return  new ResponseEntity(apiResponse, HttpStatus.OK);
    }

    //유저 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> userDelete(@PathVariable @NotNull Long id) {
        log.info("[DELETE] /user/ " + id + " /userDelete");

        userService.deleteUserService(id);
        apiResponse.setMsg(UserConst.SUCCESS_DELETE_USER);
        apiResponse.setMsg("");
        return new ResponseEntity(apiResponse, HttpStatus.OK);
    }

    // 아이디 중복 체크 ( 회원 가입 )
    @GetMapping("/check/{id}")
    public ResponseEntity<ApiResponse> userIdCheck(@PathVariable("id") String user_id) {
        log.info("[GET] /user/check/id " + user_id + " /userIdCheck");

        if(userService.idCheckService(user_id)) {
            apiResponse.setMsg(UserConst.EXIST_USER);
            apiResponse.setBody("");
            return new ResponseEntity(apiResponse, HttpStatus.OK);
        } else {
            apiResponse.setMsg(UserConst.NO_USER);
            apiResponse.setBody("");
            return new ResponseEntity(apiResponse, HttpStatus.OK);
        }
    };

    // 로그인 ( 암호화는 서버에서 할지? 클라에서 할지 ? )
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> userlogin(HttpServletRequest request) {

        String id = request.getParameter("id");
        String pw = request.getParameter("password");
        log.info("[POST] /user/login " + "[ID] :  "  + id + "[PW] : " + pw + " /userLogin");

        if(userService.userLoginService(id, pw)) {
            apiResponse.setMsg(UserConst.SUCCESS_LOGIN);
            apiResponse.setBody("토큰 정보");
            return new ResponseEntity(apiResponse, HttpStatus.OK);
        } else {
            apiResponse.setMsg(UserConst.FAILED_LOGIN);
            apiResponse.setBody("");
            return new ResponseEntity(apiResponse, HttpStatus.OK);
        }
    }
}