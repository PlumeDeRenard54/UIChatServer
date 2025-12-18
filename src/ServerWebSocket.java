
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.uichat.Message;
import org.java_websocket.WebSocket;
import org.java_websocket.server.DefaultWebSocketServerFactory;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.handshake.ClientHandshake;

public class ServerWebSocket extends WebSocketServer {
    public Map<String,List<Message>> log = new HashMap<>();
    public ServerWebSocket(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Nouvelle connexion de " + conn.getRemoteSocketAddress());
        conn.send(new Message("System","Welcome","All").serialize());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        //TODO Save locally
        System.out.println("Connexion fermée : " + reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        //Deserialisation
        Message message1 = Message.unserialize(message);

        //Log
        if (log.containsKey(message1.roomName)) {
            log.get(message1.roomName).add(message1);
        }else{
            //Setting d'une nouvelle room
            message1.message = "Ouverture de Room : " + message1.roomName;
            List<Message> tmp = new ArrayList<>();
            tmp.add(message1);
            log.put(message1.roomName,tmp);
        }

        //Envoi du contenu de la room
        List<Message> tmp = new ArrayList<>(log.get(message1.roomName));
        tmp.removeLast();
        for (Message msg : tmp) {
            conn.send(msg.serialize());
        }


        System.out.println(conn.getRemoteSocketAddress() + message1.toString());

        //Renvoi
        broadcast(message);
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        System.out.println("Uhhhh?");
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("Erreur: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("Serveur WebSocket WSS démarré sur le port 8887");
    }

    public static void main(String[] args) throws Exception {

        // écoute sur toutes les interfaces (important !)
        InetSocketAddress address = new InetSocketAddress("0.0.0.0", 8080);
        ServerWebSocket server = new ServerWebSocket(address);

        /*String keystorePath = "/etc/letsencrypt/live/khaos-experiences.fr/certificat.p12";
        String keystorePassword = System.getenv("WSS_KEYSTORE_PASSWORD");

        if (keystorePassword == null) {
            throw new RuntimeException("La variable d'environnement WSS_KEYSTORE_PASSWORD n'est pas définie !");
        }

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(new FileInputStream(keystorePath), keystorePassword.toCharArray());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, keystorePassword.toCharArray());*/

        //SSLContext sslContext = SSLContext.getInstance("TLS");
        //sslContext.init(kmf.getKeyManagers(), null, null);

        //server.setWebSocketFactory(new DefaultSSLWebSocketServerFactory(sslContext));
        server.setWebSocketFactory(new DefaultWebSocketServerFactory());

        server.start();
    }
}
