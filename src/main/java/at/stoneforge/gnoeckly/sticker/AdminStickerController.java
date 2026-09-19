package at.stoneforge.gnoeckly.sticker;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/stickers")
@PreAuthorize("hasAuthority('SUPERADMIN')")
public class AdminStickerController {

    private final StickerService stickerService;

    public AdminStickerController(StickerService stickerService) {
        this.stickerService = stickerService;
    }

    @GetMapping
    public List<StickerResponse> list() {
        return stickerService.adminList();
    }

    @GetMapping("/{id}")
    public StickerResponse get(@PathVariable UUID id) {
        return stickerService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StickerResponse create(@Valid @RequestBody CreateStickerRequest request) {
        return stickerService.create(request);
    }

    @PutMapping("/{id}")
    public StickerResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateStickerRequest request) {
        return stickerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        stickerService.softDelete(id);
    }

    @PostMapping(value = "/{id}/image", consumes = "multipart/form-data")
    public StickerResponse uploadImage(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return stickerService.uploadImage(id, file);
    }
}
