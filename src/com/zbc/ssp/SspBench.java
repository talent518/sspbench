package com.zbc.ssp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.zbc.ssp.crypt.Crypt;

public class SspBench implements Runnable {
	public static class ClientInfo {
		public static final int STEP_KEY = 0;
		public static final int STEP_LOGIN = 1;
		public static final int STEP_REQUEST = 2;

		public ByteBuffer buf;

		public ByteBuffer data = null;
		public int index = 0;
		public String recvKey;
		public String sendKey;
		public int size = 0;
		public int sized = 0;
		public int step = 0;
		public int times = 0;

		public ClientInfo(int i) {
			super();
			this.buf = ByteBuffer.allocate(bufferSize);
			this.index = i;

			byte[] chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".getBytes();
			byte[] bytes = new byte[128];
			int n;
			for (i = 0; i < 128; i++) {
				n = (int) (Math.random() * chars.length);
				bytes[i] = chars[n];
			}

			sendKey = new String(bytes);
		}
	}

	private static SocketAddress addr = null;
	private static int bufferSize = 8192;
	private static Charset charset = Charset.forName("UTF-8");

	private static AtomicInteger conns = new AtomicInteger(0);
	private static AtomicInteger keys = new AtomicInteger(0);
	private static AtomicInteger loginFailures = new AtomicInteger(0);
	private static AtomicInteger loginOfflines = new AtomicInteger(0);
	private static AtomicInteger logins = new AtomicInteger(0);
	private static int nconns = 100;
	private static int nrequests = 10000;
	private static int nthreads = Runtime.getRuntime().availableProcessors() * 2;
	private static final Option OPTION_HOST = new Option("H", "host", true, "mysql host(default: localhost)");
	private static final Option OPTION_NAME = new Option("N", "name", true, "mysql database name(default: ssp)");

	private static final Option OPTION_PASSWORD = new Option("W", "password", true, "mysql password(default is empty)");

	private static final Option OPTION_PORT = new Option("P", "port", true, "mysql port(default: 3306)");
	private static final Option OPTION_SCONN = new Option("c", "conns", true, "ssp server conns pre a thread(default: " + nconns + ")");
	private static final Option OPTION_SHOST = new Option("h", "host", true, "ssp server host(default: 127.0.0.1)");
	private static final Option OPTION_SPORT = new Option("p", "port", true, "ssp server port(default: 8086)");
	private static final Option OPTION_SREQUEST = new Option("r", "requests", true, "ssp server requests pre a connect(default: " + nrequests + ")");

	private static final Option OPTION_STHREAD = new Option("t", "threads", true, "ssp server threads(default: " + nthreads + ")");
	private static final Option OPTION_USER = new Option("U", "user", true, "mysql user(default: root)");
	private static final Options OPTIONS = initOptions();
	private static AtomicInteger requestFailures = new AtomicInteger(0);
	private static AtomicInteger requests = new AtomicInteger(0);
	private static AtomicInteger threads = new AtomicInteger(0);

	private static List<String> users = new ArrayList<String>();

	private static Options initOptions() {
		Options options = new Options();
		options.addOption(OPTION_SHOST);
		options.addOption(OPTION_SPORT);
		options.addOption(OPTION_SCONN);
		options.addOption(OPTION_SREQUEST);
		options.addOption(OPTION_STHREAD);
		options.addOption(OPTION_HOST);
		options.addOption(OPTION_PORT);
		options.addOption(OPTION_NAME);
		options.addOption(OPTION_USER);
		options.addOption(OPTION_PASSWORD);
		return options;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		parseArgs(args);

		Thread thread;
		int i;

		for (i = 0; i < nthreads; i++) {
			thread = new Thread(new SspBench(i));
			thread.start();
		}

		int n;
		int _conns = 0, _keys = 0, _logins = 0, _loginOfflines = 0, _loginFailures = 0, _requests = 0, _requestFailures = 0;
		int __conns, __keys = 0, __logins, __loginOfflines, __loginFailures, __requests, __requestFailures;
		do {
			double tmpTime = new Date().getTime() / 1000.0;
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}

			n = threads.get();
			__conns = conns.get();
			__keys = keys.get();
			__logins = logins.get();
			__loginOfflines = loginOfflines.get();
			__loginFailures = loginFailures.get();
			__requests = requests.get();
			__requestFailures = requestFailures.get();

			tmpTime = new Date().getTime() / 1000.0 - tmpTime;

			System.out.printf("threads: %d, conns: %d(%.1f), keys: %d(%.1f), logins: %d(%.1f), loginOfflines: %d(%.1f), loginFailures: %d(%.1f), requests: %d(%.1f), requestFailures: %d(%.1f)\n", n, __conns, (__conns - _conns) / tmpTime, __keys, (__keys - _keys) / tmpTime, __logins, (__logins - _logins) / tmpTime, __loginOfflines, (__loginOfflines - _loginOfflines) / tmpTime, __loginFailures, (__loginFailures - _loginFailures) / tmpTime, __requests, (__requests - _requests) / tmpTime, __requestFailures, (__requestFailures - _requestFailures) / tmpTime);

			_conns = __conns;
			_keys = __keys;
			_logins = __logins;
			_loginOfflines = __loginOfflines;
			_loginFailures = __loginFailures;
			_requests = __requests;
			_requestFailures = __requestFailures;
		} while (n > 0);

		System.exit(0);
	}

	private static void parseArgs(String[] args) {
		if (args.length == 0) {
			String syntax = "sspb [options]";
			HelpFormatter helpFormatter = new HelpFormatter();
			helpFormatter.printHelp(syntax, OPTIONS);

			System.exit(255);

			return;
		}

		CommandLineParser commandLineParser = new PosixParser();
		CommandLine commandLine;
		try {
			commandLine = commandLineParser.parse(OPTIONS, args);
		} catch (ParseException e) {
			e.printStackTrace();
			System.exit(1);
			return;
		}

		String shost = "127.0.0.1";
		if (commandLine.hasOption(OPTION_SHOST.getOpt())) {
			shost = commandLine.getOptionValue(OPTION_SHOST.getOpt());
		}

		int sport = 8086;
		if (commandLine.hasOption(OPTION_SPORT.getOpt())) {
			sport = Integer.parseInt(commandLine.getOptionValue(OPTION_SPORT.getOpt()));
		}

		addr = new InetSocketAddress(shost, sport);

		if (commandLine.hasOption(OPTION_STHREAD.getOpt())) {
			nthreads = Integer.parseInt(commandLine.getOptionValue(OPTION_STHREAD.getOpt()));
		}

		if (commandLine.hasOption(OPTION_SCONN.getOpt())) {
			nconns = Integer.parseInt(commandLine.getOptionValue(OPTION_SCONN.getOpt()));
		}

		if (commandLine.hasOption(OPTION_SREQUEST.getOpt())) {
			nrequests = Integer.parseInt(commandLine.getOptionValue(OPTION_SREQUEST.getOpt()));
		}

		String host = "localhost";
		if (commandLine.hasOption(OPTION_HOST.getOpt())) {
			host = commandLine.getOptionValue(OPTION_HOST.getOpt());
		}

		int port = 3306;
		if (commandLine.hasOption(OPTION_PORT.getOpt())) {
			port = Integer.parseInt(commandLine.getOptionValue(OPTION_PORT.getOpt()));
		}

		String name = "ssp";
		if (commandLine.hasOption(OPTION_NAME.getOpt())) {
			name = commandLine.getOptionValue(OPTION_NAME.getOpt());
		}

		String user = "root";
		if (commandLine.hasOption(OPTION_USER.getOpt())) {
			user = commandLine.getOptionValue(OPTION_USER.getOpt());
		}

		String password = "";
		if (commandLine.hasOption(OPTION_PASSWORD.getOpt())) {
			password = commandLine.getOptionValue(OPTION_PASSWORD.getOpt());
		}

		queryUsers(host, port, name, user, password);
	}

	private static void queryUsers(String host, int port, String name, String user, String password) {
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(2);
		}

		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;

		try {
			conn = java.sql.DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + name + "?useUnicode=true&characterEncoding=UTF-8", user, password);

			stmt = conn.createStatement();

			rs = stmt.executeQuery("SELECT username FROM uc_members LIMIT " + (nthreads * nconns));

			while (rs.next()) {
				users.add(rs.getString(1));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(2);
		} finally {
			try {
				rs.close();
			} catch (SQLException e) {
			}
			try {
				stmt.close();
			} catch (SQLException e) {
			}
			try {
				conn.close();
			} catch (SQLException e) {
			}
		}
	}

	private int index = 0;

	private Selector selector = null;

	public SspBench() {
		super();
	}

	public SspBench(int i) {
		super();

		index = i;
	}

	@SuppressWarnings("deprecation")
	public Boolean doRecv(SocketChannel ch, ClientInfo p) {
		String xmlString = new String(p.data.array(), charset);
		SAXReader saxReader = new SAXReader();

		try {
			Document doc = saxReader.read(new ByteArrayInputStream(xmlString.getBytes(charset)));
			Element root = doc.getRootElement();

			String type = root.attribute("type").getValue();

			if (type.equalsIgnoreCase("Connect.Data")) {
				xmlString = Crypt.decode(root.getText(), p.recvKey);
				if (xmlString.length() > 0) {
					doc = saxReader.read(new ByteArrayInputStream(xmlString.getBytes(charset)));
					root = doc.getRootElement();
					type = root.attribute("type").getValue();
				} else {
					return false;
				}
			}

			// System.out.println("doRecv(" + index + "," + p.index + "): " + xmlString);

			switch (p.step) {
			case ClientInfo.STEP_KEY:
				if (type.equalsIgnoreCase("Connect.Key")) {
					keys.incrementAndGet();
					
					p.recvKey = root.getText();
					p.step = ClientInfo.STEP_LOGIN;

					int i = index * nconns + p.index;
					String user = users.get(i % users.size());

					return doSend(ch, p, "<request type=\"User.Login\"><params><username>" + user + "</username><password>123456</password><timezone>" + (-new Date().getTimezoneOffset() * 60) + "</timezone></params></request>", true);
				} else {
					System.out.println(root.getText());

					return false;
				}
			case ClientInfo.STEP_LOGIN:
				if (type.equalsIgnoreCase("User.Login.Succeed")) {
					p.step = ClientInfo.STEP_REQUEST;

					logins.incrementAndGet();

					return doSend(ch, p, "<request type=\"Gold.State\"/>", true);
				} else {
					loginFailures.incrementAndGet();

					System.out.println(root.getText());

					return false;
				}
			case ClientInfo.STEP_REQUEST:
				if (type.equalsIgnoreCase("Gold.State.Succeed")) {
					requests.incrementAndGet();

					return doSend(ch, p, "<request type=\"Gold.State\"/>", true);
				} else if (type.equalsIgnoreCase("Gold.State.Failed")) {
					requestFailures.incrementAndGet();

					return doSend(ch, p, "<request type=\"Gold.State\"/>", true);
				} else {
					loginOfflines.incrementAndGet();

					System.out.println(root.getText());

					return false;
				}
			}
		} catch (DocumentException e) {
			System.out.println("doRecv(" + index + "," + p.index + "): " + xmlString);

			// e.printStackTrace();

			return false;
		}

		return true;
	}

	public Boolean doSend(SocketChannel ch, ClientInfo p, String str) {
		return doSend(ch, p, str, false);
	}

	public Boolean doSend(SocketChannel ch, ClientInfo p, String str, Boolean isCrypt) {
		if (p.step == ClientInfo.STEP_REQUEST) {
			if (p.times >= nrequests) {
				return false;
			}
			p.times++;
		}

		// System.out.println("doSend(" + index + "," + p.index + "): " + str);

		if (isCrypt) {
			str = "<request type=\"Connect.Data\">" + Crypt.encode(str, p.sendKey) + "</request>";
		}

		byte[] bytes = str.getBytes(charset);
		int size = bytes.length;
		ByteBuffer buf = ByteBuffer.allocate(size + 4);
		int i;

		buf.clear();
		for (i = 0; i < 4; i++) {
			buf.put((byte) (size >> ((3 - i) * 8)));
		}
		buf.put(bytes);

		try {
			buf.flip();
			i = ch.write(buf);
			if (buf.limit() != i) {
				System.out.println("not send complete: " + i + ", " + buf.limit());
				return false;
			}

			return true;
		} catch (IOException e) {
			System.out.println("write error: " + e.getMessage());
			e.printStackTrace();

			return false;
		}
	}

	private Boolean readBuffer(SocketChannel sc, ClientInfo p) {
		ByteBuffer buf = p.buf;

		buf.clear();

		int len = 0;

		try {
			len = sc.read(buf);

			if (len > 0) {// 非阻塞，立刻读取缓冲区可用字节
				buf.flip();

				int offset = 0;

				do {
					if (p.size == 0) {
						if (offset + 4 > len) throw new Exception("read header error");
						int i;
						for (i = offset; i < 4; i++) {
							p.size += ((buf.get(i) & 0xff) << ((3 - i) * 8));
						}
						offset += 4;
						p.data = ByteBuffer.allocate(p.size);
					}
					int len2 = Math.min(p.size - p.sized, len - offset);
					if (len2 <= 0) break;
					p.data.put(buf.array(), offset, len2);

					p.sized += len2;

					// System.out.println("Recving(" + index + "," + p.index + "): " + str);

					if (p.size == p.sized) {
						if (!doRecv(sc, p)) return false;

						p.size = 0;
						p.sized = 0;
						p.data = null;
					}

					offset += len2;
				} while (offset < len);
			} else if (len == -1) {
				// System.out.println("客户端断开。。。");

				return false;
			}

			return true;
		} catch (Exception e) {
			System.out.println("read error: " + e.getMessage());
			e.printStackTrace();

			return false;
		}
	}

	public void run() {
		SocketChannel ch;
		ClientInfo p;
		int i, n, selectorSize = 0;

		try {
			selector = Selector.open();
		} catch (IOException e1) {
			// e1.printStackTrace();
			return;
		}

		threads.incrementAndGet();

		for (i = 0; i < nconns; i++) {
			try {
				ch = SocketChannel.open();
				p = new ClientInfo(i);

				ch.configureBlocking(false);
				ch.connect(addr);
				ch.register(selector, SelectionKey.OP_CONNECT, p);
				selectorSize++;
			} catch (Exception e) {
				// e.printStackTrace();
			}
		}

		while (selectorSize > 0) {
			try {
				n = selector.select();
			} catch (IOException e) {
				throw new RuntimeException("Selector.select()异常!");
			}

			if (n == 0) {
				continue;
			}

			Set<SelectionKey> keys = selector.selectedKeys();
			Iterator<SelectionKey> iter = keys.iterator();

			while (iter.hasNext()) {
				SelectionKey key = iter.next();

				iter.remove();

				ch = (SocketChannel) key.channel();
				p = (ClientInfo) key.attachment();
				
				if(key.isConnectable()) {
					try {
						ch.finishConnect();
						Boolean b = doSend(ch, p, "<request type=\"Connect.Key\">" + p.sendKey + "</request>");
						if (b) {
							key.interestOps(SelectionKey.OP_READ);
							key.selector().wakeup();
							conns.incrementAndGet();
						} else {
							key.interestOps(key.interestOps() & (~SelectionKey.OP_CONNECT));
							key.cancel();
							ch.shutdownInput();
							ch.shutdownOutput();
							ch.close();
							selectorSize--;
						}
					} catch (IOException e) {
						key.interestOps(key.interestOps() & (~SelectionKey.OP_CONNECT));
						key.cancel();
						selectorSize--;
					}
					continue;
				}

				if (readBuffer(ch, p)) {
					// 没有可用字节,继续监听OP_READ
					key.interestOps(key.interestOps() | SelectionKey.OP_READ);
					key.selector().wakeup();
					continue;
				}
				
				if(p.step != ClientInfo.STEP_KEY) {
					SspBench.keys.decrementAndGet();
				}

				conns.decrementAndGet();
				selectorSize--;

				key.interestOps(key.interestOps() & (~SelectionKey.OP_READ));
				key.cancel();
				try {
					ch.shutdownInput();
					ch.shutdownOutput();
					ch.close();
				} catch (IOException e) {
				}
			}
		}

		try {
			selector.close();
		} catch (IOException e) {
			// e.printStackTrace();
		}

		threads.decrementAndGet();
	}

}
