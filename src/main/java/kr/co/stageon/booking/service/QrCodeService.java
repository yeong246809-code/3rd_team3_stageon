package kr.co.stageon.booking.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * 모바일 티켓의 QR 코드를 생성하는 서비스입니다.
 *
 * QR 내용을 문자열로 전달받아 PNG 이미지를 생성하고,
 * HTML에서 바로 출력할 수 있도록 Base64 문자열로 반환합니다.
 */
@Service
public class QrCodeService {

    // QR 이미지 크기
    private static final int QR_SIZE = 220;


    /**
     * QR 코드 이미지를 Base64 형식으로 생성합니다.
     *
     * 예)
     * STAGEON:TICKET:31
     *
     * ↓
     *
     * data:image/png;base64,...
     */
    public String generateQrCode(String content) {

        try {
            // 1. QR 코드 생성기
            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            // 2. 전달받은 문자열을 QR 패턴으로 변환
            BitMatrix bitMatrix = qrCodeWriter.encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    QR_SIZE,
                    QR_SIZE
            );

            // 3. QR 이미지를 PNG 데이터로 변환
            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            MatrixToImageWriter.writeToStream(
                    bitMatrix,
                    "PNG",
                    outputStream
            );

            // 4. PNG 이미지를 Base64 문자열로 변환
            String base64Image = Base64.getEncoder()
                    .encodeToString(outputStream.toByteArray());

            // 5. HTML img src에 바로 넣을 수 있는 형식으로 반환
            return "data:image/png;base64," + base64Image;

        } catch (WriterException | IOException e) {

            throw new IllegalStateException(
                    "QR 코드 생성 중 오류가 발생했습니다.",
                    e
            );
        }
    }
}