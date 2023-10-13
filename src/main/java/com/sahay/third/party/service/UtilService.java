package com.sahay.third.party.service;


import com.sahay.third.party.model.UserActivity;
import com.sahay.third.party.repo.UserActivityRepository;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.RequestBuilder;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import javax.mail.MessagingException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class UtilService {

    @Value("${org.app.properties.sms.endpoint}")
    private String SMS_URL;
    @Value("${spring.mail.username}")
    private String FROM_EMAIL;

    @Value("${org.app.properties.otp-characters}")
    private String characters;

    private final Configuration configuration;

    private final JavaMailSender mailSender;

    private final HttpProcessor httpProcessor;

    private final UserActivityRepository userActivityRepository;



    public String generateOtp() {
        Random random = new Random();

        String otp = "";

        for (int i = 0; i < 8; i++) {
            int index = random.nextInt(characters.length());
            otp += characters.charAt(index);
        }
        return otp;
    }

    // TODO: 4/7/2023 email

    public void sendEmail(String toEmail, Map<String, Object> body) {

        var message = mailSender.createMimeMessage();
        try {
            var messageHelper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_RELATED,
                    StandardCharsets.UTF_8.name());

            Template template = configuration
                    .getTemplate("email-template.ftl");

            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, body);
            messageHelper.setFrom(FROM_EMAIL);
            messageHelper.setSubject("One Time Password");
            messageHelper.setTo(toEmail);

            messageHelper.setText(html, true);
            mailSender.send(message);

            log.info("EMAIL SENT : {}", messageHelper);

        } catch (IOException | TemplateException | MessagingException e) {
            e.printStackTrace();
        }


    }


    // TODO: 4/7/2023 sms 

    public void sendSms(String msIsdn, String message) {

        var smsRequest = new JSONObject();
        smsRequest.put("username", "channel");
        smsRequest.put("password", "$_@C0NNEKT");
        smsRequest.put("messageType", "1200");
        smsRequest.put("transactionId", "1200560161");
        smsRequest.put("transactionType", "NOT");
        smsRequest.put("alertType", "GEN");
        smsRequest.put("serviceCode", "290");
        smsRequest.put("toMsisdn", msIsdn);
        smsRequest.put("message", message);

        RequestBuilder payload = new RequestBuilder("POST");
        payload
                .addHeader("Content-Type", "application/json")
                .setBody(smsRequest.toString())
                .setUrl(SMS_URL)
                .build();
        log.info("SMS request : {}", smsRequest);
        JSONObject smsResponse = httpProcessor.processProperRequest(payload);
        log.info("SMS response : {}", smsResponse);

    }

    public String getTheCurrentLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    public void saveUserActivity(UserActivity userActivity) {
        userActivity.setActivity(userActivity.getActivity());
        if (userActivity.getUsername() != null) {
            userActivity.setUsername(userActivity.getUsername());
        } else {
            userActivity.setUsername(getTheCurrentLoggedInUser());
        }
        userActivity.setActivityTime(LocalDateTime.now());
        userActivityRepository.save(userActivity);
    }
}
