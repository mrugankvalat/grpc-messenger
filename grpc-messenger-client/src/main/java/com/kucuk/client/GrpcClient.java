package com.kucuk.client;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;

import javax.net.ssl.SSLException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class GrpcClient {

    public static void main(String[] args) throws SSLException, InterruptedException, ExecutionException {

        int sleepPeriod = 10;
        int threadCount = 100;
        int callCount = 1000;

        if (args.length > 0) {
            sleepPeriod = Integer.parseInt(args[0]);
        }
        if (args.length > 1) {
            threadCount = Integer.parseInt(args[1]);
        }
        if (args.length >2) {
            callCount = Integer.parseInt(args[2]);
        }

        File ca = new File("../cert/ca.crt");
        if (!ca.exists()) {
            System.out.println("Cert chain error!");
            return;
        }

        String author = "ikucuk@gmail.com";
        String title = "Sample Message Title";
        String content = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<MessageServiceCaller.CallResult>> results = new ArrayList<>(threadCount);

        for (int i = 0; i < threadCount; i++) {
            MessageServiceCaller caller = new MessageServiceCaller(callCount, "kucuk.com", 443, 12345L, author, title, content, sleepPeriod, ca);
            Future<MessageServiceCaller.CallResult> callResultFuture = executor.submit(caller);
            results.add(callResultFuture);
        }
        executor.shutdown();
        if (executor.awaitTermination(500, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }

        for (Future<MessageServiceCaller.CallResult> resultFuture : results) {
            MessageServiceCaller.CallResult result = resultFuture.get();
            System.out.println("Success Rate: " + result.getSuccessCount() +
                    " Duration: " + result.getDuration() / callCount);
        }

        Arguments arguments = new Arguments();
        arguments.addArgument("host", "kucuk.com");
        arguments.addArgument("port", "443");
        arguments.addArgument("ca", "../cert/ca.crt");
        arguments.addArgument("requestId", "1000");
        arguments.addArgument("author", "ikucuk@gmail.com");
        arguments.addArgument("title", "Message Title For Medium Message");
        arguments.addArgument("content", "Message Content");
        arguments.addArgument("sleepPeriod", "2000");
        JavaSamplerContext context = new JavaSamplerContext(arguments);
        GrpcMessengerServiceSampler sampler = new GrpcMessengerServiceSampler();
        sampler.setupTest(context);
        sampler.runTest(context);
    }
}
