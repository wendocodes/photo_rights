package com.klix.backend.service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;

import lombok.extern.slf4j.Slf4j;

/**
 * Email Sender Service
 */
@Service
@Slf4j
public class EmailService {
    private JavaMailSender javaMailSender;

    @Autowired
    public EmailService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Async
    public void sendEmail(SimpleMailMessage email) {
        javaMailSender.send(email);
    }

    /**
     * Send an email to clientadmin after login request from logged in user
     */

    public void sendmailToParent(String emailto, long pin) {

        // create a message
        SimpleMailMessage msg = new SimpleMailMessage();
        String subject = "Client Login";
        String body = "Hallo Eltern, \nDer Anmeldevorgang für Ihr Kind ist nun abgeschlossen. Bitte laden Sie die Klix-App herunter und geben Sie die unten stehende "
                + pin + " ein, um sich in unserer Einrichtung anzumelden. \nMit freundlichen Grüßen, \nKlix Bot";

        try {
            // set receiver
            msg.setTo(emailto);
            // set subject and body
            msg.setSubject(subject);
            msg.setText(body);
            // send mail
            javaMailSender.send(msg);
        } catch (MailException mailException) {
            mailException.printStackTrace();
        }

    }

    /**
     * Send an email to user to reset password
     */

    public void sendPasswordResetMail(String emailto, String link) {

        // create a message
        SimpleMailMessage msg = new SimpleMailMessage();

        String subject = "Passwort zurückzusetzen.";
        ;

        String body = "Hello, Sie haben beantragt, Ihr Passwort zurückzusetzen.\nKlicken Sie auf den unten stehenden Link, um Ihr Passwort zu ändern: "
                + link
                + " Ignorieren Sie diese E-Mail, wenn Sie sich an Ihr Passwort erinnern können, oder Sie haben den Antrag nicht gestellt. Mit freundlichen Grüßen, \nKlix Team";

        try {
            // set receiver
            msg.setTo(emailto);
            // set subject and body
            msg.setSubject(subject);
            msg.setText(body);
            // send mail
            javaMailSender.send(msg);
        } catch (MailException mailException) {
            mailException.printStackTrace();
        }

    }

    /**
     * Send a publication request email
     */

    public void sendPubRequestEmail(String emailto, String pubRequestMessage, String emailSub) {

        SimpleMailMessage msg = new SimpleMailMessage();

        String subject = emailSub;

        String body = pubRequestMessage;

        try {
            msg.setTo(emailto);
            msg.setSubject(subject);
            msg.setText(body);
            javaMailSender.send(msg);
        } catch (MailException mailException) {
            mailException.printStackTrace();
        }

    }
}
