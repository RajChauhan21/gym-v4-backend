package com.backend.gym_backend.service;

import com.backend.gym_backend.entity.OTP;
import com.backend.gym_backend.repo.OtpRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class OtpEmailService {

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private JavaMailSender mailSender;

//    public ResponseEntity<?> sendPasswordRestOtp(String toEmail){
//        int otp = 100000 + new SecureRandom().nextInt(900000);
//
//        //save in db
//        OTP build = OTP.builder()
//                .otp(String.valueOf(otp))
//                .sentAt(LocalDateTime.now())
//                .expiresAt(LocalDateTime.now().plusMinutes(5))
//                .ownerEmail(toEmail)
//                .verified(false)
//                .build();
//        otpRepository.save(build);
//
//        //send email
//        SimpleMailMessage mailMessage = new SimpleMailMessage();
//        mailMessage.setTo(toEmail);
//        mailMessage.setCc("abc@gmail.com");
//        mailMessage.setSubject("Password Reset Request");
//        mailMessage.setText("Please find below on otp to complete your password reset request");
//        mailMessage.setText("Note - OTP will be valid for only 5 minutes");
//        mailMessage.setText(String.valueOf(otp));
//        mailSender.send(mailMessage);
//        return new ResponseEntity<>("E-mail sent on provided email id", HttpStatus.ACCEPTED);
//    }

    @Transactional
    public ResponseEntity<?> sendPasswordResetOtp(String toEmail) {

        int otp = 100000 + new SecureRandom().nextInt(900000);

        // delete old otp if exists
        otpRepository.deleteByOwnerEmail(toEmail);
        otpRepository.flush();

        OTP build = OTP.builder()
                .otp(String.valueOf(otp))
                .sentAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .ownerEmail(toEmail)
                .isVerified(false)
                .build();

        otpRepository.save(build);

        SimpleMailMessage mailMessage = new SimpleMailMessage();

        mailMessage.setTo(toEmail);

        mailMessage.setSubject("Password Reset Request");

        mailMessage.setText(
                "Your password reset OTP is: " + otp +
                        "\n\nThis OTP is valid for 5 minutes."
        );

        mailSender.send(mailMessage);

        return new ResponseEntity<>(
                "OTP sent successfully",
                HttpStatus.ACCEPTED
        );
    }

    public ResponseEntity<?> verifyOtp(String email, String otp) {

        Optional<OTP> otpObject = otpRepository.findByOwnerEmail(email);

        if (otpObject.isEmpty()) {
            log.info("email not found {}",email);
            return new ResponseEntity<>(
                    "Email not found",
                    HttpStatus.ACCEPTED
            );
        }

        OTP savedOtp = otpObject.get();

        // already used
        if (savedOtp.getIsVerified()) {
            log.info("otp already used");
            return new ResponseEntity<>(
                    "OTP Expired",
                    HttpStatus.ACCEPTED
            );
        }

        // expired
        if (savedOtp.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.info("otp already expired");
            return new ResponseEntity<>(
                    "OTP Expired",
                    HttpStatus.ACCEPTED
            );
        }

        // wrong otp
        if (!savedOtp.getOtp().equals(otp)) {
            log.info("otp is wrong");
            return new ResponseEntity<>(
                    "OTP is wrong",
                    HttpStatus.ACCEPTED
            );
        }

        // mark as used
        savedOtp.setIsVerified(true);

        log.info("successfully verified otp");

        otpRepository.save(savedOtp);

        return new ResponseEntity<>(
                "201",
                HttpStatus.ACCEPTED
        );
    }

//    public boolean verifyOtp(String email, String otp){
//        Optional<OTP> otpObject = otpRepository.findByOwnerEmail(email);
//        if (otpObject.get().getOtp().equals(String.valueOf(otp)) && otpObject.get().getExpiresAt().isAfter(LocalDateTime.now())){
//            return true;
//        }
//        return false;
//    }



    public ResponseEntity<?> sendEmailForSignUp(String to, String cc){
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(to);
        mailMessage.setCc(cc);
        mailMessage.setSubject("SignUp Successful");
        mailMessage.setText("Welcome to ShopRi, we wish you will have a great user experience through out our website");
        mailSender.send(mailMessage);

        return new ResponseEntity<>("E-mail sent on provided email id", HttpStatus.ACCEPTED);
    }

    public ResponseEntity<?> sendEmailForSignIn(String to, String cc){
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(to);
        mailMessage.setCc(cc);
        mailMessage.setSubject("SignIn Successful");
        mailMessage.setText("Hey!! welcome back, we missed you!! Let's back to shopping.....");
        mailSender.send(mailMessage);

        return new ResponseEntity<>("E-mail sent on provided email id", HttpStatus.ACCEPTED);
    }

    public void sendSubscriptionCancellationEmail(
            String toEmail,
            String ownerName,
            String currentPlan,
            LocalDate subscriptionEndDate
    ) {

        String subject = "Your Subscription Has Been Cancelled";

        String body = """
            Hi %s,

            Your %s subscription has been cancelled successfully.

            You will continue to have access to your plan benefits until %s.

            After this date, your subscription will expire automatically and billing will stop.

            If this was a mistake, you can subscribe again anytime from your dashboard.

            Thank you for using our platform.

            Regards,
            Gym SaaS Team
            """
                .formatted(
                        ownerName,
                        currentPlan,
                        subscriptionEndDate
                );

        SimpleMailMessage mailMessage = new SimpleMailMessage();

        mailMessage.setTo(toEmail);

        mailMessage.setSubject(subject);

        mailMessage.setText(body);

        mailSender.send(mailMessage);
    }

    public void sendSubscriptionUpgradeEmail(
            String toEmail,
            String ownerName,
            String oldPlan,
            String newPlan,
            LocalDate effectiveDate
    ) {

        String subject = "Your Subscription Has Been Upgraded";

        String body = """
            Hi %s,

            Great news! Your subscription has been upgraded successfully.

            Previous Plan : %s
            New Plan      : %s

            The upgraded plan will become active on %s.

            You can now enjoy additional features and benefits included in your new plan.

            Thank you for growing with us.

            Regards,
            Gym SaaS Team
            """
                .formatted(
                        ownerName,
                        oldPlan,
                        newPlan,
                        effectiveDate
                );

        SimpleMailMessage mailMessage = new SimpleMailMessage();

        mailMessage.setTo(toEmail);

        mailMessage.setSubject(subject);

        mailMessage.setText(body);

        mailSender.send(mailMessage);
    }


}
