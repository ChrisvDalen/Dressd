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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OutfitFlowIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void saveThenRetrieveOutfitPreservesLayerOrder() {
        UUID avatarId = UUID.randomUUID();
        SaveOutfitRequest req = new SaveOutfitRequest(avatarId, "Casual Friday", List.of(
                new GarmentLayerDto(UUID.randomUUID(), GarmentCategory.BOTTOM, 1, 0f, 0f, 1f),
                new GarmentLayerDto(UUID.randomUUID(), GarmentCategory.TOP, 2, 0f, 0f, 1f),
                new GarmentLayerDto(UUID.randomUUID(), GarmentCategory.SOCKS, 0, 0f, 0f, 1f)
        ));

        ResponseEntity<OutfitResponse> created = rest.postForEntity("/api/outfits", req, OutfitResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID id = created.getBody().id();

        // Retrieve and check layers are ordered by zIndex ascending.
        OutfitResponse fetched = rest.getForObject("/api/outfits/" + id, OutfitResponse.class);
        assertThat(fetched.name()).isEqualTo("Casual Friday");
        assertThat(fetched.garmentLayers()).extracting(GarmentLayerDto::category)
                .containsExactly(GarmentCategory.SOCKS, GarmentCategory.BOTTOM, GarmentCategory.TOP);

        // It shows up in the owner's outfit list.
        ResponseEntity<OutfitResponse[]> list = rest.getForEntity("/api/outfits", OutfitResponse[].class);
        assertThat(list.getBody()).extracting(OutfitResponse::id).contains(id);
    }
}
