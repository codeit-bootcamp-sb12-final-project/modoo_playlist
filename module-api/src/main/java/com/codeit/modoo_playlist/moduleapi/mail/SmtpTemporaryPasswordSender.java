package com.codeit.modoo_playlist.moduleapi.mail;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.TemporaryPasswordSender;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SmtpTemporaryPasswordSender implements TemporaryPasswordSender {

  private final JavaMailSender mailSender;

  @Value("${module-api.mail.from}")
  private String from;

  @Override
  public void send(String email, String temporaryPassword, Instant expiresAt) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(from);
    message.setTo(email);
    message.setSubject("[모두의 플레이리스트] 임시 비밀번호 안내");
    message.setText("""
        비밀번호 초기화 요청에 따라 임시 비밀번호가 발급되었습니다.
        
        임시 비밀번호: %s
        
        임시 비밀번호는 발급 후 3분 동안 사용할 수 있습니다.
        로그인 후 새로운 비밀번호로 변경해 주세요.
        만료 시각: %s
        
        본인이 요청하지 않았다면 이 메일을 무시해 주세요.
        """.formatted(temporaryPassword, expiresAt));

    try {
      mailSender.send(message);
    } catch (MailException exception) {
      throw new BaseException(ErrorCode.EMAIL_SEND_FAILED, exception);
    }
  }
}
