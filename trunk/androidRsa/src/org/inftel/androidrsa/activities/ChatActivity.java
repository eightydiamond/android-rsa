
package org.inftel.androidrsa.activities;

import java.util.ArrayList;

import javax.security.cert.Certificate;

import org.inftel.androidrsa.R;
import org.inftel.androidrsa.adapters.ChatAdapter;
import org.inftel.androidrsa.rsa.KeyStore;
import org.inftel.androidrsa.rsa.RSA;
import org.inftel.androidrsa.steganography.Decode;
import org.inftel.androidrsa.utils.AndroidRsaConstants;
import org.inftel.androidrsa.xmpp.ChatMan;
import org.inftel.androidrsa.xmpp.Conexion;
import org.inftel.androidrsa.xmpp.RosterManager;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

import android.app.ListActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

public class ChatActivity extends ListActivity {
    private static final String TAG = "ChatActivity";
    private Connection connection;
    private ChatMan chatMan;
    private Roster roster;
    private static ArrayList<Message> listMessages = new ArrayList<Message>();
    private ChatAdapter adapter;
    private static ListView myListView;
    private String destJid;
    private String myJid;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        this.connection = Conexion.getInstance();
        this.roster = RosterManager.getRosterInstance();
        Chat chat = ChatMan.chat;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat);
        destJid = getIntent().getStringExtra("destJid");
        myJid = this.connection.getUser();

        if (chat == null) {
            chatMan.createChat(destJid, messageListener);
            chat = ChatMan.chat;
        }

        adapter = new ChatAdapter(this, listMessages);
        setListAdapter(adapter);
        myListView = getListView();

    }

    public void send(View view) {
        Message message = new Message(destJid);
        EditText editText = (EditText) findViewById(R.id.textInput);
        if (!chatMan.isCipher()) {
            try {
                message.setBody(editText.getText().toString());
                message.setFrom(myJid);
                message.setTo(destJid);
                chatMan.getChat().sendMessage(message);
                Log.d(TAG, "Enviando: " + message.getBody());
                editText.setText("");
                listMessages.add(message);
                myListView.setSelection(myListView.getAdapter().getCount() - 1);

            } catch (XMPPException e) {
                Log.d(TAG, "ERROR al enviar mensaje");
            }
        }
        else {
            // TODO obtener clave publica del destino y mandar mensaje cifrado
            Bitmap bm = adapter.getAvatarMap().get(destJid);
            try {
                Certificate cert = Decode.decode(bm);
                String encodedMessage = RSA.cipher(message.getBody(),
                        cert.getPublicKey());
                message.setBody(encodedMessage);
                message.setFrom(myJid);
                message.setTo(destJid);
                chatMan.getChat().sendMessage(message);
                Log.d(TAG, "Enviando: " + message.getBody());
                editText.setText("");
                listMessages.add(message);
                myListView.setSelection(myListView.getAdapter().getCount() - 1);
                // KeyStore.getInstance().setCertificate(AndroidRsaConstants.FRIEND_ALIAS
                // + RosterManager.findByJid(destJid), Decode.decode(bm)) ;

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private MessageListener messageListener = new MessageListener() {
        public void processMessage(Chat chat, Message message) {
            if (!chatMan.isCipher()) {
                if (message.getBody() != null) {
                    // Log.i(TAG, "Recibido mensaje: " + message.getBody());
                    listMessages.add(message);
                    refreshAdapter();
                    myListView.smoothScrollToPosition(adapter.getCount() - 1);

                }
            }
            else {
                if (message.getBody() != null) {
                    // Getting the passphrase to encrypt the private Key
                    SharedPreferences prefs = getSharedPreferences(
                            AndroidRsaConstants.SHARED_PREFERENCE_FILE,
                            Context.MODE_PRIVATE);
                    String passphrase = prefs.getString(AndroidRsaConstants.USERID,
                            "thisisapassphrasedefault");

                    try {
                        String decodedMessage = RSA.decipher(message.getBody(),
                                RSA.getPrivateKeyDecryted(KeyStore.getInstance().getPk(),
                                        passphrase));
                        Log.i(TAG, "Recibido mensaje: " + decodedMessage);

                        message.setBody(decodedMessage);
                        listMessages.add(message);
                        refreshAdapter();
                        myListView.smoothScrollToPosition(adapter.getCount() - 1);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

        }

    };

    private void refreshAdapter() {
        runOnUiThread(new Runnable() {
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onBackPressed() {
        chatMan.chat = null;
        super.onBackPressed();
    }

}