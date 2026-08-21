import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestJschKey {
    public static void main(String[] args) throws Exception {
        String key = new String(Files.readAllBytes(Paths.get("/root/.ssh/id_ed25519")));
        JSch jsch = new JSch();
        jsch.addIdentity("test-key", key.getBytes(), null, null);
        Session session = jsch.getSession("root", "127.0.0.1", 22);
        session.setConfig("StrictHostKeyChecking", "no");
        session.setConfig("PreferredAuthentications", "publickey");
        System.out.println("connecting with key...");
        session.connect(10000);
        System.out.println("connected OK");
        ChannelExec ch = (ChannelExec) session.openChannel("exec");
        ch.setCommand("echo key-auth-exit-0; hostname; date");
        ch.connect(10000);
        byte[] buf = new byte[1024];
        java.io.InputStream in = ch.getInputStream();
        int n;
        while ((n = in.read(buf)) > 0) {
            System.out.print(new String(buf, 0, n));
        }
        ch.disconnect();
        session.disconnect();
        System.out.println("done");
    }
}