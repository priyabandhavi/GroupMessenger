package edu.buffalo.cse.cse486586.groupmessenger2;

/**
 * Created by priya on 3/14/15.
 */


/**
 * Created by priya on 3/14/15.
 */
public class Seq_MsgId implements Comparable<Seq_MsgId> {


    String msgid;
    int seq;



    public void set_seq(int seq){
        this.seq = seq;

    }

    public Integer get_seq(){
        return seq;
    }

    public void set_msgid(String msgid){
        this.msgid = msgid;

    }

    public String get_msgid(){
        return msgid;
    }

    @Override
    public int compareTo(Seq_MsgId arg0) {
        return this.get_seq().compareTo(arg0.get_seq());
    }
}
