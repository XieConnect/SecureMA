/**
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 6/27/12
 * @description Test running Alice and Bob
 */

package test;

import main.Mediator;
import main.Provider;

public class AutomatedTest {
    public static void main(String[] args) {
        String[] runParams = new String[2];
        runParams[0] = "fairplay";

        if (args.length > 0 && args[0].equals("init")) {
            runParams[1] = args[0];
        }

        // Run Alice
        Process providerProcess = null;
        try {
            System.out.println("Running Provider...");

            providerProcess = new JavaProcess().exec(Provider.class, args);
        } catch (Exception e) {
            System.out.println("Error running Provider: " + e.getMessage());
        }

        // Run Bob
        Process mediatorProcess = null;
        try {
            System.out.println("Running Mediator...");

            mediatorProcess = new JavaProcess().exec(Mediator.class, runParams);
        } catch (Exception e) {
            System.out.println("Error running Mediator: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            mediatorProcess.waitFor();
            providerProcess.waitFor();
            System.out.println("> Both processes finished.");
        } catch (Exception e) {
            System.out.println("Error terminating process: " + e.getMessage());
        } finally {
            if (mediatorProcess != null)
                mediatorProcess.destroy();

            if (providerProcess != null)
                providerProcess.destroy();
        }

    }

}