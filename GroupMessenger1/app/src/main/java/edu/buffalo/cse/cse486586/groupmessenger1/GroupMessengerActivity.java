package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.widget.TextView;
import android.content.Context;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

// GroupMessengerActivity is the main Activity.
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    
    // Fixed redirection ports for 5 AVD's
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    
    // Fields to store key:message ID and value:message
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    
    // Message ID represented as a sequence number
    private static int sequenceNum = 0;
    
    // Server socket listening port
    static final int SERVER_PORT = 10000;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        
        // Getting the port number of myself(AVD)
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        // As we did in the PA1, lets create a server asynTask and client asyncTask
        // Server task invocation
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch(IOException e) {
            Log.e(TAG, "Error in creating a socket");
            return;
        }
        
        // Using the TextView to display messages to make debugging easier.
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
               
        // Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
        // OnPTestClickListener demonstrates how to access a ContentProvider. 
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
         
        // Registering and implementing an OnClickListener for the "Send" button.
        // Getting the message from the input box (EditText) and sending it to other AVDs.
        // Creating editText to display 
        final EditText editText = (EditText) findViewById(R.id.editText1);
        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    String msg = editText.getText().toString() + "\n";
                    editText.setText(""); // This is one way to reset the input box.
                    TextView localTextView = (TextView) findViewById(R.id.textView1);
                    localTextView.append("\t" + msg); // This is one way to display a string.
                    TextView remoteTextView = (TextView) findViewById(R.id.textView1);
                    remoteTextView.append("\n");
                    
                    // Client task invocation
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    /* ---------------------------------------------------------------------
    * Server Task Implementation
    * Receives messages and passes them to onProgressUpdate()
     -------------------------------------------------------------------- */
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
    
        /* -------------------------------------------------------------------------------
        * @Override
        * @name doInBackground() 
        * @desc This method does all the tasks that are required, in the background.  
        * @param sockets
        * @return Void
        ---------------------------------------------------------------------------------- */
        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Socket server = null;
            String msg = null;
            while(true) {
                try {
                    server = serverSocket.accept();
                    
                    // InputStream
                    InputStream is = server.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    
                    // OutputStream
                    String msgToSend = "ACK MSG";
                    DataOutputStream outToServer = new DataOutputStream(server.getOutputStream());
                    outToServer.writeBytes(msgToSend);
                    publishProgress(br.readLine());
                    server.close();
                } catch (IOException e) {
                    Log.e(TAG, "publish progress failed");
                }
            }
        }
        
        /* -------------------------------------------------------------------------------
        * @Override
        * @name onProgressUpdate() 
        * @desc This method displays what is received in doInBackground().  
        * @param strings
        * @return void
        ---------------------------------------------------------------------------------- */
        protected void onProgressUpdate(String...strings) {
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");
            TextView localTextView = (TextView) findViewById(R.id.textView1);
            localTextView.append("\n");
            
            // Need to build Uri object and ContentValues to insert the message into the file system
            // Building URI object
            Uri uri = Uri.parse("content://edu.buffalo.cse.cse486586.groupmessenger1.provider");
            
            // Building ContentValues Object with key as sequence number and value as the msg received
            ContentValues contVal = new ContentValues();
            contVal.put(KEY_FIELD, Integer.toString(sequenceNum));
            contVal.put(VALUE_FIELD,strReceived);
            
            // Inserting
            getContentResolver().insert(uri, contVal);
            sequenceNum++;
            return;
        }
    }

    /* ---------------------------------------------------------------
    * ClientTask is an AsyncTask that should send a string over the
    * network. It is created by ClientTask.executeOnExecutor() call 
    * whenever OnKeyListener.onKey() detects an enter key press event.
    ----------------------------------------------------------------*/
    private class ClientTask extends AsyncTask<String, Void, Void> {

        /* -------------------------------------------------------------------------------
        * @Override
        * @name doInBackground() 
        * @desc This method sends messages to all the AVD's  
        * @param msgs
        * @return Void
        ---------------------------------------------------------------------------------- */
        protected Void doInBackground(String... msgs) {
            try { 
                String[] remotePorts = {REMOTE_PORT0, REMOTE_PORT1, REMOTE_PORT2, REMOTE_PORT3, REMOTE_PORT4};
                for(int i = 0; i < 5; i++) {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePorts[i]));
                    
                    // OutputStream
                    String msgToSend = msgs[0];
                    DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
                    outToServer.writeBytes(msgToSend);
                    outToServer.flush();
                    
                    // InputStream
                    InputStream is = socket.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    if (br.readLine().equals("ACK MSG")) {
                        socket.close();
                    }
                }
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }
            return null;
        }
    }
}
