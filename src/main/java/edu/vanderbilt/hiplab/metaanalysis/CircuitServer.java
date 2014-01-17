package edu.vanderbilt.hiplab.metaanalysis;

import Program.EstimateNConfig;
import Program.EstimateNServer;
import Utils.StopWatch;
import jargs.gnu.CmdLineParser;

import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

/**
 * Refer to README for details.
 * Author: Wei Xie
 * Version:
 */
public class CircuitServer {
    // input value to the circuit (random)
    static BigInteger inputValue;
    static int nBits;

    static Random rnd = new Random();

    private static void printUsage() {
        System.out.println("Usage: java TestHammingServer [{-n, --bit-length} length]");
    }

    private static void process_cmdline_args(String[] args) {
        CmdLineParser parser = new CmdLineParser();
        CmdLineParser.Option optionBitLength = parser.addIntegerOption('n', "bit-length");

        try {
            parser.parse(args);
        } catch (CmdLineParser.OptionException e) {
            System.err.println(e.getMessage());
            printUsage();
            System.exit(2);
        }

        nBits = ((Integer) parser.getOptionValue(optionBitLength, new Integer(64))).intValue();
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

        StopWatch.pointTimeStamp("Starting program");
        process_cmdline_args(args);

        generateData();

        // args: input value,  max bit size of value,  number of loops
        EstimateNServer server = new EstimateNServer(EstimateNConfig.nBits, EstimateNConfig.maxN);

        server.runOffline();

        // host socket server to get inputs
        ServerSocket ss = new ServerSocket(3491);
        Socket sock = ss.accept();
        System.out.println("## Input socket connected.");


        System.out.println("## Now take inputs (before WHILE).");
        BigInteger inputLine;
        ObjectInputStream inStream = new ObjectInputStream(sock.getInputStream());
        ObjectOutputStream outStream = new ObjectOutputStream(sock.getOutputStream());

        try {
            while (true) {
                inputLine = (BigInteger) inStream.readObject();

                System.out.println("## Read inputs:");
                System.out.println("Got: " + inputLine);
                inputValue = inputLine;

                server.setInputs(inputValue);
                server.runOnline();

                // get outputs
                for (int outputIndex = 0; outputIndex < server.results.length; outputIndex++) {
                    System.out.println("Outside: " + server.results[outputIndex]);
                    outStream.writeObject(server.results[outputIndex]);
                }
            }
        } catch (EOFException e) {
            System.out.println("No more inputs. Will exit.");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
