package study.java.net.Socket;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Calendar;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;


@SuppressWarnings("serial")
public class ChatWindow extends JFrame implements ActionListener {

	private JMenuBar menuBar;
	private JMenu clientMenu;
	private JMenuItem startClientSocketMI, httpClientMenuItem, closeWindow;

	private Container container;
	private JTextArea historyTextArea;
	private JScrollPane scorllTextArea;
	private JTextField sendTextField;
	private JButton sendBtn;
	private JPanel buttomPanel;

	private Calendar calendar;

//	private int clientType;
//	private final int NONE = 0, SOCKETCLIENT = 1, HTTPCLIENT = 2;

	private MainFrame mainFrame;
//	private SocketConnection socketConn;

	public ChatWindow(MainFrame main, String title, int width, int height) {
		this.setTitle(title);
		mainFrame = main;
		menuBar = new JMenuBar();
		container = this.getContentPane();

		clientMenu = new JMenu("客户端参数");
		startClientSocketMI = new JMenuItem("连接远程ServerSocket");
//		httpClientMenuItem = new JMenuItem("Http客户端");
		closeWindow = new JMenuItem("关闭窗口");
		startClientSocketMI.addActionListener(this);
//		httpClientMenuItem.addActionListener(this);
		closeWindow.addActionListener(this);
		clientMenu.add(startClientSocketMI);
//		clientMenu.add(httpClientMenuItem);
		clientMenu.add(closeWindow);
        menuBar.add(clientMenu);
		
		setJMenuBar(menuBar);

		historyTextArea = new JTextArea();
		historyTextArea.setEditable(false);
		historyTextArea.setBackground(Color.lightGray);
		scorllTextArea = new JScrollPane(historyTextArea);
		scorllTextArea.setBorder(BorderFactory.createEmptyBorder(3, 3, 5, 3));

		sendTextField = new JTextField();
		sendTextField.addActionListener(this);
		sendBtn = new JButton("发送");
		sendBtn.addActionListener(this);
		buttomPanel = new JPanel();
		buttomPanel.setLayout(new BorderLayout());
		buttomPanel.add(sendTextField, BorderLayout.CENTER);
		buttomPanel.add(sendBtn, BorderLayout.LINE_END);
		buttomPanel.setBorder(BorderFactory.createEmptyBorder(1, 3, 3, 3));

		container.add(scorllTextArea, BorderLayout.CENTER);
		container.add(buttomPanel, BorderLayout.SOUTH);

		calendar = Calendar.getInstance();
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
//			    myLog("退出用户界面。。。");
			    close();
			}
		});
		this.setPreferredSize(new Dimension(width, height));
	    Dimension screenSize =Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation((screenSize.width - width)/2, (screenSize.height - height)/2);

        eventQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();
	}
	public void open(){
	    this.pack();
	    this.setVisible(true);
	}
	public void close(){
	    this.setVisible(false);
	}

	public void myLog(String name, String msg){
        eventQueue.postEvent( new LOGAWTEvent( this, name, msg));
	}
    public void processLog(String name, String msg){
        int hour, minute, second;
        calendar.setTimeInMillis(System.currentTimeMillis());
        hour = calendar.get(Calendar.HOUR_OF_DAY);
        minute = calendar.get(Calendar.MINUTE);
        second = calendar.get(Calendar.SECOND);
        String content = name + "（" + hour + ":" + minute + ":" + second + "）：" + msg;
        historyTextArea.append(content + "\n");
        System.out.println(content);
    }
    
    private EventQueue eventQueue = null;
    @Override
    protected void processEvent(AWTEvent e) {
        // TODO Auto-generated method stub
        if ( e instanceof LOGAWTEvent )
        {
            LOGAWTEvent ev = (LOGAWTEvent) e;
//            System.out.print(ev.getName());
            processLog(ev.getName(), ev.getMessage());
        }else{
            super.processEvent(e);
        }
    }
    
	
	// private SimpleHttpServer httpServer;
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		JComponent item = (JComponent) e.getSource();
        if(item.equals(closeWindow)){
            close();
        }else
		if (item.equals(sendBtn) || item.equals(sendTextField)) {
			// System.out.println("sendTextField");
			String text = sendTextField.getText();
			if (!text.equals("")) {
//				socketConn.sendMessage(text);
                sendTextField.setText("");
			}
		}
	}
	
	@SuppressWarnings("serial")
	class LOGAWTEvent extends AWTEvent {
	    public static final int EVENT_ID = AWTEvent.RESERVED_ID_MAX + 1;
	    private String mName;
	    private String mMessage;
	
	    LOGAWTEvent(Object target, String name, String message) {
	        super(target, EVENT_ID);
	        mName = name;
	        mMessage = message;
	    }
	
	    public String getName() {
	        return mName;
	    }
	
	    public String getMessage() {
	        return mMessage;
	    }
	}
}


