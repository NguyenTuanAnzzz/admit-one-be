package com.an.admit_one.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

// Tạo ảnh QR PNG cho email. Email không chạy JavaScript nên không vẽ QR ở trình duyệt như FE được
@Service
public class QrCodeService {

    // Chuỗi → ảnh QR PNG (mảng byte). size: cạnh ảnh tính bằng pixel
    public byte[] toPng(String text, int size) {
        try {
            // MARGIN 1: viền trắng mỏng quanh mã, máy quét vẫn đọc tốt
            BitMatrix matrix = new QRCodeWriter().encode(
                    text,
                    BarcodeFormat.QR_CODE,
                    size,
                    size,
                    Map.of(EncodeHintType.MARGIN, 1)
            );

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();

        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Không tạo được mã QR", e);
        }
    }
}
