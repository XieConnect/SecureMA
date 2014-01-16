package edu.vanderbilt.hiplab.metaanalysis;

import Program.ProgClient;
import Program.EstimateNClient;
import Program.EstimateNConfig;
import Program.Program;
import Utils.StopWatch;
import jargs.gnu.CmdLineParser;

import java.math.BigInteger;
import java.util.Random;

/**
 * Refer to README for details.
 * Author: Wei Xie
 * Version:
 */
public class CircuitClient {
    static BigInteger inputValue;
    static int nBits;

    static Random rnd = new Random();

    private static void printUsage() {
        System.out.println("Usage: java TestHammingClient [{-n, --bit-length} length] [{-s, --server} servername] [{-r, --iteration} r]");
    }

    private static void process_cmdline_args(String[] args) {
        CmdLineParser parser = new CmdLineParser();
        CmdLineParser.Option optionServerIPname = parser.addStringOption('s', "server");
        CmdLineParser.Option optionBitLength = parser.addIntegerOption('n', "bit-Length");
        CmdLineParser.Option optionIterCount = parser.addIntegerOption('r', "iteration");

        try {
            parser.parse(args);
        } catch (CmdLineParser.OptionException e) {
            System.err.println(e.getMessage());
            printUsage();
            System.exit(2);
        }

        nBits = ((Integer) parser.getOptionValue(optionBitLength, new Integer(64))).intValue();
        ProgClient.serverIPname = (String) parser.getOptionValue(optionServerIPname, new String("localhost"));
        Program.iterCount = ((Integer) parser.getOptionValue(optionIterCount, new Integer(1))).intValue();
    }

    private static void generateData() throws Exception {
        //inputValue = new BigInteger(4, rnd).add(BigInteger.ONE);
        inputValue = new BigInteger("5");
    }

    public static void main(String[] args) throws Exception {
        StopWatch.pointTimeStamp("Starting program");
        process_cmdline_args(args);

        generateData();

        EstimateNClient client = new EstimateNClient(EstimateNConfig.nBits);

        client.runOffline();

        for (int i = 0; i < 3; i++) {
            System.out.println("#### One more inputs:");
            client.setInputs(inputValue);
            client.runOnline();
        }
        System.out.println("From client!");
    }
}
