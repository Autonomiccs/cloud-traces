package br.com.autonomiccs.cloudTraces.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.com.autonomiccs.cloudTraces.exceptions.GoogleTracesToCloudTracesException;

public class ProcessLogFileResults {

    private final static String STRING_BEFORE_VALUE_OF_MEMORY_STD_BEFORE_MANAGEMENT = "before management; memory STD [";
    private final static Pattern PATTERN_MEMORY_STD_BEFORE_MANAGEMENT = Pattern
            .compile(Pattern.quote(STRING_BEFORE_VALUE_OF_MEMORY_STD_BEFORE_MANAGEMENT) + "(.*?)" + Pattern.quote("Gib]"));

    private final static String STRING_BEFORE_VALUE_OF_CPU_STD = "cpu STD [";
    private final static Pattern PATTERN_CPU_STD = Pattern.compile(Pattern.quote(STRING_BEFORE_VALUE_OF_CPU_STD) + "(.*?)" + Pattern.quote("Ghz]"));

    private final static String STRING_BEFORE_VALUE_OF_CPU_USAGE_STD = "cpu usage STD [";
    private final static Pattern PATTERN_CPU_USAGE_STD = Pattern.compile(Pattern.quote(STRING_BEFORE_VALUE_OF_CPU_USAGE_STD) + "(.*?)" + Pattern.quote("Ghz]"));

    private final static String STRING_BEFORE_VALUE_OF_MEMORY_STD_AFTER_MANAGEMENT = "after management; memory STD [";
    private final static Pattern PATTERN_MEMORY_STD_AFTER_MANAGEMENT = Pattern
            .compile(Pattern.quote(STRING_BEFORE_VALUE_OF_MEMORY_STD_AFTER_MANAGEMENT) + "(.*?)" + Pattern.quote("Gib]"));

    private final static String STRING_BEFORE_PROCESING_TIME = "total processing time [";
    private final static Pattern PATTERN_PROCESING_TIME = Pattern.compile(Pattern.quote(STRING_BEFORE_PROCESING_TIME) + "(.*?)" + Pattern.quote("]"));

    private final static String STRING_BEFORE_SELECTED_HEURISTIC = "selected map index [";
    private final static Pattern PATTERN_SELECTED_HEURISTIC = Pattern.compile(Pattern.quote(STRING_BEFORE_SELECTED_HEURISTIC) + "(.*?)" + Pattern.quote("]"));

    private final static String STRING_BEFORE_NUMBER_OF_HOSTS = "number of hosts [";
    private final static Pattern PATTERN_NUMBER_OF_HOSTS = Pattern.compile(Pattern.quote(STRING_BEFORE_NUMBER_OF_HOSTS) + "(.*?)" + Pattern.quote("]"));

    private final static String STRING_BEFORE_NUMBER_OF_VMS = "number of VMs [";
    private final static Pattern PATTERN_NUMBER_OF_VMS = Pattern.compile(Pattern.quote(STRING_BEFORE_NUMBER_OF_VMS) + "(.*?)" + Pattern.quote("]"));

    private final static String STRING_BEFORE_NUMBER_OF_MIGRATIONS = "#migrations [";
    private final static Pattern PATTERN_MIGRATION = Pattern.compile(Pattern.quote(STRING_BEFORE_NUMBER_OF_MIGRATIONS) + "(.*?)" + Pattern.quote("]"));

    private final static Pattern PATTERN_MEMORY_STD = Pattern.compile(Pattern.quote("] memory STD [") + "(.*?)" + Pattern.quote("Gib]"));
    private final static Pattern PATTERN_MEMORY_USAGE_STD = Pattern.compile(Pattern.quote("memory usage STD [") + "(.*?)" + Pattern.quote("Gib]"));

    private final static Pattern PATTERN_CLUSTER_ID = Pattern.compile(Pattern.quote("Cluster [cluster-") + "(.*?)" + Pattern.quote("] memory STD "));

    private static double wightedMemoryStdSum = 0;
    private static double wightedMemoryUsageStdSum = 0;
    private static double weightedCpuStd = 0;
    private static double weightedCpuUsageStd = 0;
    private static long cloudMemory = 0;
    private static long cloudCpu = 0;
    private static int numberOfClusters = 0;
    private static List<Double> clustersMemoryStd = new ArrayList<>();
    private static List<Double> clustersMemoryUsageStd = new ArrayList<>();
    private static List<Double> clustersCpuStd = new ArrayList<>();
    private static List<Double> clustersCpuUsageStd = new ArrayList<>();

    private static List<Double> clustersMemory = new ArrayList<>();
    private static List<Double> clustersCpu = new ArrayList<>();

    private static List<Double> clustersMemoryUsage = new ArrayList<>();
    private static List<Double> clustersCpuUsage = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        validateInputFile(args);

        String simulatedResultsLogFile = args[0];

        PrintWriter outputFile = new PrintWriter("simulationResultsToAnalyse.txt");
        BufferedReader bufferedReader = new BufferedReader(new FileReader(simulatedResultsLogFile));

        List<String> linesToWrite = new ArrayList<>();

        String logType;
        try {
            logType = args[1];
        } catch (ArrayIndexOutOfBoundsException e) {
            logType = "";
        }

        allocatedLog(bufferedReader, linesToWrite);
        writeLines(outputFile, linesToWrite);

        bufferedReader.close();
        outputFile.close();
        System.out.println("Finished!");
    }

    private static void allocatedLog(BufferedReader bufferedReader, List<String> linesToWrite) throws IOException {
        String lineToRead;
        String time = "90000.00";
        String clusters = "1";
        String currentTime;
        linesToWrite.add("TIME CPU_STD_MEAN CPU_USAGE_STD_MEAN MEMORY_STD_MEAN MEMORY_USAGE_STD_MEAN");

        while ((lineToRead = bufferedReader.readLine()) != null) {
            Matcher matcherCpuStd = PATTERN_CPU_STD.matcher(lineToRead);
            Matcher matcherCpuUsageStd = PATTERN_CPU_USAGE_STD.matcher(lineToRead);
            Matcher matcherMemoryStd = PATTERN_MEMORY_STD.matcher(lineToRead);
            Matcher matcherMemoryUsageStd = PATTERN_MEMORY_USAGE_STD.matcher(lineToRead);
            Matcher matcherClusterId = PATTERN_CLUSTER_ID.matcher(lineToRead);

            Pattern PATTERN_TIME = Pattern.compile(Pattern.quote("time [") + "(.*?)" + Pattern.quote("]"));
            Matcher matcherTime = PATTERN_TIME.matcher(lineToRead);

            Pattern PATTERN_CLOUD_MEMORY = Pattern.compile(Pattern.quote("Cloud configuration: Cloud id [Google data traces], total memory [") + "(.*?)" + Pattern.quote("MB]"));
            Pattern PATTERN_CLOUD_CPU = Pattern.compile(Pattern.quote("], total cpu [") + "(.*?)" + Pattern.quote("Mhz]"));
            Pattern PATTERN_CLUSTER_CONFIGURATION = Pattern.compile(Pattern.quote("Cluster configuration at time "));
            Pattern CLUSTER_MEMORY = Pattern.compile(Pattern.quote("total memory [") + "(.*?)" + Pattern.quote("MB]"));
            Pattern CLUSTER_MEMORY_USAGE = Pattern.compile(Pattern.quote("memory used [") + "(.*?)" + Pattern.quote("Mb]"));
            Pattern CLUSTER_CPU = Pattern.compile(Pattern.quote("total cpu [") + "(.*?)" + Pattern.quote("Mhz]"));
            Pattern CLUSTER_CPU_USAGE = Pattern.compile(Pattern.quote("cpu used [") + "(.*?)" + Pattern.quote("Mhz]"));

            Matcher matcherCloudMemory = PATTERN_CLOUD_MEMORY.matcher(lineToRead);
            Matcher matcherCloudCpu = PATTERN_CLOUD_CPU.matcher(lineToRead);
            Matcher matcherClusterConfiguration = PATTERN_CLUSTER_CONFIGURATION.matcher(lineToRead);
            Matcher matcherClusterCpu = CLUSTER_CPU.matcher(lineToRead);
            Matcher matcherClusterCpuUsage = CLUSTER_CPU_USAGE.matcher(lineToRead);
            Matcher matcherClusterMemory = CLUSTER_MEMORY.matcher(lineToRead);
            Matcher matcherClusterMemoryUsage = CLUSTER_MEMORY_USAGE.matcher(lineToRead);

            if (matcherCloudMemory.find()) {
                cloudMemory = Long.parseLong(matcherCloudMemory.group(1));
                matcherCloudCpu.find();
                cloudCpu = Long.parseLong(matcherCloudCpu.group(1));
            }

            if (matcherClusterConfiguration.find()) {
                matcherClusterCpu.find();
                matcherClusterCpuUsage.find();
                matcherClusterMemory.find();
                matcherClusterMemoryUsage.find();

                clustersCpu.add(Double.parseDouble(matcherClusterCpu.group(1)));
                clustersMemory.add(Double.parseDouble(matcherClusterMemory.group(1)));

                clustersCpuUsage.add(Double.parseDouble(matcherClusterCpuUsage.group(1)));
                clustersMemoryUsage.add(Double.parseDouble(matcherClusterMemoryUsage.group(1)));
            }

            matcherTime.find();
            if (matcherClusterId.find()) {
                currentTime = matcherTime.group(1);
                currentTime = currentTime.replace(",", ".");
                matcherMemoryStd.find();
                matcherMemoryUsageStd.find();
                matcherCpuStd.find();
                matcherCpuUsageStd.find();
                if (time.equals(currentTime)) {
                    addClusterMemoryAndCpuStd(clusters, matcherCpuStd, matcherCpuUsageStd, matcherMemoryStd, matcherMemoryUsageStd, matcherClusterId);
                } else {
                    for(int i = 0; i < numberOfClusters; i++) {
                        double clustersCpuDividedByCloudCpu = clustersCpu.get(i) / cloudCpu;
                        weightedCpuStd += (clustersCpuStd.get(i) * clustersCpuDividedByCloudCpu);
                        weightedCpuUsageStd += (clustersCpuUsageStd.get(i) * clustersCpuDividedByCloudCpu);
                        wightedMemoryStdSum += (clustersMemoryStd.get(i) * (clustersMemory.get(i) / cloudMemory));
                        wightedMemoryUsageStdSum += (clustersMemoryUsageStd.get(i) * (clustersMemory.get(i) / cloudMemory));
                    }
                    linesToWrite.add(String.format("%s %s %s %s %s", time, weightedCpuStd / numberOfClusters, weightedCpuUsageStd / numberOfClusters,
                            wightedMemoryStdSum / numberOfClusters, wightedMemoryUsageStdSum / numberOfClusters));

                    weightedCpuStd = 0;
                    weightedCpuUsageStd = 0;
                    wightedMemoryStdSum = 0;
                    wightedMemoryUsageStdSum = 0;
                    numberOfClusters = 0;
                    clustersMemoryStd = new ArrayList<>();
                    clustersCpuStd = new ArrayList<>();
                    clustersMemoryUsageStd = new ArrayList<>();
                    clustersCpuUsageStd = new ArrayList<>();
                    clustersMemory = new ArrayList<>();
                    clustersCpu = new ArrayList<>();

                    time = currentTime;
                    addClusterMemoryAndCpuStd(clusters, matcherCpuStd, matcherCpuUsageStd, matcherMemoryStd, matcherMemoryUsageStd, matcherClusterId);
                }
            }
        }
    }

    private static void addClusterMemoryAndCpuStd(String clusters, Matcher matcherCpuStd, Matcher matcherCpuUsageStd, Matcher matcherMemoryStd, Matcher matcherMemoryUsageStd,
            Matcher matcherClusterId) {
        String memoryString = matcherMemoryStd.group(1);
        memoryString = memoryString.replace(",", ".");
        wightedMemoryStdSum += Double.valueOf(memoryString);

        String memoryUsageString = matcherMemoryUsageStd.group(1);
        memoryUsageString = memoryUsageString.replace(",", ".");
        wightedMemoryUsageStdSum += Double.valueOf(memoryUsageString);

        String cpuString = matcherCpuStd.group(1);
        cpuString = cpuString.replace(",", ".");
        weightedCpuStd += Double.valueOf(cpuString);

        String cpuUsageString = matcherCpuUsageStd.group(1);
        cpuUsageString = cpuUsageString.replace(",", ".");
        weightedCpuUsageStd += Double.valueOf(cpuUsageString);

        clustersMemoryStd.add(Double.valueOf(memoryString));
        clustersCpuStd.add(Double.valueOf(cpuString));

        clustersMemoryUsageStd.add(Double.valueOf(memoryUsageString));
        clustersCpuUsageStd.add(Double.valueOf(cpuUsageString));

        clusters = matcherClusterId.group(1);
        numberOfClusters = Integer.valueOf(clusters);
    }

    private static void bestResults(BufferedReader bufferedReader, List<String> linesToWrite) throws IOException {
        String lineToRead;
        Pattern PATTERN_BEST_RESULTS_COUNT = Pattern.compile(Pattern.quote("MinimumResults=[") + "(.*?)" + Pattern.quote("]"));

        linesToWrite.add(
                "COSINE_MEAN COSINE_MEDIAN MEAN_ALPHA1 MEAN_ALPHA2 MEAN_ALPHA10 MEDIAN_ALPHA1 MEDIAN_ALPHA2 MEDIAN_ALPHA10");

        while ((lineToRead = bufferedReader.readLine()) != null) {
            Matcher matcherBestResults = PATTERN_BEST_RESULTS_COUNT.matcher(lineToRead);

            if (matcherBestResults.find()) {
                String bestRestults = matcherBestResults.group(1);
                linesToWrite.add(bestRestults);
            }
        }
    }

    private static void addLineToListOfLinesToWrite(List<String> linesToWrite, String migrations, String cpuBefore, String cpuAfter, String memoryBefore, String memoryAfter,
            String processingTime, String numberOfHosts, String numberOfVms, String selectedHeuristic) {
        if (!migrations.equals("") && !cpuBefore.equals("") && !cpuAfter.equals("") && !memoryBefore.equals("") && !memoryAfter.equals("")) {
            linesToWrite.add(String.format("%s %s %s %s %s %s %s %s %s", migrations, cpuBefore, cpuAfter, memoryBefore, memoryAfter, processingTime, numberOfHosts, numberOfVms,
                    selectedHeuristic));
        } else {
            linesToWrite.add(String.format("Falied to write this line; migrations=[%s], cpuBefore=[%s], cpuAfter=[%s], memoryBefore=[%s], memoryAfter=[%s]", migrations, cpuBefore,
                    cpuAfter, memoryBefore, memoryAfter));
        }
    }

    private static void writeLines(PrintWriter outputFile, List<String> linesToWrite) {
        for (String line : linesToWrite) {
            outputFile.println(line);
        }
    }

    private static void validateInputFile(String[] args) {
        File file = new File(args[0]);
        if (!file.exists()) {
            throw new GoogleTracesToCloudTracesException(String.format("File [%s] does not exist.", args[0]));
        }
        if (!file.canRead()) {
            throw new GoogleTracesToCloudTracesException(String.format("Cannot read file [%s] .", args[0]));
        }
    }

}
