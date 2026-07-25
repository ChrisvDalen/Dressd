package com.dressd.wardrobe.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dressd.common.web.BadRequestException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GarmentImagesTest {

    private static final UUID OWNER = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID OTHER_OWNER = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @TempDir
    Path root;

    private LocalObjectStorage storage;
    private GarmentImages images;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties();
        properties.getLocal().setRoot(root.toString());
        properties.getLocal().setPublicBaseUrl("/media");
        storage = new LocalObjectStorage(properties);
        images = new GarmentImages(storage);
    }

    @Test
    void confirmMovesAPendingScanIntoTheGarmentsArea() {
        String pendingUrl = storage.store(images.pendingKey(OWNER), "png".getBytes(), "image/png");
        assertThat(pendingUrl).startsWith("/media/pending/" + OWNER + "/");

        String confirmed = images.confirm(OWNER, pendingUrl);

        assertThat(confirmed).startsWith("/media/garments/" + OWNER + "/");
        assertThat(root.resolve(confirmed.substring("/media/".length()))).exists();
        assertThat(root.resolve(pendingUrl.substring("/media/".length()))).doesNotExist();
    }

    @Test
    void confirmIsIdempotentForAlreadyConfirmedImages() {
        String pendingUrl = storage.store(images.pendingKey(OWNER), "png".getBytes(), "image/png");
        String confirmed = images.confirm(OWNER, pendingUrl);

        assertThat(images.confirm(OWNER, confirmed)).isEqualTo(confirmed);
    }

    @Test
    void confirmRejectsAUrlOutsideOurStorage() {
        assertThatThrownBy(() -> images.confirm(OWNER, "https://elsewhere.example/x.png"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a Dressd media URL");
    }

    @Test
    void confirmRejectsAnotherOwnersScan() {
        String foreign = storage.store(images.pendingKey(OTHER_OWNER), "png".getBytes(), "image/png");

        assertThatThrownBy(() -> images.confirm(OWNER, foreign))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("one of your scans");
    }

    @Test
    void confirmRejectsPathTraversal() {
        assertThatThrownBy(() -> images.confirm(OWNER, "/media/../../etc/passwd"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void discardOnlyDeletesTheOwnersOwnGarmentImage() {
        String mine = images.confirm(OWNER,
                storage.store(images.pendingKey(OWNER), "png".getBytes(), "image/png"));
        String theirs = images.confirm(OTHER_OWNER,
                storage.store(images.pendingKey(OTHER_OWNER), "png".getBytes(), "image/png"));

        images.discard(OWNER, theirs);
        assertThat(root.resolve(theirs.substring("/media/".length()))).exists();

        images.discard(OWNER, mine);
        assertThat(root.resolve(mine.substring("/media/".length()))).doesNotExist();
    }

    @Test
    void purgeRemovesOnlyPendingScansOlderThanTheTtl() throws IOException {
        String stale = storage.store(images.pendingKey(OWNER), "old".getBytes(), "image/png");
        String fresh = storage.store(images.pendingKey(OWNER), "new".getBytes(), "image/png");
        String confirmed = images.confirm(OWNER,
                storage.store(images.pendingKey(OWNER), "kept".getBytes(), "image/png"));

        Path stalePath = root.resolve(stale.substring("/media/".length()));
        Files.setLastModifiedTime(stalePath,
                java.nio.file.attribute.FileTime.from(Instant.now().minus(Duration.ofDays(2))));

        assertThat(images.purgeStalePending(Duration.ofHours(6))).isEqualTo(1);

        assertThat(stalePath).doesNotExist();
        assertThat(root.resolve(fresh.substring("/media/".length()))).exists();
        assertThat(root.resolve(confirmed.substring("/media/".length()))).exists();
    }

    @Test
    void storeRejectsKeysThatEscapeTheRoot() {
        assertThatThrownBy(() -> storage.store("../escaped.png", "x".getBytes(), "image/png"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void keyForUrlIgnoresUrlsFromElsewhere() {
        assertThat(storage.keyForUrl("https://cdn.example/media/x.png")).isEmpty();
        assertThat(storage.keyForUrl("/other/x.png")).isEmpty();
        assertThat(storage.keyForUrl(null)).isEmpty();
        assertThat(storage.keyForUrl("/media/garments/x.png")).contains("garments/x.png");
    }
}
