package org.offitec.osp.application.service;

import org.offitec.osp.domain.data.AdminRegisterData;
import org.offitec.osp.domain.data.UserRegisterData;
import org.offitec.osp.domain.service.UserRegisterService;
import org.offitec.osp.infrastructure.mail.MailService;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.offitec.osp.presentation.dto.AdminRegisterDTO;
import org.offitec.osp.presentation.dto.UserRegisterDTO;
import org.springframework.stereotype.Service;
import org.offitec.osp.infrastructure.security.JwtService;

@Service
public class AdminRegisterAppService {

    private final UserRegisterService userRegisterService;
    private final JwtService jwtService;
    private final MailService mailService;

    public AdminRegisterAppService(UserRegisterService userRegisterService, JwtService jwtService, MailService mailService){
        this.userRegisterService = userRegisterService;
        this.jwtService = jwtService;
        this.mailService = mailService;
    }

    public void AdminRegister(AdminRegisterDTO dto, String adminEmail){

        AdminRegisterData data = new AdminRegisterData(dto.getEmail(), adminEmail);
        boolean requiresActivation = userRegisterService.AdminRegister(data);

        if (requiresActivation) {
            try {
                String token = jwtService.generateActivationToken(dto.getEmail(), 2592000); // 30 days
                mailService.sendActivationEmail(dto.getEmail(), token);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void UserRegister(UserRegisterDTO dto, String adminEmail){

        UserRegisterData data = new UserRegisterData(dto.getEmail(), dto.getCategory(), adminEmail);
        boolean requiresActivation = userRegisterService.UserRegister(data);

        if (requiresActivation) {
            try {
                String token = jwtService.generateActivationToken(dto.getEmail(), 2592000); // 30 days
                mailService.sendActivationEmail(dto.getEmail(), token);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
