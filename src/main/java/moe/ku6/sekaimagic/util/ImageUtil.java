package moe.ku6.sekaimagic.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public class ImageUtil {

    public static BufferedImage DecodeBase64ToImage(String base64String) throws IOException {
        byte[] imageBytes = Base64.getDecoder().decode(base64String);
        return ImageIO.read(new ByteArrayInputStream(imageBytes));
    }

    public static BufferedImage DecodeByteArrayToImage(byte[] imageBytes) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(imageBytes));
    }

    public static BufferedImage CropToSquare(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int minSide = Math.min(width, height);
        int x = (width - minSide) / 2;
        int y = (height - minSide) / 2;
        return image.getSubimage(x, y, minSide, minSide);
    }

    public static BufferedImage ResizeImage(BufferedImage image, int width, int height) {
        BufferedImage resizedImage = new BufferedImage(width, height, image.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();
        return resizedImage;
    }

    public static byte[] EncodeImage(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }
}