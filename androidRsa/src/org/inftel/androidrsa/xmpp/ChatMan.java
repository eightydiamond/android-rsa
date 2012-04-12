
package org.inftel.androidrsa.xmpp;

import org.inftel.androidrsa.activities.ChatActivity;
import org.inftel.androidrsa.activities.ContactsActivity;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.MessageListener;

import android.content.Intent;
import android.util.Log;

public class ChatMan {
    private final static String TAG = "ChatActivity";
    private Connection connection;
    public static Chat chat = null;
    public static boolean cipher = false;
    private ContactsActivity activity;

    public ChatMan(ContactsActivity cActivity) {
        this.activity = cActivity;
        this.connection = Conexion.getInstance();
    }

    public void initListener() {
        // Listener para detectar si el chat lo crea el otro
        org.jivesoftware.smack.ChatManager chatmanager = connection.getChatManager();
        ChatManagerListener chatManagerListener = new ChatManagerListener() {
            public void chatCreated(Chat chat, boolean createdLocally)
            {
                if (!createdLocally) {
                    Log.d(TAG, "Chat Creado localmente por " + chat.getParticipant());
                    ChatMan.chat = chat;
                    Log.d(TAG, "Listeners: " + chat.getListeners().toString());

                    Intent i = new Intent(activity, ChatActivity.class);
                    activity.startActivity(i);
                }
            }

        };

        chatmanager.addChatListener(chatManagerListener);
    }

    public void createChat(String jidDest, MessageListener messageListener) {
        if (RosterManager.isSecure(jidDest)) {
            cipher = true;
        }
        else {
            cipher = false;
        }
        Log.d(TAG, "Creando chat con: " + jidDest + " cifrado=" + cipher);
        ChatManager chatmanager = connection.getChatManager();

        // Listener para recibir mensajes

        // Creo el chat
        chat = chatmanager.createChat(jidDest, messageListener);

    }

    public Chat getChat() {
        return chat;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }

    public boolean isCipher() {
        return cipher;
    }

    public void setCipher(boolean cipher) {
        this.cipher = cipher;
    }

}
