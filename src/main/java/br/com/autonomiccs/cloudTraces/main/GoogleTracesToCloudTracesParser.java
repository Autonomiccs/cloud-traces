/*
 * Cloud traces
 * Copyright (C) 2016 Autonomiccs, Inc.
 *
 * Licensed to the Autonomiccs, Inc. under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The Autonomiccs, Inc. licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package br.com.autonomiccs.cloudTraces.main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;

import br.com.autonomiccs.cloudTraces.beans.GoogleJob;
import br.com.autonomiccs.cloudTraces.beans.GoogleTask;
import br.com.autonomiccs.cloudTraces.beans.GoogleTrace;
import br.com.autonomiccs.cloudTraces.beans.VirtualMachine;
import br.com.autonomiccs.cloudTraces.beans.VmServiceOffering;
import br.com.autonomiccs.cloudTraces.exceptions.GoogleTracesToCloudTracesException;
import br.com.autonomiccs.cloudTraces.service.VmServiceOfferingService;

public class GoogleTracesToCloudTracesParser {

    private final static Logger logger = Logger.getLogger(GoogleTracesToCloudTracesParser.class);

    private static String cloudTracesFileName = "cloudVmTraces.csv";

    /**
     * This parameter indicates how much a normalized core usage with value '0' represents in MHz
     */
    private static int minimumCoreUsageInMhz = 100;

    /**
     * This parameter indicates how much a normalized core usage with value '1' represents in MHz
     */
    private static int maximumCoreUsageInMhz = 3400;

    /**
     * This parameter indicates how much a normalized memory usage with value '0' represents in MB
     */
    private static int minimumMemoryUsageInMb = 25;

    /**
     * This parameter indicates how much a normalized memory usage with value '1' represents in MB
     */
    private static int maximumMemoryUsageInMb = 2048;

    public static void main(String[] args) {
        validateArguments(args);
        List<GoogleTrace> googleTraces = readAllGoogleTracesFromDataset(args[0]);

        logger.info(String.format("#Google traces loaded [%d]", googleTraces.size()));
        Collection<GoogleJob> googleJobs = buildTasksHierachyAndCreateJobList(googleTraces);
        buildJobsTaksByTimeMap(googleJobs);
        fillOutStartAndEndTimeOfJobs(googleJobs);
        calculateThePeakJobResourceUsage(googleJobs);

        GoogleJob biggestCpuUsageJob = googleJobs.iterator().next();
        GoogleJob biggestMemoryUsageJob = biggestCpuUsageJob;

        GoogleJob lowestCpuUsageJob = googleJobs.iterator().next();
        GoogleJob lowestMemoryUsageJob = lowestCpuUsageJob;

        for (GoogleJob googleJob : googleJobs) {
            if (biggestCpuUsageJob.getMaximumCpuUsageAtTime() < googleJob.getMaximumCpuUsageAtTime()) {
                biggestCpuUsageJob = googleJob;
            }
            if (biggestMemoryUsageJob.getMaximumMemoryUsageAtTime() < googleJob.getMaximumMemoryUsageAtTime()) {
                biggestMemoryUsageJob = googleJob;
            }

            if (lowestCpuUsageJob.getMaximumCpuUsageAtTime() > googleJob.getMaximumCpuUsageAtTime()) {
                lowestCpuUsageJob = googleJob;
            }
            if (lowestMemoryUsageJob.getMaximumMemoryUsageAtTime() > googleJob.getMaximumMemoryUsageAtTime()) {
                lowestMemoryUsageJob = googleJob;
            }
        }
        logger.info("Max job cpu usage: " + biggestCpuUsageJob);
        logger.info("Max job memory usage: " + biggestMemoryUsageJob);
        logger.info("Min job cpu usage: " + lowestCpuUsageJob);
        logger.info("Min job memory usage: " + lowestMemoryUsageJob);

        List<VirtualMachine> virtualMachines = createVmsToExecuteJobs(googleJobs);
        writeVmTracesToFile(virtualMachines);
    }

    private static void writeVmTracesToFile(List<VirtualMachine> virtualMachines) {
        List<Integer> allTimeThatHaveTasks = getAllTimesThatWeExecuteTask(virtualMachines);

        Collections.sort(virtualMachines, new Comparator<VirtualMachine>() {
            @Override
            public int compare(VirtualMachine o1, VirtualMachine o2) {
                return o1.getDeployTime() - o2.getDeployTime();
            }
        });
        try (BufferedWriter bfw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(cloudTracesFileName)))) {
            bfw.write(
                    "#Time, vmName, deployTime, remove time, service offering name, service offering number of cores, total allocated cpu, allocated memory, used cpu, used memory");
            bfw.newLine();
            int lines = 0;
            for (Integer time : allTimeThatHaveTasks) {
                for (VirtualMachine virtualMachine : virtualMachines) {
                    if (virtualMachine.getDeployTime() > time || virtualMachine.getDestroyTime() < time) {
                        continue;
                    }
                    VmServiceOffering vmServiceOffering = virtualMachine.getVmServiceOffering();
                    List<GoogleTask> vmTasksForTime = virtualMachine.getGoogleJob().getMapTimeByTasks().get(time);
                    if (CollectionUtils.isEmpty(vmTasksForTime)) {
                        continue;
                    }
                    double vmUsedMemoryForTime = 0;
                    double vmUsedCpuForTime = 0;
                    for (GoogleTask googleTask : vmTasksForTime) {
                        vmUsedCpuForTime += googleTask.getCpuUsage();
                        vmUsedMemoryForTime += googleTask.getMemoryUsage();
                    }
                    String vmTraceForTime = String.format("%d, %s, %d, %d, %s, %d, %d, %d, %.10f, %.10f", time, virtualMachine.getVmId(), virtualMachine.getDeployTime(),
                            virtualMachine.getDestroyTime(), vmServiceOffering.getName(), vmServiceOffering.getNumberOfCores(),
                            vmServiceOffering.getCoreSpeed() * vmServiceOffering.getNumberOfCores(), vmServiceOffering.getMemoryInMegaByte(), vmUsedCpuForTime,
                            vmUsedMemoryForTime);
                    bfw.write(vmTraceForTime);
                    bfw.newLine();
                    lines++;
                }
            }
            logger.info(String.format("#lines [%d] written to the cloud data traces.", lines));
        } catch (IOException e) {
            throw new GoogleTracesToCloudTracesException(e);
        }

    }

    private static List<Integer> getAllTimesThatWeExecuteTask(List<VirtualMachine> virtualMachines) {
        Set<Integer> times = new HashSet<>();
        for (VirtualMachine virtualMachine : virtualMachines) {
            times.addAll(virtualMachine.getGoogleJob().getMapTimeByTasks().keySet());
        }
        ArrayList<Integer> listOfTimes = new ArrayList<>(times);
        Collections.sort(listOfTimes);
        return listOfTimes;
    }

    private static List<VirtualMachine> createVmsToExecuteJobs(Collection<GoogleJob> googleJobs) {
        List<VirtualMachine> virtualMachines = new ArrayList<>(googleJobs.size());
        int count = 1;
        for (GoogleJob googleJob : googleJobs) {
            VirtualMachine vm = createVirtualMachineForJob(googleJob);
            vm.setVmId("VM-" + (count++));
            virtualMachines.add(vm);
        }
        return virtualMachines;
    }

    private static VirtualMachine createVirtualMachineForJob(GoogleJob googleJob) {
        VirtualMachine virtualMachine = new VirtualMachine();
        virtualMachine.setDeployTime(googleJob.getStartTime());
        virtualMachine.setDestroyTime(googleJob.getEndTime());
        virtualMachine.setGoogleJob(googleJob);
        virtualMachine.setVmServiceOffering(getServiceOfferingForVmToSupportJobsPeakLoad(googleJob));
        return virtualMachine;
    }

    private static VmServiceOffering getServiceOfferingForVmToSupportJobsPeakLoad(GoogleJob googleJob) {
        for (VmServiceOffering vmServiceOffering : VmServiceOfferingService.getVmServiceOfferings()) {
            if (vmServiceOffering.getCoreSpeed() * vmServiceOffering.getNumberOfCores() < googleJob.getMaximumCpuUsageAtTime()) {
                continue;
            }
            if (vmServiceOffering.getMemoryInMegaByte() < googleJob.getMaximumMemoryUsageAtTime()) {
                continue;
            }
            return vmServiceOffering;
        }
        throw new GoogleTracesToCloudTracesException("Could not find a suitable service offering for a VM that is supposed to host the Job: " + googleJob);
    }

    private static void calculateThePeakJobResourceUsage(Collection<GoogleJob> googleJobs) {
        for (GoogleJob googleJob : googleJobs) {
            List<Integer> times = new ArrayList<>(googleJob.getMapTimeByTasks().keySet());
            Collections.sort(times);
            for (Integer time : times) {
                double cpuUsageAtTime = 0;
                double memoryUsageAtTime = 0;
                for (GoogleTask googleTask : googleJob.getMapTimeByTasks().get(time)) {
                    cpuUsageAtTime += googleTask.getCpuUsage();
                    memoryUsageAtTime += googleTask.getMemoryUsage();
                }
                if (cpuUsageAtTime > googleJob.getMaximumCpuUsageAtTime()) {
                    googleJob.setMaximumCpuUsageAtTime(cpuUsageAtTime);
                    googleJob.setTimeWithPeakCpuUsage(time);
                }
                if (memoryUsageAtTime > googleJob.getMaximumMemoryUsageAtTime()) {
                    googleJob.setMaximumMemoryUsageAtTime(memoryUsageAtTime);
                    googleJob.setTimeWithPeakMemoryUsage(time);
                }
            }
        }

    }

    private static void fillOutStartAndEndTimeOfJobs(Collection<GoogleJob> googleJobs) {
        for (GoogleJob googleJob : googleJobs) {
            List<Integer> times = new ArrayList<>(googleJob.getMapTimeByTasks().keySet());
            Collections.sort(times);
            googleJob.setStartTime(times.get(0));
            googleJob.setEndTime(times.get(times.size() - 1));
        }

    }

    private static void buildJobsTaksByTimeMap(Collection<GoogleJob> googleJobs) {
        for (GoogleJob googleJob : googleJobs) {
            for (GoogleTask googleTask : googleJob.getTasks()) {
                List<GoogleTask> googleTasksAtTime = googleJob.getMapTimeByTasks().get(googleTask.getTime());
                if (googleTasksAtTime == null) {
                    googleTasksAtTime = new ArrayList<>();
                    googleJob.getMapTimeByTasks().put(googleTask.getTime(), googleTasksAtTime);
                }
                googleTasksAtTime.add(googleTask);
            }
        }
    }

    private static Collection<GoogleJob> buildTasksHierachyAndCreateJobList(List<GoogleTrace> googleTraces) {
        Map<Integer, Integer> mapJobIdByJobType = new HashMap<>();
        Map<Integer, GoogleTask> mapTaskIdGoogleTaskObject = new HashMap<>();
        for (GoogleTrace g : googleTraces) {
            mapJobIdByJobType.put(g.getJobId(), g.getJobType());
            int taskId = g.getTaskId();
            GoogleTask googleTaskParent = mapTaskIdGoogleTaskObject.get(taskId);
            if (googleTaskParent == null) {
                googleTaskParent = createGoogleTaskFromGoogleTrace(g);
                mapTaskIdGoogleTaskObject.put(taskId, googleTaskParent);
            } else {
                GoogleTask googleTask = createGoogleTaskFromGoogleTrace(g);
                googleTaskParent.addExecutionOfTaskThroughTime(googleTask);
            }
        }
        logger.info(String.format("#Tasks after we created the task hierarchy [%d]", mapTaskIdGoogleTaskObject.size()));
        logger.info(String.format("#Jobs ids found [%d]", mapJobIdByJobType.size()));
        Map<Integer, GoogleJob> mapJobIdByGoogleJob = new HashMap<>();
        for (GoogleTask googleTask : mapTaskIdGoogleTaskObject.values()) {
            int jobId = googleTask.getJobId();
            GoogleJob googleJob = mapJobIdByGoogleJob.get(jobId);
            if (googleJob == null) {
                googleJob = new GoogleJob(jobId);
                googleJob.setJobType(mapJobIdByJobType.get(jobId));
                mapJobIdByGoogleJob.put(jobId, googleJob);
            }
            googleJob.getTasks().addAll(googleTask.getExecutionThroughTime());
        }
        logger.info(String.format("#Jobs with tasks [%d]", mapJobIdByGoogleJob.values().size()));
        return mapJobIdByGoogleJob.values();
    }

    private static GoogleTask createGoogleTaskFromGoogleTrace(GoogleTrace g) {
        GoogleTask googleTask = new GoogleTask(g.getTaskId(), g.getTime(), g.getJobId());

        double normalizedTaskCores = g.getNormalizedTaskCores();
        double normalizedTaskMemory = g.getNormalizedTaskMemory();

        googleTask.setNormalizedTaskCores(normalizedTaskCores);
        googleTask.setNormalizedTaskMemory(normalizedTaskMemory);

        googleTask.setCpuUsage(deNormalizeData(normalizedTaskCores, maximumCoreUsageInMhz, minimumCoreUsageInMhz));
        googleTask.setMemoryUsage(deNormalizeData(normalizedTaskMemory, maximumMemoryUsageInMb, minimumMemoryUsageInMb));
        return googleTask;
    }

    private static double deNormalizeData(double normalizedValue, double max, double min) {
        return normalizedValue * (max - min) + min;
    }

    private static Set<Integer> times = new HashSet<>();

    private static List<GoogleTrace> readAllGoogleTracesFromDataset(String googleTracesDataSet) {
        List<GoogleTrace> googleTraces = new ArrayList<>();
        try (BufferedReader bf = new BufferedReader(new FileReader(googleTracesDataSet))) {
            //ignore the header, the metadata line.
            String line = bf.readLine();
            do {
                line = bf.readLine();
                if (line == null) {
                    continue;
                }
                GoogleTrace googleTrace = createGoogleTrace(line);
                googleTraces.add(googleTrace);
            } while (line != null);
        } catch (IOException e) {
            throw new GoogleTracesToCloudTracesException(e);
        }
        logger.info(String.format("#Times that have some task execution [%d]", times.size()));
        return googleTraces;
    }

    private static Pattern patternMatchGoogleTracesGroups = Pattern.compile("(\\d+)\\s(\\d+)\\s(\\d+)\\s(\\d)\\s(.+)");

    private static GoogleTrace createGoogleTrace(String line) {
        Matcher matcher = patternMatchGoogleTracesGroups.matcher(line);
        if (!matcher.matches()) {
            throw new GoogleTracesToCloudTracesException(String.format("The trace [%s] does not meet the expected pattern.", line));
        }
        GoogleTrace googleTrace = new GoogleTrace();
        times.add(NumberUtils.toInt(matcher.group(1)));
        googleTrace.setTime(NumberUtils.toInt(matcher.group(1)));
        googleTrace.setJobId(NumberUtils.toInt(matcher.group(2)));
        googleTrace.setTaskId(NumberUtils.toInt(matcher.group(3)));
        googleTrace.setJobType(NumberUtils.toInt(matcher.group(4)));

        String[] normalizedCpuAndMemory = matcher.group(5).split(" ");
        googleTrace.setNormalizedTaskCores(NumberUtils.toDouble(normalizedCpuAndMemory[0]));
        googleTrace.setNormalizedTaskMemory(NumberUtils.toDouble(normalizedCpuAndMemory[1]));
        return googleTrace;
    }

    private static void validateArguments(String[] args) {
        if (args.length != 1) {
            throw new GoogleTracesToCloudTracesException("You should inform the full qualified path to the Google traces data set.");
        }
        File file = new File(args[0]);
        if (!file.exists()) {
            throw new GoogleTracesToCloudTracesException(String.format("File [%s] does not exist.", args[0]));
        }
        if (!file.canRead()) {
            throw new GoogleTracesToCloudTracesException(String.format("Cannot read file [%s] .", args[0]));
        }
    }
}
