package at.stoneforge.gnoeckly.joke;

import at.stoneforge.midgard.web.PagedResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Moderation (Superadmin): Queue, Freigabe, Ablehnung, Loeschung, Meldungen. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('SUPERADMIN')")
public class AdminJokeController {

    private final JokeService jokeService;

    public AdminJokeController(JokeService jokeService) {
        this.jokeService = jokeService;
    }

    @GetMapping("/jokes")
    public PagedResponse<JokeResponse> byStatus(@RequestParam(defaultValue = "PENDING") JokeStatus status,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return jokeService.byStatus(status, PageRequest.of(page, Math.min(size, 100)));
    }

    @PostMapping("/jokes/{id}/approve")
    public JokeResponse approve(@PathVariable UUID id, @AuthenticationPrincipal UUID adminId) {
        return jokeService.approve(id, adminId);
    }

    @PostMapping("/jokes/{id}/reject")
    public JokeResponse reject(@PathVariable UUID id, @AuthenticationPrincipal UUID adminId,
                               @Valid @RequestBody RejectJokeRequest request) {
        return jokeService.reject(id, adminId, request.reason());
    }

    @DeleteMapping("/jokes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        jokeService.adminDelete(id);
    }

    @GetMapping("/reports")
    public PagedResponse<JokeReportResponse> openReports(@RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return jokeService.openReports(PageRequest.of(page, Math.min(size, 100)));
    }

    @PostMapping("/reports/{id}/resolve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resolveReport(@PathVariable UUID id) {
        jokeService.resolveReport(id);
    }
}
