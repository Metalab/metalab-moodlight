package at.metalab.moodlight;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ClientEndpoint
@ServerEndpoint(value = "/colors/")
public class ColorSocket {

	private static String path;

	private final static Logger LOG = Logger.getLogger(ColorSocket.class
			.getName());

	private final static Set<Session> SESSIONS = Collections
			.synchronizedSet(new HashSet<Session>());

	public static MqttUnreliablePublisher mqttPub;
	
	@OnOpen
	public void onWebSocketConnect(Session session) {
		LOG.info("Socket Connected: " + session);
		SESSIONS.add(session);
	}

	@OnMessage
	public void onWebSocketText(Session session, String message) {
		setColor(message);
	}

	private static long lastUpdate = System.currentTimeMillis();

	public synchronized static void setColor(String color) {
		if (System.currentTimeMillis() - lastUpdate < 500) {
			return;
		}
		lastUpdate = System.currentTimeMillis();

		mqttPub.publish(color);
	}

	@OnClose
	public void onWebSocketClose(Session session, CloseReason reason) {
		SESSIONS.remove(session);
		LOG.fine("Socket Closed: " + reason);
	}

	@OnError
	public void onWebSocketError(Throwable cause) {
		cause.printStackTrace(System.err);
	}

	public static void setPath(String path) {
		ColorSocket.path = path;
	}
}