package th.in.meen.natmeout.util;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import th.in.meen.natmeout.model.message.HelloMessage;
import th.in.meen.natmeout.model.message.TunnelMessage;
import th.in.meen.natmeout.tunneler.tcp.PublicSideTunnelerImpl;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class PublicSideTcpServerTest {

    private static final Logger log = LoggerFactory.getLogger(PublicSideTcpServerTest.class);

    @Test
    public void dummyClient() throws IOException, InterruptedException {
        Socket clientSocket = new Socket("127.0.0.1", 5000);
        InputStream inputStream = clientSocket.getInputStream();
        OutputStream outputStream = clientSocket.getOutputStream();

        TunnelMessage tunnelMessage = new HelloMessage("ThisIsS3ret");
        byte[] dataToSend = PublicSideTunnelerImpl.generateBytesToSend(tunnelMessage);
        outputStream.write(dataToSend);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    TunnelMessage response = null;
                    try {
                        response = PublicSideTunnelerImpl.readMessage(inputStream);
                        if(response != null)
                            log.debug("Received message type " + response.getCommand() + " - " + response.toString());
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                        break;
                    }
                }
            }
        });
        t.start();

        t.join();
    }
}
