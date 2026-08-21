import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TestAes {
    public static void main(String[] args) {
        String enc = "j3Cb+m8d9v21GO+4PoAiAwcAMOhniKFITgD3D9AVBXQ=";
        byte[] KEY = "OpsFlow-Aes-Key-2026-Secure-K3YS".getBytes(StandardCharsets.UTF_8);
        System.out.println("key length = " + KEY.length);
        try {
            Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY, "AES"));
            String plain = new String(c.doFinal(Base64.getDecoder().decode(enc)), StandardCharsets.UTF_8);
            System.out.println("decrypted=[" + plain + "]");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}