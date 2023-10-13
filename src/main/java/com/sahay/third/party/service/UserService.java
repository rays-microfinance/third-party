package com.sahay.third.party.service;

import com.sahay.third.party.exception.CustomException;
import com.sahay.third.party.model.*;
import com.sahay.third.party.object.ChangePasswordRequest;
import com.sahay.third.party.object.CreateUserRequest;
import com.sahay.third.party.object.UserResponse;
import com.sahay.third.party.repo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {


    @Value("${org.app.properties.password-regex}")
    private String regex;

    private final ClientRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final MenuRepository menuRepository;

    private final UserActivityRepository userActivityRepository;

    private final UtilService utilService;
    private final BCryptPasswordEncoder passwordEncoder;

    // create menu
    public Menu createMenu(Menu menu) throws CustomException {

        if (menuRepository.findMenuByName(menu.getName()).isPresent())
            throw new CustomException("menu exists");

        menu.setCreatedAt(Timestamp.from(Instant.now()));
        UserActivity userActivityLog = new UserActivity();
        userActivityLog.setActivity("CREATE MENU");
        utilService.saveUserActivity(userActivityLog);
        return menuRepository.save(menu);

    }

    // get menu
    public List<Menu> fetchMenus() {
        UserActivity userActivityLog = new UserActivity();
        userActivityLog.setActivity("VIEW MENUS");
        utilService.saveUserActivity(userActivityLog);
        return menuRepository.findAll();
    }

    // create permission
    public Permission createPermission(Permission permission) throws CustomException {

        boolean permissionExists = permissionRepository.findPermissionByName(permission.getName()).isPresent();

        if (permissionExists)
            throw new CustomException("Permission already exists");

        permission.setCreatedAt(Timestamp.from(Instant.now()));
        UserActivity userActivityLog = new UserActivity();
        userActivityLog.setActivity("CREATE PERMISSION");
        utilService.saveUserActivity(userActivityLog);
        return permissionRepository.save(permission);
    }

    // create role
    public Role createRole(Role role) throws CustomException {

        if (roleRepository.findRoleByName(role.getName()).isPresent())
            throw new CustomException("Role name already exists");

        if (roleRepository.findRoleByRoleCode(role.getRoleCode()).isPresent())
            throw new CustomException("Role code already exists");

        role.setCreatedAt(Timestamp.from(Instant.now()));
        UserActivity userActivityLog = new UserActivity();
        userActivityLog.setActivity("CREATE ROLE");
        utilService.saveUserActivity(userActivityLog);
        return roleRepository.save(role);
    }

    // get roles

    public List<Role> fetchRoles() {
        UserActivity userActivityLog = new UserActivity();
        userActivityLog.setActivity("VIEW ROLES");
        utilService.saveUserActivity(userActivityLog);
        return roleRepository.findAll();
    }

    // deactivate role
    public Role deactivateRole(int id) throws CustomException {
        Role role = roleRepository.findById(id).get();
        if (role == null)
            throw new CustomException("Role with id " + id + " doesn't exists");
        role.setActive(false);

        UserActivity userActivityLog = new UserActivity();
        userActivityLog.setActivity("USER DEACTIVATION");
        utilService.saveUserActivity(userActivityLog);
        roleRepository.save(role);
        return role;
    }

    public List<Permission> fetchPermissions() {
        UserActivity userActivityLog = new UserActivity();
        userActivityLog.setActivity("VIEW PERMISSIONS");
        utilService.saveUserActivity(userActivityLog);
        return permissionRepository.findAll();
    }

    public void createUser(CreateUserRequest userRequest) throws CustomException {

        var user = new Client();
        Client username = userRepository.findClientByUsername(userRequest.getUsername());

        if (username != null) {
            throw new CustomException("Username is taken");
        }

        Client clientByEmail = userRepository.findClientByEmail(userRequest.getEmail());
        if (clientByEmail != null) {
            throw new CustomException("Email is taken");
        }

        Client clientByPhone = userRepository.findClientByPhoneNumber(user.getPhoneNumber());


        if (clientByPhone != null) {
            throw new CustomException("Phone number is taken");

        }

        String otp = utilService.generateOtp();

        log.info("OTP : {}", otp, "USER EMAIL : {}", userRequest.getEmail());
        user.setUsername(userRequest.getUsername());
        user.setEmail(userRequest.getEmail());
        user.setPhoneNumber(userRequest.getPhoneNumber());
        user.setCompany(userRequest.getCompany());
        user.setRole(userRequest.getRole());
        user.setPassword(passwordEncoder.encode(otp));
        user.setCreatedDate(Timestamp.from(Instant.now()));
        user.setIsActive(false);
        user.setIsPasswordChanged(false);
        // send email

        UserActivity userActivityLog = new UserActivity();
        userActivityLog.setActivity("CREATE USER");
        utilService.saveUserActivity(userActivityLog);
        userRepository.save(user);

        Map<String, Object> emailBody = new HashMap<>();
        emailBody.put("Username", user.getUsername());
        emailBody.put("OTP", otp);

        utilService.sendEmail(
                user.getEmail(), emailBody
        );
        // send sms
        utilService.sendSms(user.getPhoneNumber(), String.format("Dear %s, here is your OTP: %s", user.getUsername(), otp));

    }

    // get Users
    public List<UserResponse> fetchUsers() {
        List<Client> users = userRepository.findAll();

        List<UserResponse> userList = users.stream().map(user -> {
            var userResponse = new UserResponse();
            userResponse.setId(user.getId());
            userResponse.setUsername(user.getUsername());
            userResponse.setEmail(user.getEmail());
            userResponse.setRole(user.getRole());
            userResponse.setIsActive(user.getIsActive());
            userResponse.setIsPasswordChanged(user.getIsPasswordChanged());
            userResponse.setCreatedDate(user.getCreatedDate());
            return userResponse;
        }).collect(Collectors.toList());
        UserActivity userActivityLog = new UserActivity();
        userActivityLog.setActivity("VIEW USERS");
        utilService.saveUserActivity(userActivityLog);
        return userList;
    }

    // change password
    public Client changePassword(ChangePasswordRequest request, int userId) throws CustomException {

        Optional<Client> client = userRepository.findById(userId);

        UserActivity userActivityLog = new UserActivity();

        if (!client.isPresent())
            throw new CustomException("User with id " + userId + " not found");

        boolean validPassword = request.getPassword()
                .matches(regex) && passwordEncoder.matches(request.getPassword(), passwordEncoder.encode(request.getPassword()));

        if (!validPassword) {
            userActivityLog.setActivity("INVALID PASSWORD");
            utilService.saveUserActivity(userActivityLog);
            throw new CustomException("Password is invalid");
        }

        client.get().setPassword(passwordEncoder.encode(request.getPassword()));
        client.get().setIsActive(true);
        client.get().setIsPasswordChanged(true);

        userActivityLog.setActivity("CHANGE PASSWORD");
        utilService.saveUserActivity(userActivityLog);
        return userRepository.save(client.get());
    }

    // forgot password
    public Client forgotPassword(String email, String phoneNumber) throws CustomException {

        Client clientByEmail = userRepository.findClientByEmail(email);

        if (clientByEmail == null) {
            throw new CustomException("user not found");
        }

        // generate otp
        String otp = utilService.generateOtp();

        Map<String, Object> emailBody = new HashMap<>();

        emailBody.put("Username", clientByEmail.getUsername());
        emailBody.put("OTP", otp);

        log.info("FORGOT PASSWORD OTP :{}", otp);

        utilService.sendEmail(clientByEmail.getEmail(), emailBody);

        utilService.sendSms(phoneNumber, String.format("Dear %s, here is your OTP: %s", clientByEmail.getUsername(), otp));

        clientByEmail.setPassword(passwordEncoder.encode(otp));
        clientByEmail.setIsActive(false);
        clientByEmail.setIsPasswordChanged(false);

        var userActivityLog = new UserActivity();
        userActivityLog.setActivity("FORGOT PASSWORD");
        utilService.saveUserActivity(userActivityLog);

        return userRepository.save(clientByEmail);
    }

    public void deactivateUser(int userId) throws CustomException {

        Optional<Client> user = userRepository.findById(userId);

        if (!user.isPresent()) {
            throw new CustomException("User with id " + userId + " not found");
        }

        user.get().setIsActive(false);
        userRepository.save(user.get());

    }

    public List<UserActivity> getUserActivity() {
        var userActivityLog = new UserActivity();
        userActivityLog.setActivity("VIEW USER ACTIVITY");
        utilService.saveUserActivity(userActivityLog);
        return userActivityRepository.findAll();
    }
}
