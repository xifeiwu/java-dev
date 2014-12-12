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
public class UserFrame extends JFrame implements ActionListener {

	private JMenuBar menuBar;
	private JMenu clientMenu;
	private JMenuItem startClientSocketMI, httpClientMenuItem, stopClientMI;
    private JMenu serverMenu;
    private JMenuItem startSocketServerMI, httpServerMenuItem, stopServerMI;

	private Container container;
	private JTextArea historyTextArea;
	private JScrollPane scorllTextArea;
	private JTextField sendTextField;
	private JButton sendBtn;
	private JPanel buttomPanel;

	private Calendar calendar;

	private int clientType;
	private final int NONE = 0, SOCKETCLIENT = 1, HTTPCLIENT = 2;

	private UserFrame instance;
	private SocketConnection socketConn;

	public UserFrame() {
		this.setTitle("用户界面");
		instance = this;
		menuBar = new JMenuBar();
		container = this.getContentPane();

		clientMenu = new JMenu("客户端参数");
		startClientSocketMI = new JMenuItem("连接远程ServerSocket");
//		httpClientMenuItem = new JMenuItem("Http客户端");
		stopClientMI = new JMenuItem("关闭连接");
		startClientSocketMI.addActionListener(this);
//		httpClientMenuItem.addActionListener(this);
		stopClientMI.addActionListener(this);
		clientMenu.add(startClientSocketMI);
//		clientMenu.add(httpClientMenuItem);
		clientMenu.add(stopClientMI);
        menuBar.add(clientMenu);
		
		serverMenu = new JMenu("服务器参数");
        startSocketServerMI = new JMenuItem("开启ServerSocket");
//        httpServerMenuItem = new JMenuItem("Http客户端");
        stopServerMI = new JMenuItem("关闭ServerSocket");
        startSocketServerMI.addActionListener(this);
//        httpServerMenuItem.addActionListener(this);
        stopServerMI.addActionListener(this);
        serverMenu.add(startSocketServerMI);
//        clientMenu.add(httpServerMenuItem);
        serverMenu.add(stopServerMI);
		menuBar.add(serverMenu);
		
		setJMenuBar(menuBar);

		historyTextArea = new JTextArea();
		historyTextArea.setEditable(false);
		historyTextArea.setBackground(Color.lightGray);
		scorllTextArea = new JScrollPane(historyTextArea);
		scorllTextArea.setBorder(BorderFactory.createEmptyBorder(3, 3, 5, 3));

		sendTextField = new JTextField();
		sendTextField.addActionListener(this);
		sendBtn = new JButton("发送");
		// sendBtn.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));
		sendBtn.addActionListener(this);
		buttomPanel = new JPanel();
		buttomPanel.setLayout(new BorderLayout());
		buttomPanel.add(sendTextField, BorderLayout.CENTER);
		buttomPanel.add(sendBtn, BorderLayout.LINE_END);
		buttomPanel.setBorder(BorderFactory.createEmptyBorder(1, 3, 3, 3));

		container.add(scorllTextArea, BorderLayout.CENTER);
		container.add(buttomPanel, BorderLayout.SOUTH);

		calendar = Calendar.getInstance();
		
		clientType = NONE;
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
//			    myLog("退出用户界面。。。");
//				closeClient();
				System.exit(0);
			}
		});

		socketConn = new SocketConnection(this);
        eventQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();
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
    
	private JDialog mDialog;
	private JButton confirmBtn, cancelBtn;
	private JTextField hostField, portField;
	private String destAddress;
	private int destPort;
	private void initDialog(){
	    JPanel mPanel = new JPanel();
        GridLayout mLayout = new GridLayout(0,2);
        mLayout.setHgap(10);
        mLayout.setVgap(10);
        JLabel hostLabel = new JLabel("IP地址：");
        JLabel portLabel = new JLabel("端口号：");

        hostField = new JTextField();
        hostField.setColumns(16);
        portField = new JTextField();
        portField.setColumns(16);
        
        hostLabel.setLabelFor(hostField);
        portLabel.setLabelFor(portField);
        
        confirmBtn = new JButton("确定");
        cancelBtn = new JButton("取消");
        confirmBtn.addActionListener(this);
        cancelBtn.addActionListener(this);

        mPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
//        mPanel.setPreferredSize(new Dimension(400, 0));
        mPanel.setLayout(mLayout);
        mPanel.add(hostLabel);
        mPanel.add(hostField);
        mPanel.add(portLabel);
        mPanel.add(portField);
        mPanel.add(confirmBtn);
        mPanel.add(cancelBtn);
        
        mDialog = new JDialog(this,true);
        mDialog.setTitle("输入IP和端口号");
        mDialog.getContentPane().add(mPanel);
        mDialog.pack();   
	}

    private void showDialog(){
	    if(null == mDialog){
	        initDialog();
	    }
        mDialog.setVisible(true);   
	}
	private void hideDialog(){
        mDialog.setVisible(false);
	}
	
	// private SimpleHttpServer httpServer;
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		JComponent item = (JComponent) e.getSource();
		if (item.equals(startClientSocketMI)) {
			System.out.println("socketClient");
			clientType = SOCKETCLIENT;
//			String inputValue = JOptionPane.showInputDialog("Input IP and Port for Destination Server:");
			showDialog();
//            myLog("You want to connet to serverw " + destAddress + ": " + destPort);
			socketConn.startClientSocket(destAddress, destPort);
		}else
        if (item.equals(stopClientMI)) {
            socketConn.stopClientSocket();          
        }
        if (item.equals(startSocketServerMI)) {
            socketConn.startServerSocket(9090);
        }else
		if(item.equals(stopServerMI)){
		    socketConn.stopServerSocket();
		}else
		if(item.equals(confirmBtn)){
		    destAddress = hostField.getText();
		    String portStr = portField.getText();
		    if((null != destAddress) && (null != portStr)){
		        destPort = Integer.parseInt(portStr);
//    		    myLog("You want to connet to server " + destAddress + ": " + destPort);
    		    portField.setText("");
    		    portField.setText("");
    		    this.hideDialog();
		    }
		}else
		if(item.equals(cancelBtn)){
            portField.setText("");
            portField.setText("");
            this.hideDialog();	    
		}else

		if (item.equals(sendTextField) || item.equals(sendBtn)) {
			// System.out.println("sendTextField");
			String text = sendTextField.getText();
			if (!text.equals("")) {
				sendTextField.setText("");
				socketConn.sendMessage(text);
//				switch (clientType) {
//				case SOCKETCLIENT:
//					appendToHistoryTextArea("客户端", text);
//					break;
//				case HTTPCLIENT:
//					appendToHistoryTextArea("客户端", text);
//					break;
//				case NONE:
//					appendToHistoryTextArea("提示", "服务器还没有启动");
//					break;
//				}
			}
		}
	}


//  private SocketClient socketClient;
//  private HttpClient httpClient;
//  private Thread serverThread;
    Runnable clientRun = new Runnable() {
        @Override
        public void run() {
            // TODO Auto-generated method stub
            switch(clientType){
            case SOCKETCLIENT:
//              socketClient = new SocketClient(instance);
                break;
            case HTTPCLIENT:
//              httpClient = new HttpClient(instance);
                break;
            }
        }

    };

//    private void closeClient(){
//        switch (clientType) {
//        case SOCKETCLIENT:
//            break;
//        case HTTPCLIENT:
//            break;
//        }
//    }

	public static void main(String[] args){
	    int WIDTH = 500;
	    int HEIGHT = 650;
        UserFrame userFrame = new UserFrame();
        userFrame.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        userFrame.pack();
        userFrame.setVisible(true);
//        userFrame.initDialog();
	}
}


//@SuppressWarnings("serial")
//class LOGAWTEvent extends AWTEvent {
//    public static final int EVENT_ID = AWTEvent.RESERVED_ID_MAX + 1;
//    private String mName;
//    private String mMessage;
//
//    LOGAWTEvent(Object target, String name, String message) {
//        super(target, EVENT_ID);
//        mName = name;
//        mMessage = message;
//    }
//
//    public String getName() {
//        return mName;
//    }
//
//    public String getMessage() {
//        return mMessage;
//    }
//}