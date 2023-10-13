package com.sahay.third.party.controller;


import com.sahay.third.party.config.JwtTokenUtil;
import com.sahay.third.party.exception.CustomException;
import com.sahay.third.party.model.*;
import com.sahay.third.party.object.*;
import com.sahay.third.party.repo.ClientRepository;
import com.sahay.third.party.repo.UserActivityRepository;
import com.sahay.third.party.service.UserService;
import com.sahay.third.party.service.UtilService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/third-party/api/v1/portal")
public class UserController {

    private final UserService userService;

    private final JwtTokenUtil jwtTokenUtil;

    private final UserDetailsService userDetailsService;

    private final ClientRepository userRepository;
    private final UserActivityRepository userActivityRepository;
    private final AuthenticationManager authenticationManager;

    private final UtilService utilService;

//    @GetMapping("/admin")
//    @PreAuthorize("hasAuthority('admin')")
//    public String adminOnly() {
//        return "Only admin users can see this";
//    }
//
//    @GetMapping("/users")
//    @PreAuthorize("hasAnyAuthority('user' , 'admin')") //
//    public String userAndAdminOnly() {
//        return "Only admin and user users can see this";
//    }

    // create menu
    @PostMapping("/menu")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<?> createMenu(@RequestBody Menu menuBody) throws CustomException {
        Menu menu = userService.createMenu(menuBody);
        return new ResponseEntity<>(menu, HttpStatus.OK);
    }

    // get menus
    @GetMapping(value = "/menu", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('admin' , 'user')")
    public ResponseEntity<?> getMenus() {
        List<Menu> menus = userService.fetchMenus();
        var customResponse = new MenuDto();

        customResponse.setResponse("000");
        customResponse.setResponseDescription("success");
        customResponse.setMenus(menus);
        return new ResponseEntity<>(customResponse, HttpStatus.OK);

    }

    // create permission
    @PostMapping("/permission")
    @PreAuthorize("hasAuthority('admin')")

    public ResponseEntity<?> createPermission(@RequestBody Permission permissionBody) throws CustomException {
        Permission permission = userService.createPermission(permissionBody);
        return new ResponseEntity<>(permission, HttpStatus.OK);

    }

    // get permissions
    @GetMapping(value = "/permission", produces = "application/json")
    @PreAuthorize("hasAnyAuthority('admin' , 'user')")
    public ResponseEntity<?> getPermissions() {
        List<Permission> permissions = userService.fetchPermissions();
        return new ResponseEntity<>(permissions, HttpStatus.OK);

    }

    // create role
    @PostMapping("/role")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<?> createRole(@RequestBody Role roleBody) throws CustomException {
        Role permission = userService.createRole(roleBody);
        return new ResponseEntity<>(permission, HttpStatus.OK);
    }

    // get roles

    @GetMapping("/role")
    @PreAuthorize("hasAnyAuthority('admin' , 'user')")
    public ResponseEntity<?> getRoles() throws CustomException {
        List<Role> roles = userService.fetchRoles();
        return new ResponseEntity<>(roles, HttpStatus.OK);
    }

    // deactivate role
    @GetMapping("/role/{roleId}")
    @PreAuthorize("hasAnyAuthority('admin' , 'user')")
    public ResponseEntity<?> deactivateRole(@PathVariable() int roleId) throws CustomException {
        Role role = userService.deactivateRole(roleId);
        return new ResponseEntity<>(role, HttpStatus.OK);
    }
    // create user

    @PostMapping(value = "/user")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<?> createUser(@Validated @RequestBody CreateUserRequest userBody, BindingResult bindingResult) throws CustomException {

        var userActivityLog = new UserActivity();
        log.info("create user : {}", userBody);
        if (bindingResult.hasErrors()) {
            userActivityLog.setActivity("INVALID EMAIL");
            utilService.saveUserActivity(userActivityLog);
            throw new CustomException("Invalid email");
        }
        userService.createUser(userBody);
        var customResponse = new CustomResponse();
        customResponse.setResponse("000");
        customResponse.setResponseDescription("User created successfully");
        return new ResponseEntity<>(customResponse, HttpStatus.OK);

    }

    // get users

    @GetMapping("/user")
//    @PreAuthorize("hasAnyAuthority('admin' , 'user')")
    public ResponseEntity<?> getUsers() {
        List<UserResponse> userResponses = userService.fetchUsers();
        return new ResponseEntity<>(userResponses, HttpStatus.OK);
    }

    // deactivate user

    @PutMapping(value = "/user/{userId}")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<?> deactivateUser(@PathVariable int userId) throws CustomException {
        userService.deactivateUser(userId);
        var customResponse = new CustomResponse();
        customResponse.setResponse("000");
        customResponse.setResponseDescription("User deactivated successfully");
        return ResponseEntity.ok().body(customResponse);
    }

    @PostMapping(value = "/login")
    public ResponseEntity<?> auth(@RequestBody AuthRequest authRequest, HttpServletRequest request) {
        var authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                authRequest.getUsername(), authRequest.getPassword()
        ));
        var customResponse = new CustomResponse();

        if (!authentication.isAuthenticated()) {
            throw new UsernameNotFoundException("invalid username or password");
        }
        Client user = userRepository.findClientByUsername(authRequest.getUsername());
        if (!user.getIsActive() && user.getIsPasswordChanged()) {
            customResponse.setResponse("007");
            customResponse.setResponseDescription("cannot login user is deactivated");
            return ResponseEntity.ok().body(customResponse);
        }
        log.info("USER DETAILS :{}", authentication.getAuthorities());
        final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getUsername());
        String token = jwtTokenUtil.generateToken(userDetails);
        var authResponse = new AuthResponse();
        authResponse.setResponse("000");
        authResponse.setResponseDescription("success");
        authResponse.setUsername(authRequest.getUsername());
        authResponse.setToken(token);
        var userActivityLog = new UserActivity();
        userActivityLog.setUsername(authRequest.getUsername());
        userActivityLog.setActivity("LOGIN");
        utilService.saveUserActivity(userActivityLog);
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, token)
                .body(authResponse);
    }

    @PreAuthorize("hasAnyAuthority('admin' , 'user')")
    @PutMapping(value = "/change-password/{userId}")
    public ResponseEntity<?> changePassword(@PathVariable() int userId, @Validated @RequestBody ChangePasswordRequest request, BindingResult bindingResult) throws CustomException {

        var userActivityLog = new UserActivity();
        if (bindingResult.hasErrors()) {
            userActivityLog.setActivity("PASSWORD DOESN'T MATCH");
            utilService.saveUserActivity(userActivityLog);
            throw new CustomException("Passwords doesn't match");
        }
        userService.changePassword(request, userId);
        var customResponse = new CustomResponse();
        customResponse.setResponse("000");
        customResponse.setResponseDescription("Password changed successfully");
        return new ResponseEntity<>(customResponse, HttpStatus.OK);
    }

    // by the admin @PreAuthorize("ADMIN")
//    @PreAuthorize("hasAuthority('admin')")
    @PostMapping(value = "/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) throws CustomException {
        userService.forgotPassword(request.getEmail(), request.getPhoneNumber());
        var customResponse = new CustomResponse();
        customResponse.setResponse("000");
        customResponse.setResponseDescription("Password reset successful check your email or phone for one time password");
        return new ResponseEntity<>(customResponse, HttpStatus.OK);
    }
    // USER ACTIVITY
    @PreAuthorize("hasAuthority('admin')")
    @GetMapping("/user-activity")
    public ResponseEntity<?> fetchUserActivityLog() {
        List<UserActivity> userActivity = userService.getUserActivity();
        return new ResponseEntity<>(userActivity, HttpStatus.OK);
    }

}
