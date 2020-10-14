package org.berk.distributedcounter.testapp;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.berk.distributedcounter.client.DistributedCounterWebfluxClient;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;

/**
 * Parses command line arguments and runs {@link DistributedCounterPerformanceTester}
 */
public class Main {
    static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(3);
    static final int DEFAULT_MAX_SPEED = 10000;

    static final Options CMDLINE_OPTS = new Options();

    static {
        CMDLINE_OPTS.addRequiredOption("c", "counters", true, "Number of counters");
        CMDLINE_OPTS.addRequiredOption("i", "increment", true, "How much to increment each counter");
        CMDLINE_OPTS.addOption("k", "keep", false, "Keep the counters after test");
        CMDLINE_OPTS.addOption("t", "timeout", true, "Time out, defaults to 3 minutes. e.g: 30s");
        CMDLINE_OPTS.addOption("v", "verify", false, "Verify the data after testing");
        CMDLINE_OPTS.addOption("s", "speed", true, "Max req/sec to send, defaults to 10000");
        CMDLINE_OPTS.addOption("w", "skipwarmup", false, "Skip warmup requests before testing");
        CMDLINE_OPTS.addOption("h", "help", false, "Show  help");
    }

    public static void main(String[] args) {
        Main main = new Main();
        try {
            TestArgs testArgs = main.parseCmdlineArgs(args);
            DistributedCounterWebfluxClient counterClient = new DistributedCounterWebfluxClient(testArgs.getServiceHost());
            DistributedCounterPerformanceTester performanceTester = new DistributedCounterPerformanceTester(counterClient);

            if (testArgs.isWarmupEnabled()) {
                performanceTester.warmup();
            }

            PerformanceStats performanceStats = performanceTester.applyLoad(testArgs.getCounters(),
                                                                            testArgs.getIncrementBy(),
                                                                            testArgs.getSpeedLimit(),
                                                                            testArgs.getTimeOut(),
                                                                            testArgs.isVerifyCountsAfterTest(),
                                                                            testArgs.isKeepCountersAfterTest());

            System.out.println(performanceStats);

        } catch (ParseException e) {
            System.err.println(e.getMessage());
            showHelpAndExit(1);
        }
    }



    private TestArgs parseCmdlineArgs(String[] args) throws ParseException {
        TestArgs.TestArgsBuilder testArgsBuilder = TestArgs.builder();
        if (args.length == 0) {
            throw new MissingArgumentException("Missing required argument: <service address>");
        }
        try{
            new URL(args[0]);
        } catch (MalformedURLException e) {
            throw new ParseException("Invalid service address: " + e.getMessage());
        }
        testArgsBuilder.serviceHost(args[0]);
        CommandLine cmd = new DefaultParser().parse(CMDLINE_OPTS, args);
        if(cmd.hasOption("h")) {
            showHelpAndExit(0);
        }
        testArgsBuilder.counters(Integer.parseInt(cmd.getOptionValue("c")));
        testArgsBuilder.incrementBy(Integer.parseInt(cmd.getOptionValue("i")));
        testArgsBuilder.keepCountersAfterTest(cmd.hasOption("k"));
        testArgsBuilder.verifyCountsAfterTest(cmd.hasOption("v"));
        testArgsBuilder.warmupEnabled(!cmd.hasOption("w"));
        testArgsBuilder.timeOut(Optional.ofNullable(cmd.getOptionValue("t"))
                .map(drStr -> drStr.toUpperCase().startsWith("PT") ? drStr : "PT" + drStr)
                .map(Duration::parse)
                .orElse(DEFAULT_TIMEOUT));
        testArgsBuilder.speedLimit(Optional.ofNullable(cmd.getOptionValue("s"))
                .map(Integer::valueOf)
                .orElse(DEFAULT_MAX_SPEED));
        return testArgsBuilder.build();
    }

    private static void showHelpAndExit(int exitStatus) {
        new HelpFormatter().printHelp(100, "testapp <service address> -c 8 -i 10000 -d 5000ns", "", CMDLINE_OPTS, "");
        System.exit(exitStatus);
    }

}
