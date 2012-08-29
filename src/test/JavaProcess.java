package test;

/**
 * @author Wei Xie <wei.xie (at) vanderbilt.edu>
 * @version 7/5/12
 * @description Refer to README
 */
import SFE.BOAL.Bob;
import main.Mediator;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

// Execute Java class in separate process
// NOTE: some parameters inside are project-specific
public class JavaProcess {
    public Process exec(Class klass, String[] args) throws IOException,
            InterruptedException {
        Process process = null;
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome +
                File.separator + "bin" +
                File.separator + "java";
        String classpath = System.getProperty("java.class.path");

        ArrayList<String> list = new ArrayList<String>(args.length + 5);
        list.add(javaBin);
        list.add("-Drundir=run/");
        list.add("-cp");
        list.add(classpath);
        list.add(klass.getCanonicalName());
        list.addAll(Arrays.asList(args));

        ProcessBuilder builder = new ProcessBuilder(list);
        builder.redirectErrorStream(true);
        process = builder.start();

        return process;
    }
}
