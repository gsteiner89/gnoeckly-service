package at.stoneforge.gnoeckly.push;

import at.stoneforge.midgard.tenant.TenantContext;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Token-Verwaltung und Versand. {@link #notify} laedt die Tokens im (tenant-gebundenen) Aufrufer,
 * versendet aber erst nach dem Commit der Fachtransaktion und in einem eigenen Thread, der keine
 * Entities anfasst (kein {@code @Async}, damit kein Proxy-/Tenant-Ueberraschungen, Regel 8).
 * Ein Versandfehler bricht nie die Fachaktion ab.
 */
@Service
public class PushService {

    private static final Logger log = LoggerFactory.getLogger(PushService.class);

    private final PushTokenRepository repository;
    private final PushSender sender;
    private final PushTokenCleaner cleaner;
    private final ExecutorService executor;

    public PushService(PushTokenRepository repository, PushSender sender, PushTokenCleaner cleaner,
                       PushProperties properties) {
        this.repository = repository;
        this.sender = sender;
        this.cleaner = cleaner;
        this.executor = properties.async()
                ? Executors.newSingleThreadExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "gnoeckly-push");
                    thread.setDaemon(true);
                    return thread;
                })
                : null;
    }

    @Transactional
    public void register(UUID userId, String token, PushToken.Platform platform) {
        PushToken entry = repository.findByToken(token).orElseGet(PushToken::new);
        entry.setUserId(userId);
        entry.setToken(token);
        entry.setPlatform(platform);
        entry.setLastSeenAt(Instant.now());
        entry.setDeletedAt(null);
        repository.save(entry);
    }

    /** Nur der Besitzer kann einen Token abmelden; unbekannte Tokens sind kein Fehler (idempotent). */
    @Transactional
    public void unregister(UUID userId, String token) {
        repository.findByToken(token)
                .filter(entry -> entry.getUserId().equals(userId))
                .ifPresent(repository::delete);
    }

    /** Schickt {@code message} an alle Geraete des Users; ohne Tokens ein No-op. */
    public void notify(UUID userId, PushMessage message) {
        List<String> tokens = repository.findByUserIdAndDeletedAtIsNull(userId).stream()
                .map(PushToken::getToken).toList();
        if (tokens.isEmpty()) {
            return;
        }
        UUID tenantId = TenantContext.getCurrentTenantId().orElse(null);
        Runnable dispatch = () -> dispatch(tenantId, tokens, message);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    run(dispatch);
                }
            });
        } else {
            run(dispatch);
        }
    }

    private void run(Runnable dispatch) {
        if (executor != null) {
            executor.execute(dispatch);
        } else {
            dispatch.run();
        }
    }

    private void dispatch(UUID tenantId, List<String> tokens, PushMessage message) {
        try {
            List<String> invalid = sender.send(tokens, message);
            if (!invalid.isEmpty() && tenantId != null) {
                cleaner.remove(tenantId, invalid);
            }
        } catch (RuntimeException e) {
            log.warn("Push '{}' konnte nicht versendet werden: {}", message.title(), e.getMessage());
        }
    }

    @PreDestroy
    void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }
}
