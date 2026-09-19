import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

/**
 * Lokale AdMob-SSV-Simulation ohne Google (Java 21 Single-File-Programm, kein Build noetig).
 *
 * <pre>
 *   java tools/SsvSignTool.java keygen
 *       -> schreibt src/main/resources/admob-test-keys.json (Public Key, key_id 4242)
 *          und tools/ssv-private.key (Private Key, NICHT einchecken - steht in .gitignore)
 *
 *   java tools/SsvSignTool.java sign &lt;user-uuid&gt; [reward_amount]
 *       -> gibt eine fertige Callback-URL fuer http://localhost:8080 aus (dev-Profil)
 * </pre>
 *
 * Signiert wird wie bei Google: der rohe Query-String bis vor {@code &signature=}, ECDSA P-256
 * mit SHA-256, Signatur base64url ohne Padding, {@code key_id} als letzter Parameter.
 */
public class SsvSignTool {

    private static final long KEY_ID = 4242L;
    private static final Path KEYS_JSON = Path.of("src/main/resources/admob-test-keys.json");
    private static final Path PRIVATE_KEY = Path.of("tools/ssv-private.key");

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            return;
        }
        switch (args[0]) {
            case "keygen" -> keygen();
            case "sign" -> {
                if (args.length < 2) {
                    usage();
                    return;
                }
                sign(args[1], args.length > 2 ? Long.parseLong(args[2]) : 10L);
            }
            default -> usage();
        }
    }

    private static void keygen() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair pair = generator.generateKeyPair();
        String publicBase64 = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        String json = "{\n  \"keys\": [\n    {\n      \"keyId\": " + KEY_ID + ",\n      \"pem\": \"\",\n      \"base64\": \""
                + publicBase64 + "\"\n    }\n  ]\n}\n";
        Files.createDirectories(KEYS_JSON.getParent());
        Files.writeString(KEYS_JSON, json, StandardCharsets.UTF_8);
        Files.writeString(PRIVATE_KEY, Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()), StandardCharsets.UTF_8);
        System.out.println("Public Key -> " + KEYS_JSON);
        System.out.println("Private Key -> " + PRIVATE_KEY + " (nicht einchecken)");
    }

    private static void sign(String userId, long rewardAmount) throws Exception {
        UUID.fromString(userId);
        byte[] privateBytes = Base64.getDecoder().decode(Files.readString(PRIVATE_KEY).trim());
        PrivateKey privateKey = KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(privateBytes));

        String message = "ad_network=5450213213286189855&ad_unit=1234567890&reward_amount=" + rewardAmount
                + "&reward_item=coins&timestamp=" + System.currentTimeMillis()
                + "&transaction_id=" + UUID.randomUUID().toString().replace("-", "")
                + "&user_id=" + userId;
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(privateKey);
        signer.update(message.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(signer.sign());

        System.out.println("http://localhost:8080/api/v1/public/admob/ssv?" + message + "&signature=" + signature + "&key_id=" + KEY_ID);
    }

    private static void usage() {
        System.out.println("java tools/SsvSignTool.java keygen");
        System.out.println("java tools/SsvSignTool.java sign <user-uuid> [reward_amount]");
    }
}
