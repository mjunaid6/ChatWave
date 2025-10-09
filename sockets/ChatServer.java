// ChatServer.java
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;


public class ChatServer {
    private final int port = 9090;
    private final ServerSocket serverSocket;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final ConcurrentMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<String>> groups = new ConcurrentHashMap<>();


    public ChatServer() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Server listening on port " + port);
    }


    public void start() {
        try {
            while (true) {
                Socket client = serverSocket.accept();
                ClientHandler handler = new ClientHandler(client, this);
                pool.submit(handler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public boolean registerUser(String username, ClientHandler handler) {
        return clients.putIfAbsent(username, handler) == null;
    }


    public void unregisterUser(String username) {
        clients.remove(username);
        groups.forEach((g, set) -> set.remove(username));
    }


    public ClientHandler getClient(String username) {
        return clients.get(username);
    }


    public Set<String> getAllUsers() {
        return clients.keySet();
    }


    public boolean createGroup(String groupName, Set<String> members) {
        if (groups.putIfAbsent(groupName, ConcurrentHashMap.newKeySet()) == null) {
            groups.get(groupName).addAll(members);
            return true;
        }
        return false;
    }


    public Set<String> getGroupMembers(String groupName) {
        return groups.get(groupName);
    }


    public void sendPrivateMessage(String from, String to, String msg) {
        ClientHandler target = clients.get(to);
        if (target != null) target.send("PRIVATE FROM " + from + ": " + msg);
    }


    public void sendGroupMessage(String from, String groupName, String msg) {
        Set<String> members = groups.get(groupName);
        if (members == null) return;
        for (String member : members) {
            if (member.equals(from)) continue;
            ClientHandler h = clients.get(member);
            if (h != null) h.send("GROUP " + groupName + " FROM " + from + ": " + msg);
        }
    }


    public static void main(String[] args) throws IOException {
        ChatServer server = new ChatServer();
        server.start();
    }


    static class ClientHandler implements Runnable {
        private final Socket socket;
        private final ChatServer server;
        private BufferedReader in;
        private BufferedWriter out;
        private String username;


        ClientHandler(Socket socket, ChatServer server) {
            this.socket = socket;
            this.server = server;
        }


        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
                send("WELCOME: Please LOGIN <username>");


                String line;
                while ((line = in.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    String[] parts = line.split(" ", 3);
                    String cmd = parts[0].toUpperCase();


                    switch (cmd) {
                        case "LOGIN": handleLogin(parts); break;
                        case "MSG": handleMsg(parts); break;
                        case "CREATE_GROUP": handleCreateGroup(parts); break;
                        case "GROUP_MSG": handleGroupMsg(parts); break;
                        case "LIST_USERS": handleListUsers(); break;
                        case "LOGOUT": close(); return;
                        default: send("ERROR Unknown command");
                    }
                }
            } catch (IOException ignored) {} finally { close(); }
        }


        private void handleLogin(String[] parts) throws IOException {
            if (parts.length < 2) { send("ERROR usage: LOGIN <username>"); return; }
            if (server.registerUser(parts[1], this)) {
            username = parts[1];
            send("OK LOGIN successful as " + username);
            } else send("ERROR Username already in use");
        }


        private void handleMsg(String[] parts) throws IOException {
            if (parts.length < 3) { send("ERROR usage: MSG <to> <message>"); return; }
            server.sendPrivateMessage(username, parts[1], parts[2]);
            send("SENT to " + parts[1]);
        }


        private void handleCreateGroup(String[] parts) throws IOException {
            if (parts.length < 3) { send("ERROR usage: CREATE_GROUP <group> <member1,member2>"); return; }
            Set<String> members = new HashSet<>(Arrays.asList(parts[2].split(",")));
            members.add(username);
            if (server.createGroup(parts[1], members)) send("OK group created");
            else send("ERROR group exists");
        }


        private void handleGroupMsg(String[] parts) throws IOException {
            if (parts.length < 3) { send("ERROR usage: GROUP_MSG <group> <message>"); return; }
            server.sendGroupMessage(username, parts[1], parts[2]);
            send("SENT GROUP " + parts[1]);
        }


        private void handleListUsers() throws IOException {
            send("USERS " + String.join(",", server.getAllUsers()));
        }


        void send(String msg) {
            try { out.write(msg + "\r\n"); out.flush(); } catch (IOException ignored) {}
        }


        private void close() {
            try {
                if (username != null) server.unregisterUser(username); socket.close(); 
            } catch (IOException ignored) {}
        }
    }
}