package roofmessage.roofmessageapp.background.io;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketListener;
import com.neovisionaries.ws.client.WebSocketState;

import org.json.JSONException;

import java.io.IOException;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import roofmessage.roofmessageapp.background.BackgroundManager;
import roofmessage.roofmessageapp.Flush;
import roofmessage.roofmessageapp.R;
import roofmessage.roofmessageapp.io.JSONBuilder;
import roofmessage.roofmessageapp.utils.Tag;

/**
 * Created by Jesse Saran on 7/13/2016.
 */
public class WebSocketManager extends BroadcastReceiver implements Flush{
    private static WebSocketManager webSocketManager;
    private static WebSocketFactory webSocketFactory;
    private WebSocket webSocket;
    private Context context;

    private final static String COOKIE = "cookie";
    private static final String WS_URL = "ws://" + Tag.BASE_URL + "/message_route/";

    private WebSocketManager(Context context) {
        this.context = context;
    }

    public static WebSocketManager getInstance(Context context) {
        if(webSocketManager == null) {
            webSocketFactory = new WebSocketFactory();
            webSocketManager = new WebSocketManager(context);
            Log.d(Tag.WEB_SOC_MANAGER, "WebsocketManager started from scratch");
        }
        return webSocketManager;
    }

    public boolean createConnection() {
        String state = webSocket == null ? "" : webSocket.getState().name();
        Log.d(Tag.WEB_SOC_MANAGER, "Attempting connection, websocket state [" + state + "]");

        if (webSocket == null) {
            webSocketFactory.setConnectionTimeout(5000);
            try {
                webSocket = webSocketFactory.createSocket(WS_URL);
            } catch (IOException e) {
                Log.e(Tag.WEB_SOC_MANAGER, "Factory failed to create websocket: " + e.getMessage(), e);
                return false;
            }
            webSocket.addHeader(COOKIE, getCookies());
            webSocket.addListener(new Listener());
        }

        if (webSocket.getState() == WebSocketState.CLOSED) {
            try {
                webSocket = webSocket.recreate();
            } catch (IOException e) {
                Log.d(Tag.WEB_SOC_MANAGER, "Could not recreate connection.", e);
                return false;
            }
        }

        //check state
        if (webSocket.getState() == WebSocketState.CREATED) {

            // Prepare an ExecutorService.
            ExecutorService es = Executors.newSingleThreadExecutor();
            Future<WebSocket> future = webSocket.connect(es);
            try
            {
                // Wait for the opening handshake to complete.
                future.get();
            }
            catch (ExecutionException e)
            {
                if (e.getCause() instanceof WebSocketException) {
                    Log.d(Tag.WEB_SOC_MANAGER, "Failed to connect: " + e.getMessage(), e);
                }
                return false;
            } catch (InterruptedException e) {
                Log.d(Tag.WEB_SOC_MANAGER, "Future interrupted", e);
                return false;
            }
        }

        Log.d(Tag.WEB_SOC_MANAGER, "State when leaving function: " + webSocket.getState());
        if ( webSocket.getState() == WebSocketState.OPEN ) {
            return true;
        }
        return false;
    }

    private String getCookies() {
        StringBuilder cookieStringBuilder = new StringBuilder();
        //add cookies in from http session
        SessionManager sessionManager = SessionManager.getInstance(context);
        CookieStore cookieStore = sessionManager.getCookieManager().getCookieStore();
        List<HttpCookie> cookies = cookieStore.getCookies();
        for (int i = 0; i < cookies.size(); ++i ) {
            Log.d(Tag.WEB_SOC_MANAGER, "COOKIE: " + cookies.get(i).getName() + " VAL: " + cookies.get(i).getValue());
            cookieStringBuilder.append(cookies.get(i));
            if(i+1 != cookies.size()) {
                cookieStringBuilder.append("; ");
            }
        }
        return cookieStringBuilder.toString();
    }

    public void sendString(String string) {

        if (webSocket != null) {
            webSocket.sendText(string);
        }
    }

    public void disconnect() {
        webSocket.disconnect();
    }

    public WebSocketState getState() {
        return webSocket.getState();
    }

    public String getLocalizeState() {
        String localizeState = "UNKNOWN";
        if (webSocket != null) {
            localizeState = localizeState(webSocket.getState());
        }
        return localizeState;
    }

    public String localizeState(WebSocketState webSocketState) {
        String state = "";
        if (webSocketState.equals(WebSocketState.CREATED)) {
            state = WebSocketManager.this.context.getString(R.string.status_created);
        } else if (webSocketState.equals(WebSocketState.CONNECTING)) {
            state = WebSocketManager.this.context.getString(R.string.status_connecting);
        } else if (webSocketState.equals(WebSocketState.OPEN)) {
            state = WebSocketManager.this.context.getString(R.string.status_open);
        } else if (webSocketState.equals(WebSocketState.CLOSING)) {
            state = WebSocketManager.this.context.getString(R.string.status_closing);
        } else if (webSocketState.equals(WebSocketState.CLOSED)) {
            state = WebSocketManager.this.context.getString(R.string.status_closed);
        }
        return state;
    }

    @Override
    public boolean flush() {
        Log.d(Tag.WEB_SOC_MANAGER, "Flushing now.");
        webSocket.disconnect();
        while (webSocket.getState() != WebSocketState.CLOSED) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Log.d(Tag.WEB_SOC_MANAGER, "Flush sleep interrupted.");
            }
        }
        Log.d(Tag.WEB_SOC_MANAGER, "Flushing done.");
        return true;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (!action.equals("") && action.equals(Tag.ACTION_LOCAL_SEND_MESSAGE)) {
                String jsonString = intent.getStringExtra(Tag.KEY_SEND_JSON_STRING);
                webSocket.sendText(jsonString);
            }
        }
    }

    private class Listener implements WebSocketListener {

        @Override
        public void onStateChanged(WebSocket websocket, WebSocketState newState) throws Exception {
            Intent intent = new Intent(Tag.ACTION_WEBSOC_CHANGE);
            intent.putExtra("state", localizeState(newState));
            Log.d(Tag.WEB_SOC_MANAGER, "State changed, sending broadcast. New state [" + newState + "], localized state [" + localizeState(newState) + "]");
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            context.sendBroadcast(intent);
        }

        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
            websocket.sendText(new JSONBuilder(JSONBuilder.Action.CONNECTED).toString());
        }

        @Override
        public void onConnectError(WebSocket websocket, WebSocketException cause) throws Exception {

        }

        @Override
        public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {

        }

        @Override
        public void onFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

        }

        @Override
        public void onContinuationFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

        }

        @Override
        public void onTextFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

        }

        @Override
        public void onBinaryFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

        }

        @Override
        public void onCloseFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

        }

        @Override
        public void onPingFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

        }

        @Override
        public void onPongFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

        }

        @Override
        public void onTextMessage(WebSocket websocket, String text) throws Exception {
            Log.d(Tag.WEB_SOC_MANAGER, "TEXT: " + text);
            Intent intent = new Intent(Tag.ACTION_LOCAL_RECEIVED_MESSAGE);
            intent.putExtra(Tag.KEY_MESSAGE, text);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }

        @Override
        public void onBinaryMessage(WebSocket websocket, byte[] binary) throws Exception {

        }

        @Override
        public void onSendingFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

        }

        @Override
        public void onFrameSent(WebSocket websocket, WebSocketFrame frame) throws Exception {

        }

        @Override
        public void onFrameUnsent(WebSocket websocket, WebSocketFrame frame) throws Exception {

        }

        @Override
        public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
        }

        @Override
        public void onFrameError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception {

        }

        @Override
        public void onMessageError(WebSocket websocket, WebSocketException cause, List<WebSocketFrame> frames) throws Exception {

        }

        @Override
        public void onMessageDecompressionError(WebSocket websocket, WebSocketException cause, byte[] compressed) throws Exception {

        }

        @Override
        public void onTextMessageError(WebSocket websocket, WebSocketException cause, byte[] data) throws Exception {

        }

        @Override
        public void onSendError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception {

        }

        @Override
        public void onUnexpectedError(WebSocket websocket, WebSocketException cause) throws Exception {

        }

        @Override
        public void handleCallbackError(WebSocket websocket, Throwable cause) throws Exception {

        }

        @Override
        public void onSendingHandshake(WebSocket websocket, String requestLine, List<String[]> headers) throws Exception {

        }
    }
}
