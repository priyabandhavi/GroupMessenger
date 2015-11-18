package edu.buffalo.cse.cse486586.groupmessenger2;

/**
 * Created by priya on 3/13/15.
 */
public class Msg_MsgId implements Comparable<Msg_MsgId> {

    String msg,msgid,process;
    int seq;




    public void Set_Msg(String msg){
        this.msg = msg;
    }


    public String Get_Msg(){
        return msg;
    }


    public void Set_MsgId(String msgid){
        this.msgid = msgid;
    }


    public String Get_MsgId(){
        return msgid;
    }


    public void Set_process(String process){
        this.process = process;
    }


    public String Get_Process(){
        return process;
    }

    public void Set_Seq(int seq){
        this.seq = seq;
    }

    public Integer Get_Seq(){
        return seq;
    }

    @Override
    public int compareTo(Msg_MsgId arg0) {
        return this.Get_Seq().compareTo(arg0.Get_Seq()  + Integer.parseInt(arg0.Get_Process()));
    }
}