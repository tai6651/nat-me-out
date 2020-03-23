package th.in.meen.natmeout.model;

import th.in.meen.natmeout.model.message.DataMessage;

import java.net.Socket;
import java.util.concurrent.BlockingQueue;

/***
 * Written by Suttichort Sarathum
 * Email: tai5854@hotmail.com
 * Website: https://www.meen.in.th/
 */
public class PublicSideConnection {
    private BlockingQueue<DataMessage> rxQueue;
    private Thread txThread;
    private Thread rxThread;
    private Socket connectionSocket;

    public BlockingQueue<DataMessage> getRxQueue() {
        return rxQueue;
    }

    public void setRxQueue(BlockingQueue<DataMessage> rxQueue) {
        this.rxQueue = rxQueue;
    }

    public Thread getTxThread() {
        return txThread;
    }

    public void setTxThread(Thread txThread) {
        this.txThread = txThread;
    }

    public Thread getRxThread() {
        return rxThread;
    }

    public void setRxThread(Thread rxThread) {
        this.rxThread = rxThread;
    }

    public Socket getConnectionSocket() {
        return connectionSocket;
    }

    public void setConnectionSocket(Socket connectionSocket) {
        this.connectionSocket = connectionSocket;
    }
}
