package steve.bookingssystem.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class StorageServiceImpl implements StorageService {

    @Value("${supabase.storage.endpoint:}")
    private String endpoint;
    @Value("${supabase.storage.region:}")
    private String region;
    @Value("${supabase.storage.bucket:}")
    private String bucket;
    @Value("${supabase.storage.access-key:}")
    private String accessKey;
    @Value("${supabase.storage.secret-key:}")
    private String secretKey;
    @Value("${supabase.url:}")
    private String supabaseUrl;

    // Built lazily (not @PostConstruct) since this whole feature is optional -
    // the app must still start and serve everything else when S3 isn't configured yet.
    private S3Client s3Client;

    @Override
    public synchronized String uploadRoomImage(UUID roomId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Keine Datei hochgeladen");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Nur Bilddateien sind erlaubt");
        }
        if (endpoint.isBlank() || bucket.isBlank() || accessKey.isBlank() || secretKey.isBlank() || supabaseUrl.isBlank()) {
            throw new IllegalStateException("Bild-Upload ist nicht konfiguriert (Supabase S3)");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Bild konnte nicht gelesen werden", e);
        }

        // The Content-Type header is entirely client-controlled - without checking the actual
        // bytes, a file declared as "image/png" but containing e.g. HTML/JS would land in the
        // *public* Supabase bucket as-is (see docs/code-review.md, 2.6).
        if (!looksLikeImage(content)) {
            throw new IllegalArgumentException("Datei ist kein gültiges Bild");
        }

        // The extension comes only from the (now content-verified) contentType, never from the
        // client-supplied filename - that filename was previously spliced straight into the S3
        // key (e.g. "photo.png/../../evil" would have contributed ".png/../../evil").
        String key = "rooms/" + roomId + "-" + UUID.randomUUID() + extensionFor(contentType);

        client().putObject(
                PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
                RequestBody.fromBytes(content)
        );

        return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + key;
    }

    // Magic-byte sniffing for the image formats this endpoint accepts (see extensionFor below) -
    // deliberately not delegating to javax.imageio.ImageIO.read(...), since the JDK's built-in
    // ImageIO plugins don't decode WebP, which would reject legitimate WebP uploads.
    private boolean looksLikeImage(byte[] bytes) {
        if (bytes.length < 12) {
            return false;
        }
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return true; // PNG
        }
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return true; // JPEG
        }
        if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == '8') {
            return true; // GIF87a / GIF89a
        }
        String riff = new String(bytes, 0, 4, StandardCharsets.US_ASCII);
        String webp = new String(bytes, 8, 4, StandardCharsets.US_ASCII);
        return riff.equals("RIFF") && webp.equals("WEBP");
    }

    private S3Client client() {
        if (s3Client == null) {
            s3Client = S3Client.builder()
                    .endpointOverride(URI.create(endpoint))
                    .region(Region.of(region.isBlank() ? "us-east-1" : region))
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                    .build();
        }
        return s3Client;
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }
}
