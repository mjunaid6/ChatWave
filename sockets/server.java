import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private final int port = 9090;
    private final ServerSocket serverSocket;
    private ExecutorService pool = Executors.newCachedThreadPool();
    private ConcurrentHashMap<String,ClientHandler> clients = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Set<String>> groups = new ConcurrentHashMap<>();

    public Server() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("SERVER listening on PORT:"+port);
    }

    public void start() {
        try{
            while (true) {
                Socket client = serverSocket.accept();
                ClientHandler handler = new ClientHandler(client,this);
                pool.submit(handler);
            }
        } catch (IOException e) {
            System.out.println("Error in starting server : "+ e.getMessage());
        }
    }

    public boolean registerUser(String username, ClientHandler handler) {
        return clients.putIfAbsent(username, handler) == null;
    }

    public void unRegisterUser(String username) {
        clients.remove(username);
        groups.forEach((user,set) -> set.remove(username));
        
    }

    public ClientHandler getClient(String username) {
        return clients.get(username);
    }

    public Set<String> getAllClients() {
        return clients.keySet();
    }

    public boolean createGroup(String groupName, Set<String> members) {
        if(!groups.containsKey(groupName)) {
            groups.put(groupName,new HashSet<>(members));
            return true;
        }
        return false;
    }

    public Set<String> getGroupMembers(String groupName) {
        return groups.get(groupName);
    }

    public boolean sendPrivateMessage(String from, String to, String msg) {
        ClientHandler target = clients.get(to);
        if(target != null) {
            target.send("PRIVATE FROM " + from + ": " + msg);
            return true;
        }
        return false;
    }

    public boolean sendGroupMessage(String from, String to, String msg) {
        Set<String> members = groups.get(to);
        if(members == null) return false;

        String msgToSend = "GROUP MESSAGE FROM " + from + ", Message : " + msg;
        for(String user : members){
            if(user.equals(from)) continue;
            ClientHandler handler = clients.get(user);
            if(handler != null) handler.send(msgToSend);
        }
        return true;
    }

    public static void main(String[] args) throws IOException{
        Server s = new Server();
        s.start();
    }

    static class ClientHandler implements Runnable{
        private final Socket socket;
        private final Server server;
        private BufferedReader in;
        private BufferedWriter out;
        private String username;
        
        public ClientHandler(Socket socket, Server server) {
            this.socket = socket;
            this.server = server;
        }

        public void send(String msg) {
            try {
                out.write(msg + "\r\n");
                out.flush();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF-8"));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8"));
                send("WELCOME : Please LOGIN <username>");

                String line;
                while((line = in.readLine()) != null) {
                    line = line.trim();
                    if(line.isEmpty()) continue;
                    String[] parts = line.split(" ",3);
                    String cmd = parts[0].toUpperCase();

                    switch (cmd) {
                        case "LOGIN" : handleLogin(parts); break;
                        case "MSG" : handleMsg(parts); break;
                        case "CREATE_GROUP": handleCreateGroup(parts); break;
                        case "GROUP_MSG": handleGroupMsg(parts); break;
                        case "LIST_USERS": handleListUsers(); break;
                        case "LOGOUT": close(); return;
                        default: send("ERROR Unknown command");
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            } finally {
                close();
            }
        }
        
        private void handleLogin(String[] parts) {
            if(parts.length < 2) {
                send("ERROR : wrong usage : LOGIN <username>");
                return;
            }
            if(server.registerUser(parts[1],this)) {
                username = parts[1];
                send("LOGIN successfull " + username);
            }
            else {
                send("ERROR : Username already registered");
            }
        }

        private void handleMsg(String[] parts) {
            if(parts.length < 3) {
                send("ERROR : MSG <to> <message>");
                return;
            }
            server.sendPrivateMessage(username, parts[1], parts[2]);
            send("Message sent to "+parts[1]);
        }

        private void handleCreateGroup(String[] parts) {
            if(parts.length > 3) {
                send("ERROR usage : CREATE_GROUP <group_name> <member1,member2,...>");
                return;
            }
            Set<String> members = new HashSet<>(Arrays.asList(parts[2].split(",")));
            members.add(username);
            if(server.createGroup(parts[1],members)) {
                send(parts[1]+" group created successfull");
                server.sendGroupMessage(username,parts[1],parts[1]+" group created successfull and you have been added.");
            }
            else send("ERROR : Group already exists");
        }

        private void handleGroupMsg(String[] parts) {
            if(parts.length < 2) {
                send("ERROR usage : GROUP_MSG <group_name> <message>");
                return;
            }
            if(server.sendGroupMessage(username,parts[1],parts[2])) send("Message sent to group " + parts[1]);
            else send("Error in sending message.");
        }

        private void handleListUsers() {
            send("Users : " + String.join(",",server.getAllClients()));
        }

        private void close() {
            try{
                if(username != null) server.unRegisterUser(username);
                if(!socket.isClosed()) socket.close();
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }
}
