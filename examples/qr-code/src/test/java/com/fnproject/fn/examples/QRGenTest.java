package com.fnproject.fn.examples;

import com.fnproject.fn.testing.FnTestingRule;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import org.junit.Rule;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class QRGenTest {
    @Rule
    public FnTestingRule fn = FnTestingRule.createDefault();

    @Test
    public void textHelloWorld() throws Exception {
        String content = "hello world";
        fn.givenEvent()
                .withRequestUrl("http://www.example.com/qr")
                .withQueryParameter("contents", content)
                .withQueryParameter("format", "png")
                .withMethod("GET")
                .enqueue();
        fn.thenRun(QRGen.class, "create");

        assertEquals(content, decode(fn.getOnlyResult().getBodyAsBytes()));
    }

    @Test
    public void phoneNumber() throws Exception {
        String telephoneNumber = "tel:0-12345-67890";
        fn.givenEvent()
                .withRequestUrl("http://www.example.com/qr")
                .withQueryParameter("contents", telephoneNumber)
                .withMethod("GET")
                .enqueue();
        fn.thenRun(QRGen.class, "create");

        assertEquals(telephoneNumber, decode(fn.getOnlyResult().getBodyAsBytes()));
    }

    @Test
    public void formatConfigurationIsUsedIfNoFormatIsProvided() throws Exception {
        String contents = "hello world";
        fn.setConfig("FORMAT", "jpg");
        fn.givenEvent()
                .withRequestUrl("http://www.example.com/qr")
                .withQueryParameter("contents", contents)
                .withMethod("GET")
                .enqueue();
        fn.thenRun(QRGen.class, "create");

        assertEquals(contents, decode(fn.getOnlyResult().getBodyAsBytes()));
    }
    private String decode(byte[] imageBytes) throws IOException, NotFoundException, ChecksumException, FormatException {
        BinaryBitmap bitmap = readToBitmap(imageBytes);
        return new QRCodeReader().decode(bitmap).getText();
    }

    private BinaryBitmap readToBitmap(byte[] imageBytes) throws IOException {
        BufferedImage inputImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
        BufferedImageLuminanceSource luminanceSource = new BufferedImageLuminanceSource(inputImage);
        HybridBinarizer binarizer = new HybridBinarizer(luminanceSource);
        return new BinaryBitmap(binarizer);
    }
}
