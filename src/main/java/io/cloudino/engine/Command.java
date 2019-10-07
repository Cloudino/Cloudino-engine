/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.engine;

/**
 *
 * @author javiersolis
 */
public class Command
{
    public static byte TYPE_MSG=0;
    public static byte TYPE_LOG=1;
    public static byte TYPE_BIN=2;
    
    public byte type=0;                //0=message, 1=log, 2=Binary
    public byte topic[];
    public byte msg[];

    public Command() {
    }

    public Command(byte type, byte[] topic, byte[] msg) {
        this.type=type;
        this.topic = topic;
        this.msg = msg;
    }

    @Override
    public String toString() {
        String ret="Type:"+type+"";
        if(topic!=null)ret+=" Topic:"+new String(topic);
        if(msg!=null)ret+=" Msg:"+new String(msg);
        return ret;
    }    
    
}