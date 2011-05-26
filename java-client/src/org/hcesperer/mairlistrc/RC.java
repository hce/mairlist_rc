package org.hcesperer.mairlistrc;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

public class RC implements StatusHandler {
	private static final String DATE_FORMAT_NOW = "HH:mm";
	private static RC rc;
	private JFrame main;
	private JLabel statuslabel;
	private JTextArea infotext;

	private Thread t;

	private HandlerThread handlerthread;
	private JLabel pinglabel;

	public RC() {
		this.main = new JFrame();
		this.statuslabel = new JLabel("Lade...");
		Font font = new Font(Font.SANS_SERIF, Font.BOLD, 14);
		this.statuslabel.setFont(font);
		this.pinglabel = new JLabel("Nicht verbunden.");
		this.infotext = new JTextArea();
		this.infotext.setEditable(false);
		try {
			URL res = this.getClass().getResource("/README");
			int len = res.openConnection().getContentLength();
			byte[] bytes = new byte[len];
			InputStream readmeStream = res.openStream();
			readmeStream.read(bytes);
			readmeStream.close();
			String readme = new String(bytes, "utf-8");
			this.infotext.setText(readme);
		} catch (IOException e) {
			this.infotext.setText("Kann auf /README-Resource nicht zugreifen!");
		}
		this.main.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridy = 0;
		this.main.add(statuslabel, gbc);
		gbc = new GridBagConstraints();
		gbc.gridy = 1;
		this.main.add(pinglabel, gbc);
		gbc = new GridBagConstraints();
		gbc.gridy = 2;
		this.main.add(infotext, gbc);

		this.main.setTitle("HC's mAirList remote controller");

		this.main.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		this.handlerthread = new HandlerThread(this);
		this.t = new Thread(handlerthread);
		this.t.setDaemon(true);
		this.t.setName("Worker thread");
		this.t.start();
	}

	public void show() {
		this.main.pack();
		this.main.setVisible(true);
	}

	public void check() {
		this.status("Greife auf mAirList zu...");
		try {
			Socket s = new Socket("127.0.0.1", 9300);
			OutputStream os = s.getOutputStream();
			os.flush();
			s.close();
		} catch (IOException e) {
			JOptionPane
					.showMessageDialog(
							this.main,
							"Kann auf mAirList nicht zugreifen! Programm wird beendet.\n\n"
									+ "Foglendes muß in mAirLists config/remote.ini eingetragen werden:\n"
									+ "    [REMOTE1]\n" + "    type=network\n"
									+ "    port=9300");
			System.exit(1);
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		rc = new RC();
		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				rc.check();
				rc.show();
			}
		});
	}

	@Override
	public void status(String s) {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
		String curtime = sdf.format(cal.getTime());
		this.statuslabel.setText(curtime + " " + s);
		this.main.invalidate();
	}

	@Override
	public void message(String s) {
		System.out.println("Statusnachricht: " + s);
		JFrame message = new JFrame("Nachricht");
		message.add(new JLabel(s));
		message.setSize(320, 72);
		message.setVisible(true);
	}

	@Override
	public void ping(String s) {
		this.pinglabel.setText(s);
		this.main.invalidate();
	}

}
