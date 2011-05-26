package org.hcesperer.mairlistrc;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.net.ssl.HttpsURLConnection;

public class HandlerThread implements Runnable {

	private static final String DATE_FORMAT_NOW = "HH:mm:ss";

	private boolean exit = false;

	private InputStream is = null;

	private HttpsURLConnection huc = null;

	private final StatusHandler sh;

	private DataInputStream dis;

	public HandlerThread(StatusHandler sh) {
		this.sh = sh;
	}

	public void doExit() {
		this.exit = true;
	}

	@Override
	public void run() {
		while (!this.exit) {
			try {
				mainloop();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void status(String s) {
		System.out.println("Status: " + s);
		sh.status(s);
	}

	private void mainloop() throws InterruptedException {
		while (!this.exit) {
			/* Sleep at the beginning so we sleep after 'continue' */
			Thread.sleep(250);

			checkConnection();
			String command = null;
			try {
				command = readCommand();
			} catch (Exception e) {
				status("IOException/readcommand: " + e);
				this.huc = null;
				this.is = null;
				continue;
			}
			try {
				handleCommand(command);
			} catch (Exception e) {
				status("IOException/handlecommand: " + e);
				continue;
			}
		}
	}

	private void handleCommand(String command) throws IOException {
		if (command.startsWith("MSG ")) {
			sh.message(command.substring(4));
		} else if (command.startsWith("Still alive!")) {
			Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
			String curtime = sdf.format(cal.getTime());
			ping("Server ssl.hcesperer.org verbunden. " + curtime);
		} else if (command.startsWith("STOPALL")) {
			status("Stoppe alle Player.");
			command("PLAYER 1-1 STOP");
			command("PLAYER 1-2 STOP");
			status("Alle Player gestoppt.");
		} else {
			status("Kommando empfangen: " + command);
			command(command);
		}

	}

	private void ping(String string) {
		this.sh.ping(string);
	}

	private void command(String string) throws IOException {
		byte[] command;
		command = string.getBytes("utf-8");
		byte cmdlen = (byte) command.length;
		byte[] preamble = { 0x52, 0x4F, 0x31, 0x30, 0x37, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x28, 0x2A, 0x26, 0x15, 0x6A, 0x3E,
				(byte) 0xA3, 0x41, (byte) 0xBB, (byte) 0x92, 0x39, 0x2D, 0x63,
				(byte) 0xF3, 0x34, (byte) 0x88, 0x16, 0x00, 0x00, 0x00, 0x6D,
				0x41, 0x69, 0x72, 0x4C, 0x69, 0x73, 0x74, 0x43, 0x6F, 0x6D,
				0x6D, 0x61, 0x6E, 0x64, 0x53, 0x65, 0x72, 0x76, 0x69, 0x63,
				0x65, 0x0E, 0x00, 0x00, 0x00, 0x45, 0x78, 0x65, 0x63, 0x75,
				0x74, 0x65, 0x43, 0x6F, 0x6D, 0x6D, 0x61, 0x6E, 0x64, cmdlen,
				0x00, 0x00, 0x00 };
		int contentLength = preamble.length + command.length;
		String request = "POST /BIN HTTP/1.1\r\nAccept: application/octet-stream\r\nHost: 127.0.0.1:9300\r\nContent-Length: "
				+ String.valueOf(contentLength)
				+ "\r\nUser-Agent: RemObjects SDK\r\nCache-Control: no-cache\r\nConnection: Close\r\nContent-Type: application/binary\r\n\r\n";
		byte[] requestb = request.getBytes("utf-8");
		Socket s = new Socket("127.0.0.1", 9300);
		OutputStream os = s.getOutputStream();
		os.write(requestb);
		os.write(preamble);
		os.write(command);
		os.flush();
		s.close();
	}

	private String readCommand() throws IOException {
		byte len = dis.readByte();
		byte[] buf = new byte[len];
		dis.read(buf);
		return new String(buf, "utf-8");
	}

	private void checkConnection() {
		if ((huc == null) || (is == null)) {
			status("Connecting to ssl.hcesperer.org");
			try {
				huc = (HttpsURLConnection) new URL(
						"https://ssl.hcesperer.org/mairlist.yaws")
						.openConnection();
				huc.setReadTimeout(20000);
				is = huc.getInputStream();
				dis = new DataInputStream(is);
				byte[] magic  = new byte[4];
				dis.read(magic);
				if (!new String(magic, "utf-8").equals("hcok")) {
					status("Server hat Mist gesendet!");
					this.is = null;
					huc.disconnect();
					this.huc = null;
					return;
				}
				status("Verbunden mit ssl.hcesperer.org");
			} catch (MalformedURLException e) {
				status("Mißgeformte URL - Java-Installation prüfen!");
			} catch (IOException e) {
				status("IOException bei Verbindungsaufbau: " + e);
			}
		}
	}

}
