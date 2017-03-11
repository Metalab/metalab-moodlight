package at.metalab.moodlight;

import artnet4j.ArtNet;
import artnet4j.ArtNetServer;
import artnet4j.packets.ArtDmxPacket;

public class ArtnetSender {

	private static ArtNet artnet;
	
	private static int burstMode = Integer.valueOf(System.getProperty("moodlight.burstMode", "1"));
	
	static { 
		ArtNetServer ans = new ArtNetServer(9999, ArtNetServer.DEFAULT_PORT);
		
		artnet = new ArtNet(ans);
		artnet.init();
		
		try {
			artnet.start();
		} catch(Exception exception) {
			artnet = null;
		}
	}

	public static void send(int r, int g, int b) {
		// both whites are fixed to 0
		byte[] data = new byte[] {
				(byte) r, (byte) g, (byte) b, (byte) 0, (byte) 0, // Lampe 1
				(byte) r, (byte) g, (byte) b, (byte) 0, (byte) 0 // Lampe 2
		};

		ArtDmxPacket packet = new ArtDmxPacket();
		packet.setUniverse(0, Integer.valueOf(System.getProperty("artnetUniverse", "1")));
		packet.setDMX(data, 10);

		send(packet);
	}

	private static void send(final ArtDmxPacket packet) {
		if(artnet == null) {
			return;
		}
		
		for(int i = 0; i < burstMode; i++) {
			try {
				artnet.unicastPacket(packet, System.getProperty("artnetIP", "10.20.255.255"));
			} catch (Throwable t) {
				System.out.println("send-artnet failed: " + t);
				t.printStackTrace(System.out);
			}
		}
	}

}
