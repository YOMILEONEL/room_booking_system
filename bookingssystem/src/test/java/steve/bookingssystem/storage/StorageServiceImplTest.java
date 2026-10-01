package steve.bookingssystem.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Regression test for docs/code-review.md 2.6: uploadRoomImage used to trust the client-supplied
// Content-Type header outright - a ".html" file declared as "image/png" landed unchanged in the
// public Supabase bucket.
class StorageServiceImplTest {

    private StorageServiceImpl storageService;

    @BeforeEach
    void setUp() {
        storageService = new StorageServiceImpl();
        // Bypass the "not configured" check - these fixtures never reach a real network call
        // because the fake-image case is rejected before StorageServiceImpl talks to S3.
        ReflectionTestUtils.setField(storageService, "endpoint", "http://localhost:1");
        ReflectionTestUtils.setField(storageService, "region", "us-east-1");
        ReflectionTestUtils.setField(storageService, "bucket", "test-bucket");
        ReflectionTestUtils.setField(storageService, "accessKey", "test-access-key");
        ReflectionTestUtils.setField(storageService, "secretKey", "test-secret-key");
        ReflectionTestUtils.setField(storageService, "supabaseUrl", "http://localhost:1");
    }

    @Test
    void uploadRoomImage_rejectsContentThatIsNotActuallyAnImage() {
        MockMultipartFile fakeImage = new MockMultipartFile(
                "file", "malicious.png", "image/png",
                "<html><script>alert(1)</script></html>".getBytes());

        assertThatThrownBy(() -> storageService.uploadRoomImage(UUID.randomUUID(), fakeImage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("kein gültiges Bild");
    }

    @Test
    void uploadRoomImage_rejectsNonImageContentType() {
        MockMultipartFile textFile = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "hello".getBytes());

        assertThatThrownBy(() -> storageService.uploadRoomImage(UUID.randomUUID(), textFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nur Bilddateien");
    }

    @Test
    void uploadProfileImage_rejectsContentThatIsNotActuallyAnImage() {
        MockMultipartFile fakeImage = new MockMultipartFile(
                "file", "malicious.png", "image/png",
                "<html><script>alert(1)</script></html>".getBytes());

        assertThatThrownBy(() -> storageService.uploadProfileImage(UUID.randomUUID(), fakeImage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("kein gültiges Bild");
    }

    @Test
    void uploadProfileImage_rejectsEmptyFileAndNonImageType() {
        MockMultipartFile empty = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);
        MockMultipartFile text = new MockMultipartFile("file", "a.txt", "text/plain", "hello".getBytes());

        assertThatThrownBy(() -> storageService.uploadProfileImage(UUID.randomUUID(), empty))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storageService.uploadProfileImage(UUID.randomUUID(), text))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nur Bilddateien");
    }

    @Test
    void deleteProfileImageByPublicUrl_ignoresForeignOrEmptyUrls() {
        // Outside our own bucket this must be a silent no-op: a real S3 call against the
        // unreachable fake endpoint would throw.
        storageService.deleteProfileImageByPublicUrl(null);
        storageService.deleteProfileImageByPublicUrl("");
        storageService.deleteProfileImageByPublicUrl("https://evil.example/storage/v1/object/public/test-bucket/avatars/x.png");
        storageService.deleteProfileImageByPublicUrl("http://localhost:1/storage/v1/object/public/other-bucket/avatars/x.png");
        storageService.deleteProfileImageByPublicUrl("http://localhost:1/storage/v1/object/public/test-bucket/");
    }

    @Test
    void deleteProfileImageByPublicUrl_ignoresNonAvatarAndTraversalKeys() {
        // Own bucket, but not an avatar / suspicious key: must be a silent no-op (no S3 call,
        // which would throw against the unreachable fake endpoint).
        storageService.deleteProfileImageByPublicUrl("http://localhost:1/storage/v1/object/public/test-bucket/rooms/x.png");
        storageService.deleteProfileImageByPublicUrl("http://localhost:1/storage/v1/object/public/test-bucket/avatars/../rooms/x.png");
        storageService.deleteProfileImageByPublicUrl("http://localhost:1/storage/v1/object/public/test-bucket/avatars/a\\b.png");
        storageService.deleteProfileImageByPublicUrl("http://localhost:1/storage/v1/object/public/test-bucket/avatars/");
    }

    @Test
    void deleteProfileImageByPublicUrl_ownBucketUrlTriggersS3Call() {
        assertThatThrownBy(() -> storageService.deleteProfileImageByPublicUrl(
                "http://localhost:1/storage/v1/object/public/test-bucket/avatars/x.png"))
                .isNotInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void uploadRoomImage_aRealPngPassesContentValidation() {
        byte[] pngSignaturePlusPadding = new byte[]{
                (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0
        };
        MockMultipartFile realPng = new MockMultipartFile(
                "file", "room.png", "image/png", pngSignaturePlusPadding);

        // Validation passes, so the code proceeds to the (unreachable, fake) S3 endpoint instead
        // of failing with "kein gültiges Bild" - proving genuine images aren't false-rejected.
        assertThatThrownBy(() -> storageService.uploadRoomImage(UUID.randomUUID(), realPng))
                .isNotInstanceOf(IllegalArgumentException.class);
    }
}
