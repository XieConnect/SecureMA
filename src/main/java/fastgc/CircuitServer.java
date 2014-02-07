package fastgc;

import Program.EstimateNConfig;
import Program.EstimateNServer;
import Utils.StopWatch;
import edu.vanderbilt.hiplab.metaanalysis.Helpers;
import jargs.gnu.CmdLineParser;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

/**
 * Host garbled circuit as backend service (server-side)
 * Author: Wei Xie
 * Version:
 */
public class CircuitServer {
    // input value to the circuit (random)
    static BigInteger inputValue;
    static int inputPort;

    static Random rnd = new Random();

    private static void printUsage() {
        System.out.println("Usage: java TestHammingServer [{-n, --bit-length} length]");
    }

    private static void process_cmdline_args(String[] args) {
        CmdLineParser parser = new CmdLineParser();
        CmdLineParser.Option optionSocketPort = parser.addIntegerOption('p', "port");
        CmdLineParser.Option optionInputPort = parser.addIntegerOption('i', "input-port");

        try {
            parser.parse(args);
        } catch (CmdLineParser.OptionException e) {
            System.err.println(e.getMessage());
            printUsage();
            System.exit(2);
        }

        EstimateNConfig.socketPort = ((Integer) parser.getOptionValue(optionSocketPort, new Integer(23456))).intValue();
        inputPort = ((Integer) parser.getOptionValue(optionInputPort, new Integer(3491))).intValue();
    }

    /**
     * Generate input value
     */
    private static void generateData() throws Exception {
        //inputValue = new BigInteger(nBits - 2, rnd).add(BigInteger.ONE);
        //inputValue = new BigInteger("38834241553");
        inputValue = new BigInteger("1");
    }

    public static void main(String[] args) throws Exception {
        int maxInputBits = Integer.parseInt(Helpers.property("max_n_bits"));
        BigInteger fieldSpace = BigInteger.valueOf(2).pow(maxInputBits);

        StopWatch.pointTimeStamp("Starting program");
        process_cmdline_args(args);

        generateData();

        // args: input value,  max bit size of value,  number of loops
        EstimateNServer server = new EstimateNServer( maxInputBits,
                Helpers.MaxN() );

        server.runOffline();

        // host socket server to get inputs
        ServerSocket ss = new ServerSocket(inputPort);

        System.out.println("## Now take inputs (before WHILE).");
        BigInteger inputLine;

        BigInteger tmp;
        Socket sock = ss.accept();
        System.out.println("## Input socket connected.");
        ObjectInputStream inStream = new ObjectInputStream(sock.getInputStream());
        ObjectOutputStream outStream = new ObjectOutputStream(sock.getOutputStream());

        // stay awake and take inputs from outside all the time
        while (true) {

            try {
                inputLine = (BigInteger) inStream.readObject();
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }

            inputValue = inputLine;

            server.setInputs(inputValue);
            server.runOnline();

            // get outputs
            for (int outputIndex = 0; outputIndex < server.results.length; outputIndex++) {
                tmp = server.results[outputIndex].testBit(EstimateNConfig.MaxInputBits - 1) ? server.results[outputIndex].subtract(fieldSpace)  :  server.results[outputIndex];

                System.out.println("Outside: " + tmp);
                outStream.writeObject(tmp);
            }

        }



    }
}
