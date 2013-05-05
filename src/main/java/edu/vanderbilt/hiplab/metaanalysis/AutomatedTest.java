/**
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 6/27/12
 * @description Test running Alice and Bob
 */

package edu.vanderbilt.hiplab.metaanalysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class AutomatedTest {
    public static String main(String[] args) {
        // final result
        String result = "0";
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

        InputStreamReader isr = new InputStreamReader(mediatorProcess.getInputStream());
        BufferedReader input = new BufferedReader(isr);
        try {
          // to retrieve last line of output (return result)
          String line;
          while ( (line = input.readLine()) != null) {
            result = line;
          }
          result = result.trim();
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
          try {
            if (input != null) input.close();
            if (isr != null) isr.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
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

      return result;
    }

}