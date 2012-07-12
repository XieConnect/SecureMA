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
        // Run Bob
        Process mediatorProcess = null;
        try {
            System.out.println("Running Mediator...");

            mediatorProcess = JavaProcess.exec(Mediator.class, args);
        } catch (Exception e) {
            System.out.println("Error running Mediator: " + e.getMessage());
        }

        // Run Alice
        Process providerProcess = null;
        try {
            System.out.println("Running Provider...");

            providerProcess = JavaProcess.exec(Provider.class, args);
        } catch (Exception e) {
            System.out.println("Error running Provider: " + e.getMessage());
        }

        try {
            mediatorProcess.waitFor();
            providerProcess.waitFor();
        } catch (Exception e) {
            System.out.println("Error terminating process: " + e.getMessage());
        }
    }

}