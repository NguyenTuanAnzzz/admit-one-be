package com.an.admit_one.controller;

import com.an.admit_one.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.PayOS;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
public class PaymentWebhookController {

    @Autowired
    private PayOS payOS;

    @Autowired
    private PaymentService paymentService;

    // payOS gọi vào đây khi có tiền vào. Không dùng JWT, xác thực bằng chữ ký (checksum key)
    @PostMapping("/payos")
    public ResponseEntity<Map<String, Object>> payos(@RequestBody String rawBody) throws Exception {

        WebhookData data;
        try {
            // verify nhận thẳng chuỗi JSON: SDK tự parse (bằng Jackson 2 của nó, không cần ObjectMapper của Spring)
            // rồi tính lại chữ ký bằng checksum key, sai là ném lỗi
            data = payOS.webhooks().verify(rawBody);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false));
        }

        // code "00": giao dịch thành công
        if (!"00".equals(data.getCode())) {
            return ResponseEntity.ok(Map.of("success", true));
        }

        try {
            paymentService.handleTransfer(
                    "PAYOS",
                    "PAYOS-" + data.getReference(),
                    BigDecimal.valueOf(data.getAmount()),
                    // Ghép lại mã đơn: handleTransfer tìm "AO..." trong chuỗi này
                    "AO" + data.getOrderCode(),
                    rawBody
            );
        } catch (DataIntegrityViolationException e) {
            // payOS gửi lại giao dịch đã xử lý: bị chặn ở cột UNIQUE gateway_txn_ref
        }

        return ResponseEntity.ok(Map.of("success", true));
    }
}