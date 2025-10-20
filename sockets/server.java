import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private final int PORT = 9090;
    private final ServerSocket serverSocket;
    private ExecutorService pool = Executors.newCachedThreadPool();
    private ConcurrentHashMap<String,ClientHandler> clients = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Set<String>> groups = new ConcurrentHashMap<>();

    public Server() throws IOException {
        serverSocket = new ServerSocket(PORT);
        System.out.println("SERVER listening on PORT:"+PORT);
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

    public boolean createGroup(String groupName, Set<String> members, ClientHandler admin) {
        Set<String> validMembers = new HashSet<>();
        members.forEach((user) -> {
            if(clients.containsKey(user)) validMembers.add(user);
            else admin.send(user + " doesn't exist");
        });
        if(validMembers.size() < 2) return false;
        validMembers.add(admin.username);
        return groups.putIfAbsent(groupName,new HashSet<>(validMembers)) == null;
    }

    public Set<String> getGroupMembers(String groupName) {
        return groups.get(groupName);
    }

    public Set<String> getAllGroups() {
        return groups.keySet();
    }

    public boolean sendPrivateMessage(String from, String to, String msg) {
        ClientHandler target = clients.get(to);
        if(target != null) {
            target.send("PRIVATE MSG FROM " + from + ", Message : " + msg);
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
                        case "GROUP_MEMBERS" : handleGetGroupMembers(parts); break;
                        case "LIST_GROUPS" : handleListGroups(); break;
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
            if(username == null) {
                send("ERROR: Please LOGIN first using LOGIN <username>");
                return;
            }
            if(server.sendPrivateMessage(username, parts[1], parts[2])) send("Message sent to "+parts[1]);
            else send("User doesn't exit");
        }

        private void handleCreateGroup(String[] parts) {
            if(parts.length < 3) {
                send("ERROR usage : CREATE_GROUP <group_name> <member1,member2,...>");
                return;
            }
            if(username == null) {
                send("ERROR: Please LOGIN first using LOGIN <username>");
                return;
            }
            Set<String> members = new HashSet<>(Arrays.asList(parts[2].split(",")));
            if(server.createGroup(parts[1],members,this)) {
                send(parts[1]+" group created successfull");
                server.sendGroupMessage(username,parts[1],parts[1]+" group created successfull and you have been added.");
            }
            else send("ERROR : this Group cannot be created, due to any of the following reason(s)\n1. Group with this Group Name already exists.\n2. No member to be added exists.\n3. Group size is less than 3.");
        }

        private void handleGroupMsg(String[] parts) {
            if(parts.length < 3) {
                send("ERROR usage : GROUP_MSG <group_name> <message>");
                return;
            }
            if(username == null) {
                send("ERROR: Please LOGIN first using LOGIN <username>");
                return;
            }
            Set<String> members = server.getGroupMembers(parts[1]);
            if(members == null) {
                send("ERROR : "+ parts[1] + "doesn't exits");
                return;
            }
            if(!members.contains(username)) {
                send("ERROR : You're not a member of the group " + parts[1]);
                return;
            }
            if(server.sendGroupMessage(username,parts[1],parts[2])) send("Message sent to group " + parts[1]);
            else send("Error in sending message.");
        }

        private void handleListUsers() {
            if(username == null) {
                send("ERROR: Please LOGIN first using LOGIN <username>");
                return;
            }
            send("Users : " + String.join(",",server.getAllClients()));
        }

        private void handleGetGroupMembers(String[] parts) {
            if(parts.length < 2) {
                send("ERROR usage : GROUP_MEMBERS <group_name>");
                return;
            }
            if(username == null) {
                send("ERROR: Please LOGIN first using LOGIN <username>");
                return;
            }
            Set<String> members = server.getGroupMembers(parts[1]);
            if(members == null) send("Group doesn't exist");
            else send("Group Members of Group " + parts[0] + " : " + String.join(",",members));
        }

        private void handleListGroups() {
            if(username == null) {
                send("ERROR: Please LOGIN first using LOGIN <username>");
                return;
            }
            send("Groups : " + String.join(",",server.getAllGroups()));
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
