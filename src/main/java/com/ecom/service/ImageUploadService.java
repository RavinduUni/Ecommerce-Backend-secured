package com.ecom.service;

import com.ecom.dto.UploadedImage;
import com.ecom.exception.ApiException;
import io.imagekit.client.ImageKitClient;
import io.imagekit.errors.ImageKitException;
import io.imagekit.errors.ImageKitServiceException;
import io.imagekit.models.files.FileUploadParams;
import io.imagekit.models.files.FileUploadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/** Validates an uploaded image, sends it to ImageKit, and deletes old images. */
@Service
public class ImageUploadService {

    private static final Logger log = LoggerFactory.getLogger(ImageUploadService.class);

    /** Allowed content types and the file extension used for each. */
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif",
            "image/avif", "avif"
    );

    private final ImageKitClient imageKit;
    private final String folder;
    private final long maxBytes;

    public ImageUploadService(ImageKitClient imageKit,
                              @Value("${imagekit.folder:/products}") String folder,
                              @Value("${imagekit.max-file-size-bytes:5242880}") long maxBytes) {
        this.imageKit = imageKit;
        this.folder = folder;
        this.maxBytes = maxBytes;
    }

    /** Uploads the file to ImageKit and returns its public URL and file id. */
    public UploadedImage upload(MultipartFile file, String baseName) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Choose an image to upload.");
        }
        if (file.getSize() > maxBytes) {
            throw new ApiException(HttpStatus.CONTENT_TOO_LARGE,
                    "The image is too large. The limit is " + (maxBytes / (1024 * 1024)) + " MB.");
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String extension = ALLOWED_TYPES.get(contentType);
        if (extension == null) {
            throw ApiException.badRequest("Use a JPG, PNG, WebP, GIF or AVIF image.");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw ApiException.badRequest("The image couldn't be read. Try another file.");
        }

        // The browser-supplied content type can be faked, so check the file's real signature too.
        if (!looksLikeImage(bytes)) {
            throw ApiException.badRequest("That file isn't a valid image.");
        }

        FileUploadParams params = FileUploadParams.builder()
                .file(bytes)
                .fileName(slug(baseName) + "." + extension)
                .folder(folder)
                .useUniqueFileName(true)   // ImageKit adds a suffix so names never collide
                .addTag("product")
                .build();

        try {
            FileUploadResponse response = imageKit.files().upload(params);
            // In this SDK, url() and fileId() return Optional<String>.
            String url = response.url()
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_GATEWAY, "The image service didn't return a link."));
            String fileId = response.fileId().orElse(null);
            return new UploadedImage(url, fileId);
        } catch (ImageKitServiceException e) {
            // ImageKit answered with an error status (bad key, quota, invalid file, ...).
            log.error("ImageKit rejected the upload: status {}", e.statusCode(), e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "The image service rejected the upload. Try again.");
        } catch (ImageKitException e) {
            // Network problem, timeout, or unreadable response.
            log.error("ImageKit upload failed", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Couldn't reach the image service. Try again.");
        }
    }

    /** Deletes a file from ImageKit. Failures are logged, not thrown, so they never block a save. */
    public void deleteQuietly(String fileId) {
        if (fileId == null || fileId.isBlank()) return;
        try {
            imageKit.files().delete(fileId);
        } catch (ImageKitException e) {
            log.warn("Couldn't delete old ImageKit file {}", fileId, e);
        }
    }

    /** Checks the first bytes of the file against known image signatures. */
    private static boolean looksLikeImage(byte[] b) {
        if (b.length < 12) return false;
        boolean jpeg = (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
        boolean png = (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
        boolean gif = b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8';
        boolean webp = b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
        boolean avif = b[4] == 'f' && b[5] == 't' && b[6] == 'y' && b[7] == 'p'
                && b[8] == 'a' && b[9] == 'v' && b[10] == 'i' && (b[11] == 'f' || b[11] == 's');
        return jpeg || png || gif || webp || avif;
    }

    /** "Wireless Headphones!" becomes "wireless-headphones". */
    private static String slug(String text) {
        String s = text == null ? "" : text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (s.isEmpty()) s = "product";
        return s.length() > 60 ? s.substring(0, 60) : s;
    }
}
