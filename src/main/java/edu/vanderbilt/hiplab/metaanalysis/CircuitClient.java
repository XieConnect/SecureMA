package edu.vanderbilt.hiplab.metaanalysis;

import Program.ProgClient;
import Program.EstimateNClient;
import Program.EstimateNConfig;
import Program.Program;
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
public class CircuitClient {
    static BigInteger inputValue;
    static int nBits;
    static int inputPort;

    static Random rnd = new Random();

    private static void printUsage() {
        System.out.println("Usage: java TestHammingClient [{-n, --bit-length} length] [{-s, --server} servername] [{-r, --iteration} r]");
    }

    private static void process_cmdline_args(String[] args) {
        CmdLineParser parser = new CmdLineParser();
        CmdLineParser.Option optionServerIPname = parser.addStringOption('s', "server");
        CmdLineParser.Option optionBitLength = parser.addIntegerOption('n', "bit-Length");
        CmdLineParser.Option optionIterCount = parser.addIntegerOption('r', "iteration");
        CmdLineParser.Option optionSocketPort = parser.addIntegerOption('p', "port");
        CmdLineParser.Option optionInputPort = parser.addIntegerOption('i', "input-port");

        try {
            parser.parse(args);
        } catch (CmdLineParser.OptionException e) {
            System.err.println(e.getMessage());
            printUsage();
            System.exit(2);
        }

        nBits = ((Integer) parser.getOptionValue(optionBitLength, new Integer(128))).intValue();
        ProgClient.serverIPname = (String) parser.getOptionValue(optionServerIPname, new String("localhost"));
        Program.iterCount = ((Integer) parser.getOptionValue(optionIterCount, new Integer(1))).intValue();
        EstimateNConfig.socketPort = ((Integer) parser.getOptionValue(optionSocketPort, new Integer(23456))).intValue();
        inputPort = ((Integer) parser.getOptionValue(optionInputPort, new Integer(3492))).intValue();
    }

    private static void generateData() throws Exception {
        //inputValue = new BigInteger(4, rnd).add(BigInteger.ONE);
        inputValue = new BigInteger("5");
    }

    public static void main(String[] args) throws Exception {
        Object tempObject = null;

        StopWatch.pointTimeStamp("Starting program");
        process_cmdline_args(args);

        generateData();

        EstimateNClient client = new EstimateNClient(EstimateNConfig.nBits);

        client.runOffline();

        // host socket server to get inputs
        ServerSocket ss = new ServerSocket(inputPort);


        BigInteger inputLine;

        System.out.println("## Now take inputs (before WHILE).");

        while (true) {
            Socket sock = ss.accept();
            ObjectInputStream inStream = new ObjectInputStream(sock.getInputStream());
            ObjectOutputStream outStream = new ObjectOutputStream(sock.getOutputStream());
            System.out.println("## Input socket OK.");

            try {
                inputLine = (BigInteger) inStream.readObject();
            } catch (EOFException e) {
                e.printStackTrace();
                continue;
            }


                System.out.println("#### One more inputs: " + inputLine);
                inputValue = inputLine;

                client.setInputs(inputValue);
                client.runOnline();

                outStream.writeObject(client.randa.negate());
                outStream.writeObject(client.randb.negate());

            sock.close();
        }

    }
}
