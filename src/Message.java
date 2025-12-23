
import javax.sql.rowset.serial.SerialException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Message implements Serializable {

    public String user;
    public String message;
    public String roomName;

    public Message(){}

    public Message(String user, String message, String roomname){
        this.user=user;
        this.message = message;
        this.roomName = roomname;
    }

    @Override
    public String toString() {
        return user + " : " + message;
    }

    @Override
    public boolean equals(Object o){
        if (o instanceof Message message2){

            return this.roomName.equals(message2.roomName) && this.message.equals(message2.message) && this.user.equals(message2.user);

        }
        return false;
    }


    //SERIALISATION

    private static String variable2Json(String name,String data){
        return "\"" + name + "\":\"" + data + "\"";
    }

    public String serializeJson(){

        user = user.replaceAll("\"","WrongChar!");
        message = message.replaceAll("\"","WrongChar!");
        roomName = roomName.replaceAll("\"","WrongChar!");

        String tmp = "{" +
                variable2Json("user",user) + ',' +
                variable2Json( "message",message) + ',' +
                variable2Json("roomName",roomName) + "}";


        return tmp;
    }

    private static Map<String,String> automateJson(String string) throws SerialException {
        State etat = State.DebutData;
        StringBuilder bufferNom = new StringBuilder();
        StringBuilder bufferData = new StringBuilder();
        Map<String,String> donnees = new HashMap<>();

        for (int i = 0;i<string.length();i++){
            char curChar = string.charAt(i);
            switch (etat){
                case DebutData -> {
                    if (curChar!='"'){
                        etat = State.Erreur;
                    }else{
                        etat = State.VarName;
                    }
                }

                case VarName -> {
                    if (curChar == '"'){
                        etat = State.BeforeData;
                    }else{
                        bufferNom.append(curChar);
                    }
                }

                case BeforeData -> {
                    if (curChar == '"'){
                        etat = State.Data;
                    }
                }

                case Data -> {
                    if (curChar == '"'){
                        etat = State.BetweenData;
                        donnees.put(bufferNom.toString(),bufferData.toString());
                        bufferNom = new StringBuilder();
                        bufferData = new StringBuilder();
                    }else{
                        bufferData.append(curChar);
                    }
                }

                case BetweenData -> {
                    if (curChar != ','){
                        etat = State.Fin;
                    }else {
                        etat = State.DebutData;
                    }
                }
            }
        }

        if (etat != State.Fin){
            System.out.println(donnees.keySet());
            throw new SerialException("Erreur de formatage de données");}
        return donnees;
    }

    private enum State{
        DebutData,
        VarName,
        BeforeData,
        Data,
        BetweenData,
        Fin,
        Erreur
    }

    public static Message unserializeJson(String data){
        if ((data.charAt(0) != '{') || (data.charAt(data.length()-1) != '}')){
            System.out.println("Erreur de format Json" + data);
        }

        Message messageObject = new Message();

        data = data.substring(1,data.length()-1);

        try {
            Map<String, String> stringMap = automateJson(data+" ");

            for (String varName : stringMap.keySet()){
                switch (varName){
                    case "user" -> messageObject.user = stringMap.get(varName);
                    case "message" -> messageObject.message = stringMap.get(varName);
                    case "roomName" -> messageObject.roomName = stringMap.get(varName);
                }
            }

            return messageObject;

        } catch (SerialException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args){
        System.out.println("Test serialisation JSon");

        Message message1 = new Message("User","Message","Room");
        String seria = message1.serializeJson();

        //Serialisation de base
        if (!seria.equals("{\"user\":\"User\",\"message\":\"Message\",\"roomName\":\"Room\"}")){
            System.out.println("Test1 : Mauvaise serialisation : " + seria );
        }
        if (!unserializeJson(seria).equals(message1)){
            System.out.println("Test2 : Mauvaise deserialisation : " + unserializeJson(seria).toString());
        }

        //Seria complexe
        message1 = new Message("Carrot1","je , suis : une carotte","Room");
        seria = message1.serializeJson();

        //Serialisation de base
        if (!seria.equals("{\"user\":\"Carrot1\",\"message\":\"je , suis : une carotte\",\"roomName\":\"Room\"}")){
            System.out.println("Test3 : Mauvaise serialisation : " + seria );
        }

        if (!unserializeJson(seria).equals(message1)){
            System.out.println("Test4 : Mauvaise deserialisation : " + unserializeJson(seria).toString());
        }

        //Seria "
        message1 = new Message("Carrot1","je , suis \" : une carotte","Room");
        seria = message1.serializeJson();

        //Serialisation de base
        if (!seria.equals("{\"user\":\"Carrot1\",\"message\":\"je , suis WrongChar! : une carotte\",\"roomName\":\"Room\"}")){
            System.out.println("Test3 : Mauvaise serialisation : " + seria );
        }

        if (!unserializeJson(seria).equals(message1)){
            System.out.println("Test4 : Mauvaise deserialisation : " + unserializeJson(seria).toString());
        }


    }
}
