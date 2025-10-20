import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private final Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    public Client(String HOST, int PORT) throws IOException{
        this.socket = new Socket(HOST,PORT);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF-8"));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8"));
    }

    public void start() {
        Thread th = new Thread(() -> {
            String line;
            try{ 
                while((line = in.readLine()) != null) {
                    System.out.println("[SERVER] " + line); 
                    System.out.print("> ");
                    System.out.flush();
                }
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    System.out.println("Connection error: " + e.getMessage());
                } else {
                    System.out.println("Disconnected from SERRVER.");
                }
            }
        });
        th.start();
        
        try(Scanner sc = new Scanner(System.in)){
            while(sc.hasNextLine()) {
                String line = sc.nextLine();
                if(line.equalsIgnoreCase("exit")) {
                    out.write("LOGOUT\r\n");
                    out.flush();
                    break;
                }
                out.write(line + "\r\n");
                out.flush();
            }
            th.join();
            socket.close();
        } catch( IOException e) {
            System.out.println("Exception in sending : " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("Thread interrupted while waiting: " + e.getMessage());
        }

    }

    public static void main(String[] args) throws IOException{
        try{
            Scanner sc = new Scanner(System.in);

            System.out.print("Enter HOST addres : ");
            System.out.flush();
            String host = sc.nextLine();
            
            System.out.print("Enter PORT addres : ");
            int port = sc.nextInt();

            Client c2 = new Client(host,port);

            c2.start();
            sc.close();
        } catch(IOException e) {
            System.out.println("Error in establishing connection : " + e);
        }
    }
}