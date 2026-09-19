package at.stoneforge.gnoeckly.wallet;

import at.stoneforge.midgard.web.PagedResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public WalletResponse wallet(@AuthenticationPrincipal UUID userId) {
        return WalletResponse.from(walletService.get(userId));
    }

    @GetMapping("/transactions")
    public PagedResponse<CoinTransactionResponse> transactions(@AuthenticationPrincipal UUID userId,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        return PagedResponse.from(walletService.ledger(userId, PageRequest.of(page, Math.min(size, 100))),
                CoinTransactionResponse::from);
    }
}
