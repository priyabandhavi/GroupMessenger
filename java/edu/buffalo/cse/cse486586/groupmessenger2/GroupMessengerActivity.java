package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.content.ContentValues;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.UUID;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {


    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    int message_identifier = 0;
    String myPort;

    static final int SERVER_PORT = 10000;
    static final String[] REMOTE_PORTS= {"11108","11112","11116","11120","11124"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */


        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);         //REFERENCE:PA1
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));


        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());



        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        final EditText edt = (EditText) findViewById(R.id.editText1);
        findViewById(R.id.button4).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String msg = edt.getText().toString();
                        edt.setText("");
                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
                    }
                });

        try {

            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);                           //REFERENCE PA1
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        private PriorityQueue<Msg_MsgId> ReceivedMsgSeq = new PriorityQueue<>();
        private PriorityQueue<Msg_MsgId> ReceivedMsgSeq1 = new PriorityQueue<>();

        private HashMap<String,PriorityQueue<Seq_MsgId>> Msgid_ProposedSequences = new HashMap<>();


        private final Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");


        private Uri buildUri(String scheme, String authority) {
            Uri.Builder uri = new Uri.Builder();
            uri.authority(authority);
            uri.scheme(scheme);
            return uri.build();
        }

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            int count = 0;
            Socket socket = null;
            String[] received_msg = new String[4];
            int seq = 0;
            int value = 0;

            while (true) {
                try {
                    socket = serverSocket.accept();
                    BufferedReader inReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String inputLine;
                    while ((inputLine = inReader.readLine()) != null) {
                        received_msg = inputLine.split(";");
                        sb.append(received_msg[0]);


                        String message = received_msg[0];
                        String unique_id = received_msg[1];
                        String processORpropseq = received_msg[2];
                        String decide_factor = received_msg[3];


                        if (decide_factor.equals("FLAG=MSG")) {
                            Msg_MsgId m = new Msg_MsgId();
                            m.Set_Msg(message);
                            /// System.out.println(m.Get_Msg());
                            m.Set_MsgId(unique_id);
                            m.Set_process(processORpropseq);
                            m.Set_Seq(count);

                            ReceivedMsgSeq.add(m);
                            ReceivedMsgSeq1 = ReceivedMsgSeq;
                            //System.out.println(ReceivedMsgSeq.peek().Get_Msg());
                            count++;


                            ProposeSqIdAndMsgId(m.Get_MsgId(), m.Get_Seq(), m.Get_Process());

                        } else if (decide_factor.equals("FLAG=PROPOSE")) {
                            seq = Integer.parseInt(processORpropseq);
                            Seq_MsgId s = new Seq_MsgId();
                            s.set_msgid(unique_id);
                            s.set_seq(seq);


                            if (Msgid_ProposedSequences.isEmpty() || !Msgid_ProposedSequences.containsKey(unique_id)) {
                                PriorityQueue<Seq_MsgId> ProposedMsgSeq = new PriorityQueue<>(5, Collections.reverseOrder());
                                ProposedMsgSeq.add(s);
                                Msgid_ProposedSequences.put(unique_id, ProposedMsgSeq);
                            } else {
                                Msgid_ProposedSequences.get(unique_id).add(s);
                            }
                            if (Msgid_ProposedSequences.get(unique_id).size() == 5) {

                                Seq_MsgId agreed = Msgid_ProposedSequences.get(unique_id).peek();
                                mCastAgreedSq(agreed.get_msgid(), agreed.get_seq());
                            }


                        } else {


                            while (!ReceivedMsgSeq1.isEmpty()) {
                                Msg_MsgId m = ReceivedMsgSeq1.peek();
                                // System.out.println(m.Get_MsgId());
                                if (m.Get_MsgId() == unique_id) {
                                    //  System.out.println(m.Get_MsgId());
                                    ReceivedMsgSeq.remove(m);
                                    int Seqset = Integer.parseInt(processORpropseq);

                                    int agree = Msgid_ProposedSequences.get(m.Get_MsgId()).peek().get_seq();
                                    if(agree > count){
                                        count = agree + 1;
                                    }
                                    m.Set_Seq(Seqset);
                                    ReceivedMsgSeq.add(m);
                                    break;
                                } else {
                                    ReceivedMsgSeq1.remove(m);
                                }

                            }


                            /*if(==unique_id){
                                System.out.println(m.Get_MsgId());
                                ReceivedMsgSeq.remove(m);

                            }*/


                        }








                   /*
                    System.out.println(received_msg[0]);
                 System.out.println(received_msg[1]);
                    System.out.println(received_msg[2]); */


                        ContentValues keyValueToInsert = new ContentValues();
                        for (int i = 0; i < ReceivedMsgSeq.size(); i++) {
                            Msg_MsgId mm = ReceivedMsgSeq.poll();
                            //System.out.println(mm.Get_Msg());
                            keyValueToInsert.put("key", String.valueOf(value));
                            keyValueToInsert.put("value", mm.Get_Msg());


                            Uri newUri = getContentResolver().insert(
                                    mUri,    // assume we already created a Uri object with our provider URI
                                    keyValueToInsert
                            );
                        }


                        publishProgress(sb.toString());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Unable to close socket");
                    }
                }
            }
            //return null;
        }

        protected void onProgressUpdate(String... strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            TextView tv = (TextView) findViewById(R.id.textView1);
            tv.append(strReceived + "\t\n");


        }
    }





    private void ProposeSqIdAndMsgId(String msgid, int seq, String process){

        String propose_message;
        int PROCESS = Integer.parseInt(process);

        try {
            String remotePort = REMOTE_PORTS[PROCESS];
            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                    Integer.parseInt(remotePort));

            propose_message = "" + ";" + msgid + ";" + seq + ";" + "FLAG=PROPOSE";

            PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);   //REFERENCE PA1
            printWriter.println(propose_message);
            socket.close();
        } catch (UnknownHostException e) {
            Log.v(TAG, "ClientTask UnknownHostException");
        } catch (IOException e) {
            Log.v(TAG, "ClientTask socket IOException");
        }


    }

    private void mCastAgreedSq(String msgid, int seq) {
        String identify = msgid;
        String arrange = String.valueOf(seq);



        for (int i = 0; i < REMOTE_PORTS.length; i++) {
            try {

                String remotePort = REMOTE_PORTS[i];
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(remotePort));

                String finalmsg = "" + ";" + identify + ";" + arrange + ";" + "FLAG=AGREE";

                PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);   //REFERENCE PA1
                printWriter.println(finalmsg);
                socket.close();
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }
        }
    }















    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

            String message_identifier = UUID.randomUUID().toString();
            int process_id = (Integer.parseInt(myPort) - 11108) / 4;
            for(int i=0; i<REMOTE_PORTS.length;i++) {
                try {

                    String remotePort = REMOTE_PORTS[i];
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort));
                    String msgToSend = msgs[0];
                    String process = Integer.toString(process_id);

                   /*Random r = new Random();
                    int message_identifier = r.nextInt(100)+1; */

                    String initial_msg = msgToSend + ";" + message_identifier + ";" + process + ";" + "FLAG=MSG";





                    PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);   //REFERENCE PA1
                    printWriter.println(initial_msg);
                    socket.close();

                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException");
                }/*catch (IOException e) {
                    e.printStackTrace();}*/

            }
            return null;
        }
    }
}
