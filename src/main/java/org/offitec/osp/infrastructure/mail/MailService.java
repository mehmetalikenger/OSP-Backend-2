package org.offitec.osp.infrastructure.mail;

import org.springframework.stereotype.Component;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.http.HttpResponse;

@Component
public class MailService {

    public JsonNode sendActivationEmail(String email, String token) throws UnirestException {
    
        String apiKey = System.getenv("API_KEY");
        if (apiKey == null) {
            apiKey = "***REMOVED***";
        }

        HttpResponse<JsonNode> request = Unirest.post("https://api.mailgun.net/v3/pinextra.com/messages")
                .basicAuth("api", apiKey)
                .queryString("from", "OSP Support <no-reply@pinextra.com>")
                .queryString("to", email)
                .queryString("subject", "Account Activation")
                .queryString("text", "Please activate your account by clicking the following link: http://localhost:3000/activate?token=" + token)
                .queryString("html", buildHtmlTemplate("Account Activation", "Welcome to OSP! Please activate your account by clicking the button below:", "http://localhost:3000/activate?token=" + token, "Activate Account"))
                .asJson();



        return request.getBody();
    }

    public JsonNode sendForgotPasswordEmail(String email, String token) throws UnirestException {
        String apiKey = System.getenv("API_KEY");
        if (apiKey == null) {
            apiKey = "***REMOVED***";
        }

        HttpResponse<JsonNode> request = Unirest.post("https://api.mailgun.net/v3/pinextra.com/messages")
                .basicAuth("api", apiKey)
                .queryString("from", "OSP Support <no-reply@pinextra.com>")
                .queryString("to", email)
                .queryString("subject", "Forgot Password")
                .queryString("text", "You requested a password reset. Please click the following link to reset your password: http://localhost:3000/reset-password?token=" + token)
                .queryString("html", buildHtmlTemplate("Reset Password", "You requested a password reset. Please click the button below to reset your password:", "http://localhost:3000/reset-password?token=" + token, "Reset Password"))
                .asJson();



        return request.getBody();
    }

    public JsonNode sendAccountDeletionConfirmationEmail(String email, String token) throws UnirestException {
    
        String apiKey = System.getenv("API_KEY");
        if (apiKey == null) {
            apiKey = "***REMOVED***";
        }

        HttpResponse<JsonNode> request = Unirest.post("https://api.mailgun.net/v3/pinextra.com/messages")
                .basicAuth("api", apiKey)
                .queryString("from", "OSP Support <no-reply@pinextra.com>")
                .queryString("to", email)
                .queryString("subject", "Delete Account")
                .queryString("text", "You requested to delete your account. Please click the following link to confirm: http://localhost:3000/delete-account?token=" + token)
                .queryString("html", buildHtmlTemplate("Delete Account", "You requested to delete your account. Please click the button below to confirm this action. This cannot be undone.", "http://localhost:3000/delete-account?token=" + token, "Confirm Deletion"))
                .asJson();



        return request.getBody();
    }

    private String buildHtmlTemplate(String title, String message, String actionUrl, String actionText) {
        String baseUrl = "http://localhost:3000";
        return String.format(
            "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "    <style>" +
            "        body { font-family: Arial, sans-serif; background-color: #f4f7f6; margin: 0; padding: 0; }" +
            "        .container { max-width: 600px; margin: 40px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 10px rgba(0,0,0,0.05); }" +
            "        .header { background-color: #ffffff; padding: 20px; text-align: center; border-bottom: 4px solid #282f68; }" +
            "        .header img { max-width: 180px; }" +
            "        .content { padding: 40px 30px; text-align: center; color: #333333; }" +
            "        .content h2 { color: #282f68; margin-top: 0; font-size: 24px; }" +
            "        .content p { font-size: 16px; line-height: 1.5; color: #555555; margin-bottom: 30px; }" +
            "        .button { display: inline-block; padding: 12px 30px; background-color: #d7292e; color: #ffffff !important; text-decoration: none; border-radius: 5px; font-weight: bold; font-size: 16px; }" +
            "        .raw-url { margin-top: 30px; font-size: 13px; color: #888888; word-break: break-all; }" +
            "        .raw-url a { color: #282f68; }" +
            "        .footer { background-color: #f9f9f9; padding: 20px; text-align: center; border-top: 1px solid #eeeeee; }" +
            "        .footer img { max-width: 120px; margin-bottom: 10px; }" +
            "        .footer p { margin: 0; font-size: 13px; color: #888888; }" +
            "    </style>" +
            "</head>" +
            "<body>" +
            "    <div class=\"container\">" +
            "        <div class=\"header\">" +
            "            <img src=\"%s/logo/logo-1-v1.png\" alt=\"OSP Logo\" />" +
            "        </div>" +
            "        <div class=\"content\">" +
            "            <h2>%s</h2>" +
            "            <p>%s</p>" +
            "            <a href=\"%s\" class=\"button\">%s</a>" +
            "            <p class=\"raw-url\">Or copy and paste this link into your browser:<br/><a href=\"%s\">%s</a></p>" +
            "        </div>" +
            "        <div class=\"footer\">" +
            "            <img src=\"%s/logo/logo.png\" alt=\"OffiTec Logo\" />" +
            "            <p><strong>OSP</strong> (OffiTec Selection Software)</p>" +
            "            <p>&copy; OffiTec. All rights reserved.</p>" +
            "        </div>" +
            "    </div>" +
            "</body>" +
            "</html>",
            baseUrl, title, message, actionUrl, actionText, actionUrl, actionUrl, baseUrl
        );
    }
}
