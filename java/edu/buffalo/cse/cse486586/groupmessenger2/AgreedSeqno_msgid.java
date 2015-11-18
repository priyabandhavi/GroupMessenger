package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;

/**
 * Created by priya on 3/9/15.
 */
public class AgreedSeqno_msgid implements Serializable {
    private static final long serialVersionUID = 1L;
 int agreed_seqno, process;
    String msgid;


    public AgreedSeqno_msgid(String msgid, int agreed_seqno, int process){
        this.msgid = msgid;
        this.agreed_seqno = agreed_seqno;
        this.process = process;
    }

    public void setAgreed_seqno(int seq){
        agreed_seqno = seq;
    }

    public int getAgreed_seqno(){
        return agreed_seqno;
    }


    public void set_msgid(String msgid){
        this.msgid = msgid;
    }

    public String get_msgid(){
        return msgid;
    }

    public void set_process(int process){
        this.process = process;
    }

    public int get_process(){
        return process;
    }
}
