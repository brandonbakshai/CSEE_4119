import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

// client class which establishes a connection
// to the server
public class Client extends JFrame implements Runnable
{

    public Socket socket;
    static BufferedReader in;
    static BufferedReader serverIn;
    static PrintWriter out;
    static JTextArea chat;
    static String serverText;
    static String name;
    
    public Client(String hostName, int portNumber) throws UnknownHostException, IOException, InterruptedException
    {
        // establish connection to server
    	socket = new Socket(hostName, portNumber);
		in = new BufferedReader(new InputStreamReader(System.in));
		serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new PrintWriter(socket.getOutputStream());
		
        // construct JFrame
		Container cp = getContentPane();
        cp.setLayout(new GridLayout());
        JTextArea field;
        field = new JTextArea("");
        field.setEditable(false);
        cp.add(field);
        field.setMaximumSize(getMaximumSize());
        JScrollPane scroll = new JScrollPane (field,
	    		   JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        cp.add(scroll);
        chat = field;
	      
	 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("chat program");
        setSize(300, 300);
        setVisible(true);
		
        // begin infinite loop of username and passoword validation
        // and command prompting.
        // the loop only begins over again if command is interrupted,
        // meaning the user logs out
	    while (true)
	    {
            field.setText("");
            username();
            password();
            Thread thread = new Thread(this);
            thread.start();
            command();
            thread.sleep(2000);
    	}
    }
  
    // handles validation of username
	public static void username() throws IOException 
	{
		
		while (true) 
		{	
			System.out.print("username: ");
			
			String text = in.readLine();
			out.println(text);
			out.flush();
			
			serverText = serverIn.readLine();
			if (serverText.equals("success"))
			{
				name = text;
				break;
			}
		}
	}
	
	// handles validation of password
	public static void password() throws IOException 
	{	
		while (true) 
		{	
			System.out.print("password: ");
			
			String text = in.readLine();
			out.println(text);
			out.flush();
			
			serverText = serverIn.readLine();
			if (serverText.equals("Welcome to simple chat server!"))
			{
				System.out.println(serverText);
				break;
			}
		}
	}
	
	// handles command delivery
	public static void command() throws IOException 
	{	
		while (true) 
		{	
			System.out.print("command: ");
			
			String text = in.readLine();
			if (!(text == null))
				out.println(text);
				out.flush();
      	   
			if (text.equals("logout"))
				System.exit(0);
		}
	}
	
    // handles logout on the client side
	public static void logout() 
	{
		out.println("logout");
		out.flush();
	}
	
    // thread to continuously listen for server response
	@Override
	public void run() 
	{
		while (true) 
		{
			try 
			{
				while (!(serverText = serverIn.readLine()).equals("done"))
				{
					chat.setText(chat.getText() + "\n" + serverText);
					if (serverText.equals("logout")) 
					{
						System.exit(0);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
    
	// main method
	public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException
	{
		String hostName = args[0];
		int portNumber = Integer.parseInt(args[1]);
		
		try
		{
            // start client process
            Client client = new Client(hostName, portNumber);
		} catch (InterruptedException e)
		{
			logout();
			System.exit(0);
		} catch (InterruptedIOException e)
		{
			logout();
			System.exit(0);
		}
		
	}
}
