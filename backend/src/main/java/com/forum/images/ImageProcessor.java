package com.forum.images;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Component;

import com.forum.rules.ForumPolicy;
import com.forum.shared.error.ApiException;

/**
 * Valida el tipo real de la imagen por su contenido (magic bytes) y la re-codifica para eliminar
 * metadatos (EXIF, incluida la ubicación GPS). JPEG y PNG se re-codifican en su mismo formato;
 * WebP se normaliza a PNG (sin pérdida) porque Java puro no puede escribir WebP.
 */
@Component
public class ImageProcessor {

    private static final String JPEG = "image/jpeg";
    private static final String PNG = "image/png";
    private static final String WEBP = "image/webp";

    private final ForumPolicy policy;

    ImageProcessor(ForumPolicy policy) {
        this.policy = policy;
    }

    public ProcessedImage process(byte[] input) {
        String detected = detectType(input);
        if (detected == null || !policy.allowedImageTypes().contains(detected)) {
            throw ApiException.unsupportedMediaType("El archivo no es una imagen de un tipo permitido");
        }

        BufferedImage image = decode(input);
        if (image == null) {
            throw ApiException.unsupportedMediaType("No se pudo decodificar la imagen");
        }

        // WebP no se puede escribir en Java puro: se normaliza a PNG (sin pérdida).
        String outputFormat = detected.equals(JPEG) ? "jpeg" : "png";
        String outputContentType = detected.equals(JPEG) ? JPEG : PNG;
        String extension = detected.equals(JPEG) ? "jpg" : "png";

        byte[] reencoded = encode(image, outputFormat);
        return new ProcessedImage(reencoded, outputContentType, extension);
    }

    private BufferedImage decode(byte[] input) {
        try {
            return ImageIO.read(new ByteArrayInputStream(input));
        } catch (IOException e) {
            return null;
        }
    }

    private byte[] encode(BufferedImage image, String format) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (!ImageIO.write(image, format, out)) {
                throw ApiException.unsupportedMediaType("No hay codificador disponible para el formato " + format);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw ApiException.unsupportedMediaType("No se pudo procesar la imagen");
        }
    }

    /** Detecta el tipo por magic bytes; devuelve null si no es jpeg/png/webp. */
    private String detectType(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return JPEG;
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == '\r' && b[5] == '\n' && (b[6] & 0xFF) == 0x1A && b[7] == '\n') {
            return PNG;
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return WEBP;
        }
        return null;
    }

    public record ProcessedImage(byte[] data, String contentType, String extension) {
    }
}
