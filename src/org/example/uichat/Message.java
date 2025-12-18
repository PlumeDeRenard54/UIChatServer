package org.example.uichat;

//import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Message implements Serializable {
    private static final String valueSplitChar = "¤";
    private static final String fieldSplitChar = "%";

    public String user;
    public String message;
    public String roomName;

    public Message(){}

    public Message(String user, String message, String roomname){
        this.user=user;
        this.message = message;
        this.roomName = roomname;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    @Override
    public String toString() {
        return user + " : " + message;
    }

    public String serialize(){

        String tmp = "User" + valueSplitChar + user + fieldSplitChar +
                "Message" + valueSplitChar + message + fieldSplitChar +
                "RoomName" + valueSplitChar + roomName;

        return tmp;
    }

    public static Message unserialize(String data){
        Message messageObject = new Message();
        Map<String,String> datas = new HashMap<>();
        for ( String s : data.split(fieldSplitChar)){
            String[] tmp = s.split(valueSplitChar);
            datas.put(tmp[0],tmp[1]);
        }

        messageObject.roomName = datas.get("RoomName");
        messageObject.message = datas.get("Message");
        messageObject.user = datas.get("User");

        return messageObject;
    }
}
