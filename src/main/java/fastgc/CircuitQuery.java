package fastgc;

import java.io.DataOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.Socket;

/**
 * Refer to README for details.
 * Author: Wei Xie
 * Version:
 */
public class CircuitQuery {
    Socket sock;
    ObjectOutputStream outStream;
    ObjectInputStream inStream;
    BigInteger[] results = new BigInteger[2];

    public CircuitQuery(int port) throws Exception {
        sock = new Socket("localhost", port);
        outStream = new ObjectOutputStream(sock.getOutputStream());
        inStream = new ObjectInputStream(sock.getInputStream());
    }

    public BigInteger[] query(BigInteger inputValue) throws Exception {
        outStream.writeObject(inputValue);

        results[0] = (BigInteger) inStream.readObject();
        results[1] = (BigInteger) inStream.readObject();

        return results;
    }

    public static void main(String[] args) {
        System.out.println("haha");
    }
}
