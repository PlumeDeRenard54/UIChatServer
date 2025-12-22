

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
        if (o instanceof Message){
            Message message2 = (Message)o;

            return this.roomName.equals(message2.roomName) && this.message.equals(message2.message) && this.user.equals(message2.user);

        }
        return false;
    }


    //SERIALISATION

    private static String variable2Json(String name,String data){
        return "\"" + name + "\":\"" + data + "\"";
    }

    private static String getValueJson(String s){
        s=s.split(":")[1];
        return s.substring(1,s.length()-1);
    }

    public String serializeJson(){

        String tmp = "{" +
                variable2Json("user",user) + ',' +
                variable2Json( "message",message) + ',' +
                variable2Json("roomName",roomName) + "}";

        return tmp;
    }

    public static Message unserializeJson(String data){
        if ((data.charAt(0) != '{') || (data.charAt(data.length()-1) != '}')){
            System.out.println("Erreur de format Json" + data);
        }

        Message messageObject = new Message();

        data = data.substring(1,data.length()-1);
        for (String vars : data.split(",")){
            String varName = vars.split(":")[0];
            switch (varName){
                case "\"user\"" -> messageObject.user = getValueJson(vars);
                case "\"message\"" -> messageObject.message = getValueJson(vars);
                case "\"roomName\"" -> messageObject.roomName = getValueJson(vars);
            }
        }

        return messageObject;
    }

    public static void main(String[] args){
        System.out.println("Test serialisation JSon");

        Message message1 = new Message("User","Message","Room");
        String seria = message1.serializeJson();

        if (!seria.equals("{\"user\":\"User\",\"message\":\"Message\",\"roomName\":\"Room\"}")){
            System.out.println("Mauvaise serialisation : " + seria );
        }

        if (!unserializeJson(seria).equals(message1)){
            System.out.println("Mauvaise deserialisation");

        }


    }
}
