package at.stoneforge.gnoeckly.sticker;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/stickers")
public class PublicStickerController {

    private final StickerService stickerService;

    public PublicStickerController(StickerService stickerService) {
        this.stickerService = stickerService;
    }

    @GetMapping
    public List<StickerResponse> catalog() {
        return stickerService.catalog();
    }

    /**
     * Oeffentlich ohne Token: der Tenant fuer {@code StorageService.retrieve} kommt vom
     * DefaultTenantFilter. Lange Cache-Zeit plus ETag, weil Sticker-Bilder in jeder Feed-Karte
     * auftauchen und sich praktisch nie aendern.
     */
    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> image(@PathVariable UUID id) {
        StickerService.StickerImage image = stickerService.image(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .eTag("\"" + image.checksum() + "\"")
                .body(image.bytes());
    }
}
