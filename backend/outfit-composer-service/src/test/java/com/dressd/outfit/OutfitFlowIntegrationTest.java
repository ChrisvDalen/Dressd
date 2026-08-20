package com.dressd.outfit;

import static org.assertj.core.api.Assertions.assertThat;

import com.dressd.common.domain.GarmentCategory;
import com.dressd.outfit.api.dto.GarmentLayerDto;
import com.dressd.outfit.api.dto.OutfitResponse;
import com.dressd.outfit.api.dto.SaveOutfitRequest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OutfitFlowIntegrationTest {

    /** Mirrors {@code PageResponse<OutfitResponse>} for deserialisation. */
    record OutfitPage(List<OutfitResponse> content, int page, int size,
                      long totalElements, int totalPages, boolean first, boolean last) {
    }

    private static final ParameterizedTypeReference<OutfitPage> OUTFIT_PAGE =
            new ParameterizedTypeReference<>() {
            };

    @Autowired
    private TestRestTemplate rest;

    @Test
    void saveThenRetrieveOutfitPreservesLayerOrder() {
        SaveOutfitRequest req = new SaveOutfitRequest(UUID.randomUUID(), "Casual Friday", List.of(
                layer(GarmentCategory.BOTTOM, 1),
                layer(GarmentCategory.TOP, 2),
                layer(GarmentCategory.SOCKS, 0)
        ));

        ResponseEntity<OutfitResponse> created = rest.postForEntity("/api/outfits", req, OutfitResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID id = created.getBody().id();

        // Retrieve and check layers are ordered by zIndex ascending.
        OutfitResponse fetched = rest.getForObject("/api/outfits/" + id, OutfitResponse.class);
        assertThat(fetched.name()).isEqualTo("Casual Friday");
        assertThat(fetched.garmentLayers()).extracting(GarmentLayerDto::category)
                .containsExactly(GarmentCategory.SOCKS, GarmentCategory.BOTTOM, GarmentCategory.TOP);
        // Sparse client z-indexes are renumbered contiguously on save.
        assertThat(fetched.garmentLayers()).extracting(GarmentLayerDto::zIndex)
                .containsExactly(0, 1, 2);

        // It shows up in the owner's outfit list.
        OutfitPage list = rest.exchange("/api/outfits", HttpMethod.GET, HttpEntity.EMPTY, OUTFIT_PAGE)
                .getBody();
        assertThat(list.content()).extracting(OutfitResponse::id).contains(id);
        assertThat(list.totalElements()).isPositive();
    }

    @Test
    void listIsPaged() {
        for (int i = 0; i < 3; i++) {
            rest.postForEntity("/api/outfits",
                    new SaveOutfitRequest(UUID.randomUUID(), "outfit-" + i,
                            List.of(layer(GarmentCategory.TOP, 0))),
                    OutfitResponse.class);
        }

        OutfitPage firstPage = rest.exchange("/api/outfits?page=0&size=2", HttpMethod.GET,
                HttpEntity.EMPTY, OUTFIT_PAGE).getBody();

        assertThat(firstPage.content()).hasSize(2);
        assertThat(firstPage.size()).isEqualTo(2);
        assertThat(firstPage.totalElements()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void rejectsTheSameGarmentTwiceInOneOutfit() {
        UUID garmentId = UUID.randomUUID();
        SaveOutfitRequest req = new SaveOutfitRequest(UUID.randomUUID(), "Doubled", List.of(
                new GarmentLayerDto(garmentId, GarmentCategory.TOP, 0, 0f, 0f, 1f),
                new GarmentLayerDto(garmentId, GarmentCategory.LAYER, 1, 0f, 0f, 1f)
        ));

        ResponseEntity<String> response = rest.postForEntity("/api/outfits", req, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("appears twice");
    }

    @Test
    void rejectsAnOutOfRangeScale() {
        SaveOutfitRequest req = new SaveOutfitRequest(UUID.randomUUID(), "Huge", List.of(
                new GarmentLayerDto(UUID.randomUUID(), GarmentCategory.TOP, 0, 0f, 0f, 500f)
        ));

        ResponseEntity<String> response = rest.postForEntity("/api/outfits", req, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Validation failed");
    }

    @Test
    void anotherOwnerCannotReadOrDeleteMyOutfit() {
        OutfitResponse mine = rest.postForObject("/api/outfits",
                new SaveOutfitRequest(UUID.randomUUID(), "Private",
                        List.of(layer(GarmentCategory.TOP, 0))),
                OutfitResponse.class);

        org.springframework.http.HttpHeaders otherOwner = new org.springframework.http.HttpHeaders();
        otherOwner.set("X-Owner-Id", "22222222-2222-2222-2222-222222222222");

        assertThat(rest.exchange("/api/outfits/" + mine.id(), HttpMethod.GET,
                new HttpEntity<>(otherOwner), String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(rest.exchange("/api/outfits/" + mine.id(), HttpMethod.DELETE,
                new HttpEntity<>(otherOwner), String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private static GarmentLayerDto layer(GarmentCategory category, int zIndex) {
        return new GarmentLayerDto(UUID.randomUUID(), category, zIndex, 0f, 0f, 1f);
    }
}
