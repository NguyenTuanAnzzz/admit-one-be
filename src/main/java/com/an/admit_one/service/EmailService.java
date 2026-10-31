package com.an.admit_one.service;

import com.an.admit_one.enums.OtpType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    // Thời hạn mã OTP, AuthService dùng chung hằng số này khi đặt expiresAt
    public static final int OTP_VALID_MINUTES = 1;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Nội dung của một email. Bố cục (logo, thẻ, ô mã, chân trang) dùng chung cho mọi email,
     * mỗi loại email chỉ cần tạo một EmailContent.
     *
     * @param code mã hiển thị trong ô nổi bật, null nếu email không có mã
     * @param note dòng lưu ý trên nền cam nhạt, null nếu không cần
     */
    public record EmailContent(
            String subject,
            String heading,
            String intro,
            String code,
            String note,
            String ignoreLine,
            String reason
    ) {
    }

    public void sendOtpEmail(String toEmail, String otpCode) {
        sendOtpEmail(toEmail, otpCode, OtpType.VERIFY_EMAIL);
    }

    public void sendOtpEmail(String toEmail, String otpCode, OtpType type) {
        String note = "Mã có hiệu lực trong " + OTP_VALID_MINUTES + " phút. Không chia sẻ mã này với bất kỳ ai.";

        EmailContent content = switch (type) {
            case VERIFY_EMAIL -> new EmailContent(
                    "Mã xác minh tài khoản Admit One",
                    "Xác minh email của bạn",
                    "Cảm ơn bạn đã đăng ký tài khoản Admit One. Nhập mã bên dưới để xác minh địa chỉ email và bắt đầu đặt vé.",
                    otpCode,
                    note,
                    "Nếu bạn không thực hiện đăng ký, vui lòng bỏ qua email này.",
                    "Bạn nhận được email này vì vừa đăng ký tài khoản Admit One bằng địa chỉ này."
            );
            case RESET_PASSWORD -> new EmailContent(
                    "Mã đặt lại mật khẩu Admit One",
                    "Đặt lại mật khẩu",
                    "Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản Admit One của bạn. Nhập mã bên dưới để tạo mật khẩu mới.",
                    otpCode,
                    note,
                    "Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này. Mật khẩu hiện tại của bạn vẫn giữ nguyên.",
                    "Bạn nhận được email này vì có yêu cầu đặt lại mật khẩu cho tài khoản dùng địa chỉ này."
            );
        };

        send(toEmail, content);
    }

    public void send(String toEmail, EmailContent content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // multipart = true: gửi kèm bản chữ thường cho trình đọc mail không hiển thị HTML
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(content.subject());
            helper.setText(renderText(content), renderHtml(content));

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new MailPreparationException("Không tạo được email: " + content.subject(), e);
        }
    }

    private String renderText(EmailContent c) {
        StringBuilder text = new StringBuilder("Xin chào,\n\n").append(c.intro()).append("\n\n");
        if (c.code() != null) {
            text.append(c.code()).append("\n\n");
        }
        if (c.note() != null) {
            text.append(c.note()).append("\n");
        }
        return text.append(c.ignoreLine()).append("\n\n")
                .append("Trân trọng,\nĐội ngũ Admit One")
                .toString();
    }

    // Màu lấy từ admit-one-fe/tailwind.config.js. Email client bỏ thẻ <style>, nên CSS viết inline.
    private static final String FONT_BODY = "Inter,'Segoe UI',Roboto,Arial,sans-serif";
    private static final String FONT_DISPLAY = "'Bricolage Grotesque'," + FONT_BODY;

    private static final String LAYOUT = """
            <!doctype html>
            <html lang="vi">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>{{subject}}</title>
            </head>
            <body style="margin:0;padding:0;background-color:#f6f5f3;">
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f6f5f3;">
                <tr>
                  <td align="center" style="padding:40px 16px;">
                    <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="max-width:560px;">
                      <tr>
                        <td style="padding:0 4px 20px;font-family:{{fontDisplay}};font-size:24px;font-weight:800;letter-spacing:-0.01em;color:#cf3a20;">
                          admit one
                        </td>
                      </tr>
                      <tr>
                        <td style="background-color:#ffffff;border:1px solid #e4e1dc;border-radius:14px;padding:40px 32px;font-family:{{fontBody}};color:#1d1a17;">
                          <h1 style="margin:0 0 16px;font-family:{{fontDisplay}};font-size:28px;line-height:1.15;letter-spacing:-0.02em;font-weight:800;color:#1d1a17;">
                            {{heading}}
                          </h1>
                          <p style="margin:0 0 12px;font-size:16px;line-height:1.55;color:#1d1a17;">Xin chào,</p>
                          <p style="margin:0;font-size:16px;line-height:1.55;color:#1d1a17;">{{intro}}</p>
                          {{code}}
                          {{note}}
                          <hr style="margin:32px 0 20px;border:0;border-top:1px solid #e4e1dc;">
                          <p style="margin:0;font-size:14px;line-height:1.5;color:#67615b;">{{ignoreLine}}</p>
                          <p style="margin:16px 0 0;font-size:14px;line-height:1.5;color:#1d1a17;">
                            Trân trọng,<br>Đội ngũ Admit One
                          </p>
                        </td>
                      </tr>
                      <tr>
                        <td align="center" style="padding:20px 16px 0;font-family:{{fontBody}};font-size:12px;line-height:1.45;color:#67615b;">
                          {{reason}}
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """;

    // Ô mã: nền xám ấm như skeleton trên web, chữ to giãn cách để dễ đọc và gõ lại
    private static final String CODE_BLOCK = """
            <p style="margin:28px 0 0;padding:20px 16px;background-color:#efede9;border:1px solid #e4e1dc;border-radius:8px;text-align:center;font-family:{{fontDisplay}};font-size:32px;line-height:1;font-weight:800;letter-spacing:0.3em;color:#1d1a17;">{{value}}</p>
            """;

    private static final String NOTE_BLOCK = """
            <p style="margin:20px 0 0;padding:12px 16px;background-color:#fdebe6;border-radius:8px;font-size:14px;line-height:1.5;color:#b2331c;">{{value}}</p>
            """;

    private String renderHtml(EmailContent c) {
        return LAYOUT
                .replace("{{code}}", block(CODE_BLOCK, c.code()))
                .replace("{{note}}", block(NOTE_BLOCK, c.note()))
                .replace("{{subject}}", escape(c.subject()))
                .replace("{{heading}}", escape(c.heading()))
                .replace("{{intro}}", escape(c.intro()))
                .replace("{{ignoreLine}}", escape(c.ignoreLine()))
                .replace("{{reason}}", escape(c.reason()))
                .replace("{{fontDisplay}}", FONT_DISPLAY)
                .replace("{{fontBody}}", FONT_BODY);
    }

    private String block(String template, String value) {
        return value == null ? "" : template.replace("{{value}}", escape(value));
    }

    private String escape(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value, StandardCharsets.UTF_8.name());
    }
}
