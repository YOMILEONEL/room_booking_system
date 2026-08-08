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

        String key = "rooms/" + roomId + "-" + UUID.randomUUID() + extractExtension(file.getOriginalFilename(), contentType);

        try {
            client().putObject(
                    PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException e) {
            throw new RuntimeException("Bild konnte nicht hochgeladen werden", e);
        }

        return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + key;
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

    private String extractExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }
}
