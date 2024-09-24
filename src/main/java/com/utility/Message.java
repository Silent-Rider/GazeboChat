package com.utility;

import java.io.Serializable;

public class Message implements Serializable {
    private final MessageType type;
    private final String text;

    public Message(MessageType type){
        this.type = type;
        text = null;
    }

    public Message(MessageType type, String data){
        this.type = type;
        this.text = data;
    }

    public MessageType getType() {
        return type;
    }

    public String getText() {
        return text;
    }
}
