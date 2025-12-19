
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import java.util.*;

import org.java_websocket.WebSocket;
import org.java_websocket.server.DefaultWebSocketServerFactory;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.handshake.ClientHandshake;

/**
 * Socket de server
 */
public class ServerWebSocket extends WebSocketServer {
    /**
     * historique des rooms et des messages
     */
    public Map<String,List<Message>> log = new HashMap<>();

    /**
     * historique des rooms publiques
     */
    public Set<String> publicRooms = new HashSet<>();

    /**
     * Constructeur
     * @param address adresse
     */
    public ServerWebSocket(InetSocketAddress address) {
        super(address);
    }

    /**
     * Lors de la connexion à un nouvel user
     * @param conn interlocuteur
     * @param handshake handshake
     */
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Nouvelle connexion de " + conn.getRemoteSocketAddress());
        for (String room : publicRooms){
            conn.send(RoomMessage(room).serialize());
        }
    }

    /**
     * Fermeture de la connexion
     * @param conn interlocuteur
     * @param code ide de fermeture
     * @param reason raison de la fermeture
     * @param remote is remote
     */
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Connexion fermée : " + reason);
        try {
            //Sauvegarde des données dans des fichiers
            File save = new File("save");
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(save));
            oos.writeObject(log);

            File saveRooms = new File("rooms");
            ObjectOutputStream oos2 = new ObjectOutputStream(new FileOutputStream(saveRooms));
            oos2.writeObject(publicRooms);

        }catch (IOException e){
            System.out.println("Saving logs cancelled due to : " + e.getMessage());
        }
    }

    /**
     * Reception d'un message en String
     * @param conn interlocuteur
     * @param message message string
     */
    @Override
    public void onMessage(WebSocket conn, String message) {
        //Deserialisation
        Message message1 = Message.unserialize(message);

        //Log
        if (log.containsKey(message1.roomName)) {
            //Reception des commandes
            switch (message1.message) {
                case "/clear" -> {
                    log.remove(message1.roomName);
                    onMessage(conn,message);
                    return;
                }

                default -> log.get(message1.roomName).add(message1);
            }
        }else{
            //Setting d'une nouvelle room
            publicRooms.add(message1.roomName);
            broadcast(RoomMessage(message1.roomName).serialize());
            message1.message = "Ouverture de Room : " + message1.roomName;
            List<Message> tmp = new ArrayList<>();
            tmp.add(message1);
            log.put(message1.roomName,tmp);
        }

        //Envoi du contenu de la room
        List<Message> tmp = new ArrayList<>(log.get(message1.roomName));
        //Affichage des 30 derniers messages
        try {
            tmp = tmp.subList(tmp.size() - 12, tmp.size() - 1);
        } catch (Exception e) {
            if(!tmp.isEmpty()) {
                tmp.removeLast();
            }
        }
        for (Message msg : tmp) {
            conn.send(msg.serialize());
        }


        System.out.println("Room : " + message1.roomName + "  " + message1.toString());
    }

    /**
     * Lors de la reception d'un message en binaire
     * @param conn interlocuteur
     * @param message message
     */
    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        System.out.println("Uhhhh?");
    }

    /**
     * Si il y a une erreur
     * @param conn interlocuteur
     * @param ex exception
     */
    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("Erreur: " + ex.getMessage());
    }

    /**
     * Lors du lancement de l'app
     */
    @Override
    public void onStart() {
        System.out.println("Serveur WebSocket WSS démarré sur le port 8887");
        try {
            //Recuperation des données contenues dans les fichiers de sauvegarde
            File save = new File("save");
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(save));
            log = (Map<String,List<Message>>)ois.readObject();

            File saveRoom = new File("rooms");
            ObjectInputStream ois2 = new ObjectInputStream(new FileInputStream(saveRoom));
            publicRooms = (Set<String>) ois2.readObject();
        }catch (IOException e){
            System.out.println("Loading logs cancelled due to : " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Message de logIn de room
     * @param roomName nom de la room
     * @return message
     */
    public Message RoomMessage(String roomName){
        return new Message("RoomInfo",roomName,"System");
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
