import java.util.*;
import java.io.*;
import java.net.*;

// a multithreaded server which accepts
// multiple clients and supports various
// functionalities
public class Server {

    // data fields
    static HashMap<String, User> users;
    int port;
    static int TIME_OUT = 30; \\ minutes
    static int ATTEMPTS = 3; 

    // constructors
    public Server(int port) throws FileNotFoundException {
    	users = popUsers();
    	this.port = port;
    }

    // methods
    public static HashMap<String, User> popUsers() throws FileNotFoundException {
    	Scanner scanner = new Scanner(new File(
    			"./user_pass.txt"));
	
	
    	HashMap<String, User> users = new HashMap<String, User>();
    	String c;

    	while (scanner.hasNextLine()) {

    		c = scanner.nextLine();
    		Scanner scannerLine = new Scanner(c);
	    
    		String name = scannerLine.next();
    		String pass = scannerLine.next();
	    	    
    		User user = new User();

    		user.name = name;
    		user.pass = pass;
	    
    		users.put(name, user);
	    
    		scannerLine.close();
		}
	
		scanner.close();

		return users; 
    }
    
    // program to check if two dates are of the same day
    public static boolean isSameDay(Calendar date1, Calendar date2, int num) 
    {
    	
    	boolean day_of_year = date1.get(Calendar.DAY_OF_YEAR) == 
    			date2.get(Calendar.DAY_OF_YEAR);
    	boolean year = date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR);
    	
    	if (!(day_of_year && year))
    		return false;
    	
    	int min1 = date1.get(Calendar.MINUTE);
    	int min2 = date2.get(Calendar.MINUTE);
    	
    	int hour1 = date1.get(Calendar.HOUR);
    	int hour2 = date2.get(Calendar.HOUR);
    	
    	int total1 = hour1 * 60 + min1;
    	int total2 = hour2 * 60 + min2;
    	    	
    	return (Math.abs(total1 - total2) <= num);
    }
    
    // print out all online users
    public static void printOnline(User user, PrintWriter out) 
    {
    	Iterator<String> iterator =  users.keySet().iterator();
    	String tmpName;
    	User tmpUser;
    	
    	while (iterator.hasNext()) 
    	{
    		tmpName = iterator.next();
    		tmpUser = users.get(tmpName);
    		if (tmpUser != user &&
    				tmpUser.logged_in)
    			out.println(tmpUser.name);
    	}
    	out.println("done");
    	out.flush();
    }
    
    // print out all users online in the last "last" minutes
    public static void printLast(User user, int last, PrintWriter out) 
    {
    	Iterator<String> iterator =  users.keySet().iterator();
    	String tmpName;
    	User tmpUser;
    	String output = "";
    	
    	while (iterator.hasNext()) 
    	{
    		tmpName = iterator.next();
    		tmpUser = users.get(tmpName);
    		Calendar time1 = tmpUser.logtime;
    		Calendar time2 = Calendar.getInstance();
    		
    		if (tmpUser != user &&
    				time1 != null &&
    				isSameDay(time1, time2, last)) {
    			output += (tmpUser.name + "\n");
    		}
    	}
    	output += "done";
    	out.println(output);
    	out.flush();
    }
    
    // "broadcast message" to all online users
    public static void sendOnline(User user, PrintWriter out, String message) 
    {
    	Iterator<String> iterator =  users.keySet().iterator();
    	String tmpName;
    	User tmpUser;
    	
    	while (iterator.hasNext()) 
    	{
    		tmpName = iterator.next();
    		tmpUser = users.get(tmpName);
    		if (tmpUser.logged_in &&
    					(tmpUser.socket != null)) {
    			tmpUser.out.println(message + "\ndone");
    		    tmpUser.out.flush();
    		}
    	}
    }
    
    // "broadcast user message" to listed users
    public static void sendOnlineUsers(User me, String users, PrintWriter out, String message) 
    {
    	User tmpUser;
    	
    	String[] userList = users.split(" ");
    	
    	
    	for (int i = 0; i < userList.length; i++) 
    	{
    		tmpUser = Server.users.get(userList[i]);
    		if (tmpUser.logged_in &&
    					(tmpUser.socket != null)) {
    			tmpUser.out.println(message + "\ndone");
    		    tmpUser.out.flush();
    		}
    		me.out.println(message + "\ndone");
    		me.out.flush();
    	}    	
    }
    
    // nested class containing user information
    public static class User implements Runnable {

        // data fields
        String name, pass, ip;
        int login_attempts;
        boolean logged_in;
        Calendar logtime;
        Socket socket;
        BufferedReader in;
    	PrintWriter out;
        static int BLOCK_TIME = 60;
        static long timer;
        static long timeout;
        static boolean blocked;
        static boolean go;
        HashMap<String, Integer> blocked_ips;
        

        // constructors
        public User(String name, String pass, String ip, Socket socket) {
            this.name = name;
            this.pass = pass;
            this.ip = ip;
            this.login_attempts = 0;
            this.logged_in = false;
            this.socket = socket;
            this.blocked = false;
            this.go = false;
            blocked_ips = new HashMap<String, Integer>();
        }
        
        public User(String name, String pass, String ip) {
            this.name = name;
            this.pass = pass;
            this.ip = ip;
            this.login_attempts = 0;
            this.logged_in = false;
    	
        }
        
        public User(String ip, Socket socket) {
        	this("anon", null, ip, socket);
        }
        
        public User() {
        	this("anon", null);
        }
        
        //methods
        
        //set go equal to bin
        public static void setgo(boolean bin)
        {
        	go = bin;
        }
        
        // validate username
        public boolean username(String username) 
        {
        	User user = users.get(username);
        	if (user != null && !user.logged_in) 
        	{
        		out.println("success");
        		out.flush();
        		name = username;
        		user.blocked_ips.put(
        				user.ip, 0);
        		return true;
        	} else 
        	{
        		out.println("failure");
        		out.flush();
        		return false;
        	}
        }
        
        // validate password
        public boolean password(String username, String password) 
        {
        	Server.User user = Server.users.get(username);
        	long elapsedTime = (new Date()).getTime() - user.timer;
        	if (user.blocked_ips.get(user.ip) > Server.ATTEMPTS - 1)
        	{
        		user.blocked = true;
        	}
        	
        	if (user.blocked && (elapsedTime < BLOCK_TIME*1000)) 
        	{
        		out.println("failure");
        		out.flush();
        		return false;
        	} else
        	{
        		
        	
        		if ((user.pass).equals(password))
        		{
        			out.println("Welcome to simple chat server!");
        			out.flush();
        		
        			User me = Server.users.get(username);
        			me.logged_in = true;
        			me.logtime = Calendar.getInstance();
        			me.socket = socket;
        			me.out = out;
        			me.blocked = false;
        			me.login_attempts = 0;
        		
        			return true;
        		} else
        		{
        			out.println("failure");
        			out.flush();        				
        			
        			user.blocked_ips.put(user.ip, user.blocked_ips.get(user.ip)+1);
        			user.timer = (new Date()).getTime();
        				
        			return false;
        		}
        	}
        }
        
        // interpret commands sent by client
        public boolean command(String com) 
        {
        	if (com == null)
        		com = "nullcommand";
        	Scanner scan = new Scanner(com);
        	String order = scan.next().toLowerCase();
            String tmp;
        	
            if (order.equals("whoelse"))
            {
                whoelse();
            } else if (order.equals("wholast"))
            {
                wholast(com);
            } else if (order.equals("broadcast")) 
            {
            	if ((tmp = scan.next()).equals("message"))
            		broadcastmessage(com);
            	else if (tmp.equals("user"))
            		broadcastuser(com);
            } else if (order.equals("message"))
            {
                message(com);
            } else if (order.equals("logout"))
            {
                logout();
            } else 
            {
                confused();
            }

                scan.close();
                return false;
        }
        
        // wrapper method to print out who else is online
        public void whoelse() {
        	Server.printOnline(users.get(name), out);
        }
        
        // wrapper method to print out who was online in the last
        // specified amount of time
        public void wholast(String order) 
        {
        	Scanner scan = new Scanner(order);
        	scan.next();
        	int min = Integer.parseInt(scan.next());
        	Server.printLast(users.get(name), min, out);
        	scan.close();
        }
        
        // wrapper method to broadcast a message to all online users
        public void broadcastmessage(String order) 
        {
        	String message = order.split(" message ")[1];
        	Server.sendOnline(users.get(name), this.out, name + ": " + message);
        }
        
        // wrapper method to broadcast a message to a list of users
        public void broadcastuser(String order) 
        {
        	String[] all = order.split(" message ");
        	String message = all[1];
        	String users = all[0].split(" user ")[1];
        	Server.sendOnlineUsers(Server.users.get(name), users, this.out, name + ": " + message);
        }
        
        // wrapper method to send private message to a user
        public void message(String order) 
        {
        	String[] all = order.split("message ");
        	String message = all[1];
        	Scanner scanner = new Scanner(message);
        	String user = scanner.next();
        	message = message.split(user + " ")[1];
        	Server.sendOnlineUsers(Server.users.get(name), user, this.out, name + ": " + message);
        }
        
        // logout the client
        public void logout() 
        {
        	Server.users.get(name).logged_in = false;
        }
        
        // runs when comman cannot be interpreted
        public void confused()
        {
        	Server.users.get(name).out.println("invalid command");
        	Server.users.get(name).out.flush();
        	
        }
        
        
        // thread to run parallel to main thread
    	@Override
    	public void run() {
    		System.out.println("Accepted anonymous user");
    		
    		while (true)
    		{
    		
                try
                {
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out = new PrintWriter(socket.getOutputStream());
    			
                    while (true)
                    {
    							
                        boolean go = false;
                        String username = null;
    			
                        // validate username
                        while (!go)
                        {
                            username = in.readLine();
                            go = username(username);
                        }
                        
                        go = false;
    			
                        // validate password
                        while (!go)
                        {
                            String pass = in.readLine();
                            go = password(username, pass);
                        }
    			
                        go = false;
    			
                        // intepret commands
                        while (!go)
                        {
                            String com = "command";
  	        	    
                            socket.setSoTimeout(TIME_OUT*60*1000);
                            try
                            {
                                com = in.readLine();
                            } catch (SocketTimeoutException e)
                            {
                                Server.users.get(name).logged_in = false;
                                out.println("logout");
                                out.flush();
                                break;
                            }

                            if (com == null)
                                com = "nullcommand";
    				
                            command(com);
                            if (com.equals("logout") || !Server.users.get(name).logged_in)
                            {
                                out.println("logout");
                                out.flush();
                                break;
    				}
    					
    			}
    		}
    					
    		} catch (IOException e1) 
    		{
    			Server.users.get(name).logged_in = false;
    			out.println("logout");
				out.flush();
				break;
    		}
    	}
    }
}
    
    // main method
    public static void main(String[] args) throws IOException 
    {
    	int port;
    	
    	if (args[0] == null)
			port = 9999;
    	else	
    		port = Integer.parseInt(args[0]);
    	
        // establish listen socket
    	Server server = new Server(port);	
    	ServerSocket serverSocket = new ServerSocket(port);
    	
        // accept new clients in perpetuity
    	while (true) 
    	{
    		Socket clientSocket = serverSocket.accept();
    		User anonUser = new User(clientSocket.getInetAddress().toString(), clientSocket);
    		Thread thread = new Thread(anonUser);
    		thread.start();   			   			
    	}   		
   	}  
    
}

