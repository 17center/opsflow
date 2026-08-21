import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class TestJsch {
    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "password";
        JSch jsch = new JSch();
        Session session = jsch.getSession("root", "127.0.0.1", 22);
        session.setPassword("274017877forever");
        session.setConfig("StrictHostKeyChecking", "no");
        if (mode.equals("default")) {
            // no override
        } else if (mode.equals("ki")) {
            session.setConfig("PreferredAuthentications", "keyboard-interactive");
        } else {
            session.setConfig("PreferredAuthentications", "password");
        }
        System.out.println("mode=" + mode + " connecting...");
        session.connect(10000);
        System.out.println("connected OK");
        session.disconnect();
    }
}