package test;

/**
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 7/5/12
 * @description Refer to README
 */
import SFE.BOAL.Bob;
import main.Mediator;

import java.io.*;

// Execute Java class in separate process
// NOTE: some parameters inside are project-specific
public class JavaProcess {
    private JavaProcess() {}

    public static Process exec(Class klass, String[] args) throws IOException,
            InterruptedException {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome +
                File.separator + "bin" +
                File.separator + "java";
        String classpath = System.getProperty("java.class.path");

        ProcessBuilder builder = new ProcessBuilder(
                javaBin, "-cp", classpath, "-Drundir=run/", klass.getCanonicalName());

        return builder.start();
    }
}
