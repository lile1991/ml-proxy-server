package io.ml.client.socket;

import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

public class SocketClient {

    public static void main0(String[] args) throws IOException {
        // httpGetRequest();
        httpsGetRequest();

    }

    private static void httpsGetRequest() throws IOException {
        try (Socket socket = SSLSocketFactory.getDefault().createSocket("ckadmin.grights.club", 443)) {
            try (OutputStreamWriter streamWriter = new OutputStreamWriter(socket.getOutputStream());
                 BufferedWriter bufferedWriter = new BufferedWriter(streamWriter)) {

                bufferedWriter.write("GET /hw-facebook-admin/pubIpInfo/ HTTP/1.1\r\n");
                bufferedWriter.write("Host: ckadmin.grights.club\r\n");
                bufferedWriter.write("\r\n");
                bufferedWriter.flush();

                try (BufferedInputStream streamReader = new BufferedInputStream(socket.getInputStream());
                     BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(streamReader, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        System.out.println(line);
                    }
                }
            }
        }
    }

    private static void httpGetRequest() throws IOException {
        SocketAddress dest = new InetSocketAddress("ckadmin.grights.club", 80);
        try (Socket socket = new Socket()) {
            socket.connect(dest);
            try (OutputStreamWriter streamWriter = new OutputStreamWriter(socket.getOutputStream());
                 BufferedWriter bufferedWriter = new BufferedWriter(streamWriter)) {

                bufferedWriter.write("GET /hw-facebook-admin/pubIpInfo/ HTTP/1.1\r\n");
                bufferedWriter.write("Host: ckadmin.grights.club\r\n");
                bufferedWriter.write("\r\n");
                bufferedWriter.flush();

                try (BufferedInputStream streamReader = new BufferedInputStream(socket.getInputStream());
                     BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(streamReader, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        System.out.println(line);
                    }
                }
            }
        }
    }
}
