package edu.vanderbilt.hiplab.metaanalysis;

import Program.EstimateNConfig;
import Program.EstimateNServer;
import Utils.StopWatch;
import jargs.gnu.CmdLineParser;

import java.math.BigInteger;
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

        for (int i = 0; i < 3; i++) {
            System.out.println("#### One more inputs:");
            server.setInputs(inputValue);
            server.runOnline();

            // pull the outputs
            for (int outputIndex = 0; outputIndex < server.results.length; outputIndex++) {
                System.out.println("Outside: " + server.results[outputIndex]);
            }
        }

        server.cleanup();

        System.out.println("From Server!");
    }
}
