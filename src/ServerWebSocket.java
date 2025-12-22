
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
            conn.send(RoomMessage(room).serializeJson());
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
            //Creation du dossier
            new File("sauvegardes").mkdir();

            //Sauvegarde des données dans des fichiers
            for (String room : log.keySet()) {
                File save = new File("sauvegardes/"+room);
                PrintWriter pw = new PrintWriter(new FileWriter(save));
                for (Message message : log.get(room)) {
                    pw.println(message.serializeJson());
                }
                pw.flush();
            }

            File saveRooms = new File("publicRooms");
            PrintWriter pw = new PrintWriter(new FileWriter(saveRooms));
            for (String room : publicRooms){
                pw.println(room);
            }
            pw.flush();

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
        System.out.println(message);
        //Deserialisation
            Message message1 = Message.unserializeJson(message);

            //Log
            if (!log.containsKey(message1.roomName)) {
                //Setting d'une nouvelle room
                publicRooms.add(message1.roomName);
                broadcast(RoomMessage(message1.roomName).serializeJson());
                message1.message = "Ouverture de Room : " + message1.roomName;
                List<Message> tmp = new ArrayList<>();
                tmp.add(message1);
                log.put(message1.roomName, tmp);
            }

            //Traitement des commandes
            switch (message1.message.split(" ")[0]) {
                //Commandes
                case "/clear":
                    log.remove(message1.roomName);
                    break;

                case "/broadcast":
                    sendBroadCast(message1);
                    break;

                //Updates
                case "askUpdate":
                    if (message1.user.equals("System")) break;

                default:
                    log.get(message1.roomName).add(message1);
            }

            //Envoi des updates
            sendUpdate(conn, message1.roomName);

            //Log
            System.out.println("Room : " + message1.roomName + "  " + message1);
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
            if (new File("sauvegardes").listFiles() == null){return;}
            //Recuperation des données contenues dans les fichiers de sauvegarde
            for (File file : Objects.requireNonNull(new File("sauvegardes").listFiles())) {
                log.put(file.getName(),new ArrayList<>());
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null){
                    log.get(file.getName()).add(Message.unserializeJson(line));
                }
            }

            File saveRoom = new File("publicRooms");
            BufferedReader reader = new BufferedReader(new FileReader(saveRoom));
            String line;
            while ((line = reader.readLine()) != null){
                publicRooms.add(line);
            }

        }catch (IOException e){
            System.out.println("Loading logs cancelled due to : " + e.getMessage());
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

    /**
     * Envoie le contenu de la room
     * @param conn interlocuteur
     * @param roomName nom de la room
     */
    public void sendUpdate(WebSocket conn,String roomName){
        //Envoi du contenu de la room
        List<Message> tmp = new ArrayList<>(log.get(roomName));
        //Affichage des 30 derniers messages
        try {
            tmp = tmp.subList(tmp.size() - 12, tmp.size());
        } catch (Exception _) {}
        for (Message msg : tmp) {
            conn.send(msg.serializeJson());
        }
    }

    public void sendBroadCast(Message message){
        String [] messageSplit = message.message.split(" ");
        if (messageSplit.length == 1){return;}
        String messageclean = String.join(" ",Arrays.copyOfRange(messageSplit,1,messageSplit.length));
        for (String room : publicRooms){
            log.get(room).add(new Message(message.user,messageclean,room));
        }
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
