/**
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 6/27/12
 * @description Test running Alice and Bob
 */

package test;

import main.Manager;
import main.Mediator;

public class AutomatedTest {
    public static void main(String[] args) {
        Process providerProcess = null;
        Process mediatorProcess = null;

        // Run Alice
        try {
            System.out.println("  Running Manager...");
            providerProcess = JavaProcess.exec(Manager.class, args);
        } catch (Exception e) {
            System.out.println("Error running Manager: " + e.getMessage());
        }

        // Run Bob
        try {
            System.out.println("  Running Mediator...");
            mediatorProcess = JavaProcess.exec(Mediator.class, args);
        } catch (Exception e) {
            System.out.println("Error running Mediator: " + e.getMessage());
        }

        // Wait for process to terminate
        try {
            if (mediatorProcess != null)
                mediatorProcess.waitFor();

            if (providerProcess != null)
                providerProcess.waitFor();

            System.out.println("  Both processes finished.");
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