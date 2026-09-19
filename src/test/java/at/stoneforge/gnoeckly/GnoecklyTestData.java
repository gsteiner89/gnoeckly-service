package at.stoneforge.gnoeckly;

import at.stoneforge.gnoeckly.tenant.DefaultTenantResolver;
import at.stoneforge.gnoeckly.wallet.CoinTransactionType;
import at.stoneforge.gnoeckly.wallet.WalletService;
import at.stoneforge.midgard.tenant.TenantContext;
import at.stoneforge.midgard.tenant.TenantHibernateFilterActivator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testdaten ueber die echten HTTP-Flows (Registrierung, Einreichen, Freigabe), damit die Tests
 * dieselben Pfade wie die App nehmen. Nur Gutschriften laufen direkt ueber den WalletService -
 * dafuer braucht es ausserhalb eines Requests den expliziten Tenant-Block ({@link #inTenant}),
 * wie bei Midgards Upgrades.
 */
@Component
public class GnoecklyTestData {

    public static final String ADMIN_EMAIL = "admin@gnoeckly.test";
    public static final String ADMIN_PASSWORD = "Test1234!";
    private static final AtomicInteger COUNTER = new AtomicInteger();

    public record TestUser(UUID id, String email, String nickname, String accessToken) {
        public String bearer() {
            return "Bearer " + accessToken;
        }
    }

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final WalletService walletService;
    private final DefaultTenantResolver defaultTenantResolver;
    private final TenantHibernateFilterActivator filterActivator;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    public GnoecklyTestData(MockMvc mockMvc, ObjectMapper objectMapper, WalletService walletService,
                            DefaultTenantResolver defaultTenantResolver, TenantHibernateFilterActivator filterActivator,
                            EntityManager entityManager, TransactionTemplate transactionTemplate) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.walletService = walletService;
        this.defaultTenantResolver = defaultTenantResolver;
        this.filterActivator = filterActivator;
        this.entityManager = entityManager;
        this.transactionTemplate = transactionTemplate;
    }

    public TestUser registerUser() throws Exception {
        int n = COUNTER.incrementAndGet();
        String nickname = "user" + n + "_" + UUID.randomUUID().toString().substring(0, 6);
        return registerUser(nickname, nickname + "@example.test");
    }

    public TestUser registerUser(String nickname, String email) throws Exception {
        JsonNode tokens = json(mockMvc.perform(post("/api/v1/public/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", "Passw0rd!x", "nickname", nickname))))
                .andExpect(status().isCreated())
                .andReturn());
        String accessToken = tokens.get("accessToken").asText();
        JsonNode me = json(mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk()).andReturn());
        return new TestUser(UUID.fromString(me.get("userId").asText()), email, nickname, accessToken);
    }

    public TestUser admin() throws Exception {
        JsonNode tokens = json(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn());
        String accessToken = tokens.get("accessToken").asText();
        JsonNode me = json(mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk()).andReturn());
        return new TestUser(UUID.fromString(me.get("userId").asText()), ADMIN_EMAIL, "admin", accessToken);
    }

    public void credit(TestUser user, long amount) {
        inTenant(() -> walletService.credit(user.id(), amount, CoinTransactionType.ADMIN_ADJUSTMENT, null, null, "Test"));
    }

    /** Setzt das Guthaben exakt auf {@code target} (Startguthaben ist 100, manche Tests brauchen weniger). */
    public void setBalance(TestUser user, long target) throws Exception {
        long current = balance(user);
        if (current > target) {
            inTenant(() -> walletService.debit(user.id(), current - target, CoinTransactionType.ADMIN_ADJUSTMENT, null, "Test"));
        } else if (current < target) {
            credit(user, target - current);
        }
    }

    public UUID anyCategoryId() throws Exception {
        JsonNode categories = json(mockMvc.perform(get("/api/v1/public/categories")).andExpect(status().isOk()).andReturn());
        return UUID.fromString(categories.get(0).get("id").asText());
    }

    /** Reicht einen Witz ein (Autor bekommt vorher genug Guthaben) und gibt die Joke-ID zurueck. */
    public UUID submitJoke(TestUser author, String text) throws Exception {
        credit(author, 5);
        JsonNode joke = json(mockMvc.perform(post("/api/v1/jokes")
                        .header("Authorization", author.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("text", text, "categoryId", anyCategoryId()))))
                .andExpect(status().isCreated())
                .andReturn());
        return UUID.fromString(joke.get("id").asText());
    }

    public UUID approvedJoke(TestUser author, String text) throws Exception {
        UUID jokeId = submitJoke(author, text);
        mockMvc.perform(post("/api/admin/jokes/" + jokeId + "/approve").header("Authorization", admin().bearer()))
                .andExpect(status().isOk());
        return jokeId;
    }

    public long balance(TestUser user) throws Exception {
        return json(mockMvc.perform(get("/api/v1/me/wallet").header("Authorization", user.bearer()))
                .andExpect(status().isOk()).andReturn()).get("balance").asLong();
    }

    public JsonNode json(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return body.isEmpty() ? objectMapper.nullNode() : objectMapper.readTree(body);
    }

    public JsonNode perform(MockHttpServletRequestBuilder request, int expectedStatus) throws Exception {
        return json(mockMvc.perform(request).andExpect(status().is(expectedStatus)).andReturn());
    }

    public <T> T inTenant(Supplier<T> action) {
        UUID tenantId = defaultTenantResolver.defaultTenantId().orElseThrow();
        return transactionTemplate.execute(status -> {
            TenantContext.setCurrentTenantId(tenantId);
            filterActivator.activate(entityManager, tenantId);
            try {
                return action.get();
            } finally {
                filterActivator.deactivate(entityManager);
                TenantContext.clear();
            }
        });
    }
}
