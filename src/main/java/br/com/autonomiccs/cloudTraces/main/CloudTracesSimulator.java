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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.log4j.Logger;

import br.com.autonomiccs.cloudTraces.algorithms.deployment.DeploymentHeuristic;
import br.com.autonomiccs.cloudTraces.algorithms.deployment.SmallestClustersFirstDeploymentHeuristic;
import br.com.autonomiccs.cloudTraces.algorithms.management.ClusterAdministrationAlgorithm;
import br.com.autonomiccs.cloudTraces.algorithms.management.ClusterAdministrationAlgorithmEmptyImpl;
import br.com.autonomiccs.cloudTraces.beans.Cloud;
import br.com.autonomiccs.cloudTraces.beans.Cluster;
import br.com.autonomiccs.cloudTraces.beans.GoogleJob;
import br.com.autonomiccs.cloudTraces.beans.GoogleTask;
import br.com.autonomiccs.cloudTraces.beans.Host;
import br.com.autonomiccs.cloudTraces.beans.VirtualMachine;
import br.com.autonomiccs.cloudTraces.beans.VmServiceOffering;
import br.com.autonomiccs.cloudTraces.exceptions.GoogleTracesToCloudTracesException;
import br.com.autonomiccs.cloudTraces.service.VmServiceOfferingService;

public class CloudTracesSimulator {

    private final static Logger logger = Logger.getLogger(CloudTracesSimulator.class);

    /**
     * Pattern that matches the data in the file data set.
     */
    private static Pattern patternMatchVmsData = Pattern
            .compile("(\\d+),\\s(VM-\\d+),\\s(\\d+),\\s(\\d+),\\s(\\w+),\\s(\\d+),\\s(\\d+),\\s(\\d+),\\s(\\d+(\\.\\d+)?),\\s(\\d+(\\.\\d+)?)");

    /**
     * As described in 'https://github.com/google/cluster-data' the monitored interval for the traces version 1 (used during the development of this code) is 7 hours.
     */
    private static int monitoredIntervalInMinutes = 7 * 60;

    private static int megaByteToGigaByte = 1024;

    /**
     * The amount of time we go through every iteration of the loop.
     */
    private static int timeFramePerSimulationIterationInMinutes = 5;

    public static void main(String[] args) {
        validateInputFile(args);

        String cloudTracesFile = args[0];
        Collection<VirtualMachine> virtualMachines = getAllVirtualMachinesFromCloudTraces(cloudTracesFile);
        logger.info(String.format("#VirtualMachines [%d] found on [%s].", virtualMachines.size(), cloudTracesFile));

        Map<Integer, List<VirtualMachine>> mapVirtualMachinesTaskExecutionByTime = createMapVirtualMachinesTaskExecutionByTime(virtualMachines);
        logger.info(String.format("#Times [%d] that have tasks being executed by VMs ", mapVirtualMachinesTaskExecutionByTime.size()));

        Cloud cloud = createCloudEnvirtonmentToStartsimulation();
        logger.info("Cloud configuration: " + cloud);

        List<Integer> timesToExecuteTasks = new ArrayList<>(mapVirtualMachinesTaskExecutionByTime.keySet());
        Collections.sort(timesToExecuteTasks);

        Integer firstTimeInTimeUnitOfUsedCloudData = timesToExecuteTasks.get(0);
        Integer lastTimeInTimeUnitOfUserCloudData = timesToExecuteTasks.get(timesToExecuteTasks.size() - 1);

        logger.info("First time: " + firstTimeInTimeUnitOfUsedCloudData);
        logger.info("Last time: " + lastTimeInTimeUnitOfUserCloudData);

        double timeUnitPerLoopIteration = getTimeUnitPerLoopIteration(firstTimeInTimeUnitOfUsedCloudData, lastTimeInTimeUnitOfUserCloudData);
        logger.info("The time unit converted to trace time: " + timeUnitPerLoopIteration);

        double currentTime = firstTimeInTimeUnitOfUsedCloudData;

        long highetResourceAllocation = Long.MIN_VALUE;
        String cloudStateHighestMemoryAllocation = "";

        while (currentTime < lastTimeInTimeUnitOfUserCloudData + 2 * timeUnitPerLoopIteration) {
            logger.debug("Current time of iteration: " + currentTime);
            if (cloud.getMemoryAllocatedInBytes() > highetResourceAllocation) {
                highetResourceAllocation = cloud.getMemoryAllocatedInBytes();
                cloudStateHighestMemoryAllocation = cloud.toString();
            }
            applyLoadOnCloudForCurrentTime(mapVirtualMachinesTaskExecutionByTime, cloud, currentTime);
            destroyVirtualMachinesIfNeeded(cloud, currentTime);

            logger.info(String.format("Time [%.3f], cloud state [%s] ", currentTime, cloud));

            executeManagement(cloud, currentTime);
            logClustersConfigurationsAndStdAtTime(cloud.getClusters(), currentTime);

            currentTime += timeUnitPerLoopIteration;
        }
        logger.info("Cloud configuration after simulation: " + cloud);
        logger.info("Cloud highestResourceUsage: " + cloudStateHighestMemoryAllocation);
    }

    private static void logClustersConfigurationsAndStdAtTime(List<Cluster> clusters, double currentTime) {
        for (Cluster c : clusters) {
            logClusterConfigurationAtTime(c, currentTime);
            logClusterStdAtTime(currentTime, c, StringUtils.EMPTY);
        }
    }

    private static void logClusterConfigurationAtTime(Cluster c, double currentTime) {
        logger.info(String.format("Cluster configuration at time [%.2f]: %s", currentTime, c));
    }

    private static void applyLoadOnCloudForCurrentTime(Map<Integer, List<VirtualMachine>> mapVirtualMachinesTaskExecutionByTime, Cloud cloud, double currentTime) {
        List<Integer> timesUntilCurrenttime = getTimesUntilCurrentTime(mapVirtualMachinesTaskExecutionByTime, currentTime);
        List<VirtualMachine> virtualMachinesWithTaskExecutionAtTime = getVirtualMachinesWithTaskExecutionAtTime(mapVirtualMachinesTaskExecutionByTime, timesUntilCurrenttime);

        logger.info(String.format("Number of Virtual machines [%d] with execution at time [%.2f].", virtualMachinesWithTaskExecutionAtTime.size(), currentTime));
        for (VirtualMachine virtualMachine : virtualMachinesWithTaskExecutionAtTime) {
            if (virtualMachine.getHost() == null) {
                logger.debug(String.format("Deploy of virtual machine [%s] at time [%.2f] ", virtualMachine, currentTime));
                deployVirtualMachine(virtualMachine, cloud);
                displayCloudStateBeforeAndAfterVmDeployment(cloud, virtualMachine);
            }
        }
        updateCloudResourceCount(cloud, currentTime);
    }

    private static void updateCloudResourceCount(Cloud cloud, double currentTime) {
        updateCloudResourceUsageForTime(cloud, currentTime);
        updateCloudResourceAllocated(cloud);
    }

    private static void executeManagement(Cloud cloud, double currentTime) {
        logger.debug("Executing management at time:" + currentTime);
        ClusterAdministrationAlgorithm clusterAdministrationAlgorithm = getClusterAdministrationAlgorithms();
        for (Cluster c : cloud.getClusters()) {

            long timeBeforeManagementProcess = System.nanoTime();
            List<Host> sortedHosts = clusterAdministrationAlgorithm.rankHosts(c.getHosts());
            Map<VirtualMachine, Host> mapVMsToHost = clusterAdministrationAlgorithm.mapVMsToHost(sortedHosts);
            long timeAfterManagementProcess = System.nanoTime();
            logger.info(String.format("#migrations [%d] mapped for cluster [%s] at time [%.2f]; total processing time [%d] (nanoSeconds)", mapVMsToHost.size(), c.getId(),
                    currentTime, timeAfterManagementProcess - timeBeforeManagementProcess));

            if (!mapVMsToHost.isEmpty()) {
                logClusterStdAtTime(currentTime, c, true);
            }
            for (VirtualMachine vm : mapVMsToHost.keySet()) {
                Host targetHost = mapVMsToHost.get(vm);
                migrateVmToHost(vm, targetHost);
            }
            updateClusterResourceAllocated(c);
            updateClusterResourceUsageForTime(c, currentTime);

            if (!mapVMsToHost.isEmpty()) {
                logClusterStdAtTime(currentTime, c, false);
            }
        }
    }

    private static void logClusterStdAtTime(double currentTime, Cluster c, boolean beforeExecutingMigrations) {
        logClusterStdAtTime(currentTime, c, (beforeExecutingMigrations ? "before" : "after") + " management; ");
    }

    private static void logClusterStdAtTime(double currentTime, Cluster c, String epochOfLog) {
        double clusterMemoryAllocatedInMibStd = calculateClusterMemoryAllocatedInMibStd(c);
        double clusterMemoryUsageInMibStd = calculateClusterMemoryUsageInMibStd(c);
        double clusterCpuAllocatedInGhStd = calculateClusterCpuAllocatedInGhStd(c);
        double clusterCpuUsageInGhStd = calculateClusterCpuUsageInGhzStd(c);
        logger.info(String.format("Cluster [%s] %smemory STD [%.2fGib], memory usage STD [%.2fGib], cpu STD [%.2fGhz], cpu usage STD [%.2fGhz] at time [%.2f]", c.getId(),
                epochOfLog, clusterMemoryAllocatedInMibStd / megaByteToGigaByte, clusterMemoryUsageInMibStd / megaByteToGigaByte, clusterCpuAllocatedInGhStd,
                clusterCpuUsageInGhStd, currentTime));
    }

    private static StandardDeviation std = new StandardDeviation(false);

    private static double calculateClusterMemoryAllocatedInMibStd(Cluster cluster) {
        List<Host> hosts = cluster.getHosts();
        double hostsMemoryUsage[] = new double[hosts.size()];
        for (int i = 0; i < hosts.size(); i++) {
            hostsMemoryUsage[i] = hosts.get(i).getMemoryAllocatedInMib();
        }
        return std.evaluate(hostsMemoryUsage);
    }

    private static double calculateClusterMemoryUsageInMibStd(Cluster cluster) {
        List<Host> hosts = cluster.getHosts();
        double hostsMemoryUsage[] = new double[hosts.size()];
        for (int i = 0; i < hosts.size(); i++) {
            hostsMemoryUsage[i] = hosts.get(i).getMemoryUsedInMib();
        }
        return std.evaluate(hostsMemoryUsage);
    }

    private static double calculateClusterCpuAllocatedInGhStd(Cluster cluster) {
        List<Host> hosts = cluster.getHosts();
        double hostsCpuAllocated[] = new double[hosts.size()];
        for (int i = 0; i < hosts.size(); i++) {
            hostsCpuAllocated[i] = (hosts.get(i).getCpuAllocatedInMhz() / 1000d);
        }
        return std.evaluate(hostsCpuAllocated);
    }

    private static double calculateClusterCpuUsageInGhzStd(Cluster cluster) {
        List<Host> hosts = cluster.getHosts();
        double hostsCpuUsageInGhz[] = new double[hosts.size()];
        for (int i = 0; i < hosts.size(); i++) {
            hostsCpuUsageInGhz[i] = (hosts.get(i).getCpuUsedInMhz() / 1000d);
        }
        return std.evaluate(hostsCpuUsageInGhz);
    }

    private static void migrateVmToHost(VirtualMachine vm, Host targetHost) {
        Host sourceHost = vm.getHost();

        logger.debug(String.format("Migrating vm[%s] from host[%s] to host [%s]", vm.getVmId(), sourceHost.getId(), targetHost.getId()));

        sourceHost.destroyVirtualMachine(vm);
        targetHost.addVirtualMachine(vm);
    }

    private static ClusterAdministrationAlgorithmEmptyImpl getClusterAdministrationAlgorithms() {
        return new ClusterAdministrationAlgorithmEmptyImpl();
    }

    private static void updateCloudResourceUsageForTime(Cloud cloud, double currentTime) {
        long memoryUsed = 0;
        long cpuUsed = 0;
        for (Cluster c : cloud.getClusters()) {
            updateClusterResourceUsageForTime(c, currentTime);
            memoryUsed += c.getMemoryUsedInBytes();
            cpuUsed += c.getCpuUsedInMhz();
        }
        cloud.setMemoryUsedInBytes(memoryUsed);
        cloud.setCpuUsedInMhz(cpuUsed);
    }

    private static void updateClusterResourceUsageForTime(Cluster c, double currentTime) {
        long memoryUsed = 0;
        long cpuUsed = 0;
        for (Host h : c.getHosts()) {
            updateHostResourceUsageForTime(h, currentTime);
            memoryUsed += h.getMemoryUsedInBytes();
            cpuUsed += h.getCpuUsedInMhz();
        }
        c.setMemoryUsedInBytes(memoryUsed);
        c.setCpuUsedInMhz(cpuUsed);
    }

    private static final LinearInterpolator linearInterpolator = new LinearInterpolator();

    private static void updateHostResourceUsageForTime(Host h, double currentTime) {
        long memoryUsed = 0;
        long cpuUsed = 0;
        for (VirtualMachine vm : h.getVirtualMachines()) {
            GoogleTask googleTask = getTaskExecutionForTimeEqualCurrentTime(vm, currentTime);
            if (googleTask != null) {
                memoryUsed += googleTask.getMemoryUsage();
                cpuUsed += googleTask.getCpuUsage();
                continue;
            }
            GoogleTask googleTaskBeforeCurrentTime = getTaskExecutionForTimeRightBeforeCurrentTime(vm, currentTime);
            GoogleTask googleTaskAfterCurrentTime = getTaskExecutionForTimeRightAfterCurrentTime(vm, currentTime);

            if (googleTaskBeforeCurrentTime == googleTaskAfterCurrentTime) {
                memoryUsed += googleTaskAfterCurrentTime.getMemoryUsage();
                cpuUsed += googleTaskAfterCurrentTime.getCpuUsage();
                continue;
            }

            memoryUsed += calculateUsedMemoryInterpolatedValue(googleTaskBeforeCurrentTime, currentTime, googleTaskAfterCurrentTime);
            cpuUsed += calculateUsedCpuInterpolatedValue(googleTaskBeforeCurrentTime, currentTime, googleTaskAfterCurrentTime);
        }
        h.setMemoryUsedInMiB(memoryUsed);
        h.setCpuUsedInMhz(cpuUsed);
    }

    private static double calculateUsedCpuInterpolatedValue(GoogleTask googleTaskBeforeCurrentTime, double currentTime, GoogleTask googleTaskAfterCurrentTime) {
        double x[] = new double[2];
        x[0] = googleTaskBeforeCurrentTime.getTime();
        x[1] = googleTaskAfterCurrentTime.getTime();

        double y[] = new double[2];
        y[0] = googleTaskBeforeCurrentTime.getCpuUsage();
        y[1] = googleTaskAfterCurrentTime.getCpuUsage();
        return linearInterpolator.interpolate(x, y).value(currentTime);
    }

    private static double calculateUsedMemoryInterpolatedValue(GoogleTask googleTaskBeforeCurrentTime, double currentTime, GoogleTask googleTaskAfterCurrentTime) {
        double x[] = new double[2];
        x[0] = googleTaskBeforeCurrentTime.getTime();
        x[1] = googleTaskAfterCurrentTime.getTime();

        double y[] = new double[2];
        y[0] = googleTaskBeforeCurrentTime.getMemoryUsage();
        y[1] = googleTaskAfterCurrentTime.getMemoryUsage();
        return linearInterpolator.interpolate(x, y).value(currentTime);
    }

    private static GoogleTask getTaskExecutionForTimeRightAfterCurrentTime(VirtualMachine vm, double currentTime) {
        Map<Integer, List<GoogleTask>> mapTimeByTasks = vm.getGoogleJob().getMapTimeByTasks();

        List<Integer> times = getSortedTimes(mapTimeByTasks);

        int i = times.size() - 1;
        int lastTimeRightAfterCurrentTime = times.get(i);
        while (i >= 0 && times.get(i) >= currentTime) {
            lastTimeRightAfterCurrentTime = times.get(i);
            i--;
        }
        return getTaskForSelectedTime(mapTimeByTasks, lastTimeRightAfterCurrentTime);
    }

    private static GoogleTask getTaskForSelectedTime(Map<Integer, List<GoogleTask>> mapTimeByTasks, int time) {
        List<GoogleTask> googleTasks = mapTimeByTasks.get(time);
        if (CollectionUtils.isEmpty(googleTasks)) {
            throw new GoogleTracesToCloudTracesException("This cannot happen!");
        }
        return googleTasks.get(0);
    }

    private static List<Integer> getSortedTimes(Map<Integer, List<GoogleTask>> mapTimeByTasks) {
        List<Integer> times = new ArrayList<>(mapTimeByTasks.keySet());
        Collections.sort(times);
        return times;
    }

    private static GoogleTask getTaskExecutionForTimeRightBeforeCurrentTime(VirtualMachine vm, double currentTime) {
        Map<Integer, List<GoogleTask>> mapTimeByTasks = vm.getGoogleJob().getMapTimeByTasks();

        List<Integer> times = getSortedTimes(mapTimeByTasks);

        int lastTimeBeforeCurrentTime = 0;
        int i = 0;
        while (i < times.size() && times.get(i) <= currentTime) {
            lastTimeBeforeCurrentTime = times.get(i);
            i++;
        }
        return getTaskForSelectedTime(mapTimeByTasks, lastTimeBeforeCurrentTime);
    }

    private static GoogleTask getTaskExecutionForTimeEqualCurrentTime(VirtualMachine vm, double currentTime) {
        Map<Integer, List<GoogleTask>> mapTimeByTasks = vm.getGoogleJob().getMapTimeByTasks();
        for (int time : mapTimeByTasks.keySet()) {
            if (mapTimeByTasks.get(time).size() != 1) {
                throw new GoogleTracesToCloudTracesException(
                        "This clause can never happen. At this point all of the tasks should have been grouped as a single one in a time slice.");
            }
            if (time == currentTime) {
                return mapTimeByTasks.get(time).get(0);
            }
        }
        return null;
    }

    private static void destroyVirtualMachinesIfNeeded(Cloud cloud, double currentTime) {
        int virtualMachinesDestroyed = 0;
        logger.debug(String.format("Cloud resources before destroy of VMs at time [%.2f]: %s", currentTime, cloud));

        for (VirtualMachine virtualMachine : new HashSet<>(cloud.getVirtualMachines())) {
            if (virtualMachine.getDestroyTime() < currentTime && virtualMachine.getHost() != null) {
                virtualMachinesDestroyed++;
                logger.debug("Destroying VM: " + virtualMachine + "at time: " + currentTime);
                destroyVirtualMachine(virtualMachine, cloud);
            }
        }
        updateCloudResourceCount(cloud, currentTime);

        logger.debug(String.format("Cloud resources after destroy of VMs at time [%.2f]: %s", currentTime, cloud));
        logger.info(String.format("Number of virtual machines [%d] destroyed at time [%.2f]", virtualMachinesDestroyed, currentTime));
    }

    private static void displayCloudStateBeforeAndAfterVmDeployment(Cloud cloud, VirtualMachine virtualMachine) {
        logger.debug(String.format("Cloud resources before deploy of VM [%s]: %s", virtualMachine.getVmId(), cloud));
        updateCloudResourceAllocated(cloud);
        logger.debug(String.format("Cloud resources after deploy of VM [%s]: %s", virtualMachine.getVmId(), cloud));
    }

    private static void updateCloudResourceAllocated(Cloud cloud) {
        long allocatedCpu = 0;
        long allocatedMemory = 0;
        for (Cluster c : cloud.getClusters()) {
            updateClusterResourceAllocated(c);
            allocatedCpu += c.getCpuAllocatedInMhz();
            allocatedMemory += c.getMemoryAllocatedInBytes();
        }
        cloud.setCpuAllocatedInMhz(allocatedCpu);
        cloud.setMemoryAllocatedInBytes(allocatedMemory);
    }

    private static void updateClusterResourceAllocated(Cluster c) {
        long allocatedCpu = 0;
        long allocatedMemory = 0;
        for (Host h : c.getHosts()) {
            allocatedCpu += h.getCpuAllocatedInMhz();
            allocatedMemory += h.getMemoryAllocatedInBytes();
        }
        c.setCpuAllocatedInMhz(allocatedCpu);
        c.setMemoryAllocatedInBytes(allocatedMemory);
    }

    private static void destroyVirtualMachine(VirtualMachine virtualMachine, Cloud cloud) {
        cloud.destroyVirtualMachine(virtualMachine);

        Host host = virtualMachine.getHost();
        host.destroyVirtualMachine(virtualMachine);
    }

    /**
     * This method will try to deploy the virtual machine in the given cloud.
     * We use a heuristic to support the deployment process.
     * That means, the heuristic will decide in which cluster and hosts we try first to deploy the VM.
     * If the deployment is not possible, an exception will be thrown.
     */
    private static void deployVirtualMachine(VirtualMachine virtualMachine, Cloud cloud) {
        DeploymentHeuristic deploymentHeuristic = getDeploymentHeuristic();

        List<Cluster> rankedClustersToDeployVirtualMachine = deploymentHeuristic.getRankedClustersToDeployVirtualMachine(cloud.getClusters(), virtualMachine);
        for (Cluster c : rankedClustersToDeployVirtualMachine) {
            List<Host> clusterOriginalHostsList = c.getHosts();
            List<Host> rankedHostsToDeployVirtualMachie = deploymentHeuristic.getRankedHostsToDeployVirtualMachie(clusterOriginalHostsList, virtualMachine);
            for (Host host : rankedHostsToDeployVirtualMachie) {
                if (canHostSupportVirtualMachine(host, virtualMachine)) {
                    cloud.addVirtualMachine(virtualMachine);

                    int indexOfTargetHost = clusterOriginalHostsList.indexOf(host);
                    Host targetHost = clusterOriginalHostsList.get(indexOfTargetHost);

                    logger.debug("Host before deploy of VM: " + targetHost);
                    logger.debug(String.format("VM [%s] deployed at host [%s]", virtualMachine.getVmId(), host.getId()));

                    targetHost.addVirtualMachine(virtualMachine);
                    logger.debug("Host after deploy of VM: " + targetHost);
                    return;
                }
            }
        }
        throw new GoogleTracesToCloudTracesException("Could not find a suitable host to deploy VM: " + virtualMachine + "\nCloud state: " + cloud);
    }

    private static boolean canHostSupportVirtualMachine(Host host, VirtualMachine virtualMachine) {
        VmServiceOffering vmServiceOffering = virtualMachine.getVmServiceOffering();

        long hostAvailableCpu = host.getTotalCpuPowerInMhz() - host.getCpuAllocatedInMhz();
        int vmRequestedCpu = vmServiceOffering.getCoreSpeed() * vmServiceOffering.getNumberOfCores();

        if (vmRequestedCpu > hostAvailableCpu) {
            return false;
        }
        long hostTotalAvailableMemory = host.getTotalMemoryInMib() - host.getMemoryAllocatedInMib();
        return vmServiceOffering.getMemoryInMegaByte() <= hostTotalAvailableMemory;
    }

    private static DeploymentHeuristic getDeploymentHeuristic() {
        return new SmallestClustersFirstDeploymentHeuristic();
    }

    /**
     *  It returns all of the time units that exist until the 'currentTime' variable.
     *  It will consider inclusive the value of current time.
     */
    private static List<Integer> getTimesUntilCurrentTime(Map<Integer, List<VirtualMachine>> mapVirtualMachinesTaskExecutionByTime, double currentTime) {
        List<Integer> timesUntilCurrenttime = new ArrayList<>();
        for (Integer time : mapVirtualMachinesTaskExecutionByTime.keySet()) {
            if (time <= currentTime) {
                timesUntilCurrenttime.add(time);
            }
        }
        return timesUntilCurrenttime;
    }

    /**
     * For every time that is sent by parameter, we return the list of {@link VirtualMachine} from the map of virtual machines by time.
     * It removes the entry mapped that is returned.
     */
    private static List<VirtualMachine> getVirtualMachinesWithTaskExecutionAtTime(Map<Integer, List<VirtualMachine>> mapVirtualMachinesTaskExecutionByTime, List<Integer> times) {
        List<VirtualMachine> virtualMachines = new ArrayList<>();
        for (Integer time : times) {
            List<VirtualMachine> virtualMachine = mapVirtualMachinesTaskExecutionByTime.remove(time);
            virtualMachines.addAll(virtualMachine);
        }
        return virtualMachines;
    }

    private static double getTimeUnitPerLoopIteration(Integer firstTimeInTimeUnitOfUsedCloudData, Integer lastTimeInTimeUnitOfUserCloudData) {
        int totalTimeUnits = lastTimeInTimeUnitOfUserCloudData - firstTimeInTimeUnitOfUsedCloudData;
        logger.info("Time elapsed every iteration: " + timeFramePerSimulationIterationInMinutes + " minutes");
        return (timeFramePerSimulationIterationInMinutes * totalTimeUnits) / (monitoredIntervalInMinutes * 1d);
    }

    private static Cloud createCloudEnvirtonmentToStartsimulation() {
        Cloud cloud = new Cloud("Google data traces");
        cloud.getClusters().addAll(createClustersMediumSizeHosts(10));
        cloud.getClusters().addAll(createClustersLargeSizeHosts(3));
        cloud.getClusters().addAll(createClustersWithEnourmousHosts(3));

        long totalMemory = 0;
        long totalCpu = 0;
        for (Cluster c : cloud.getClusters()) {
            totalCpu += c.getTotalCpuPowerInMhz();
            totalMemory += c.getTotalMemoryInBytes();
        }
        cloud.setTotalCpuPowerInMhz(totalCpu);
        cloud.setTotalMemoryInBytes(totalMemory);

        return cloud;
    }

    private static List<Cluster> createClustersMediumSizeHosts(int numberOfClusters) {
        List<Cluster> clusters = new ArrayList<>();
        for (int i = 0; i < numberOfClusters; i++) {
            clusters.add(createClusterMediumSizeHosts());
        }
        return clusters;
    }

    private static List<Cluster> createClustersWithEnourmousHosts(int numberOfClusters) {
        List<Cluster> clusters = new ArrayList<>();
        for (int i = 0; i < numberOfClusters; i++) {
            clusters.add(createClusterWithEnourmousHosts());
        }
        return clusters;
    }

    private static List<Cluster> createClustersLargeSizeHosts(int numberOfClusters) {
        List<Cluster> clusters = new ArrayList<>();
        for (int i = 0; i < numberOfClusters; i++) {
            clusters.add(createClusterLargeSizeHosts());
        }
        return clusters;
    }

    private static Cluster createClusterMediumSizeHosts() {
        return createClusterHostsHomogeneousConfigs(8, 60 * VmServiceOfferingService.oneGigaByteInMegaByte, 16, 3400l);
    }

    private static Cluster createClusterLargeSizeHosts() {
        return createClusterHostsHomogeneousConfigs(8, 100 * VmServiceOfferingService.oneGigaByteInMegaByte, 32, 3400l);
    }

    private static Cluster createClusterWithEnourmousHosts() {
        return createClusterHostsHomogeneousConfigs(10, 400 * VmServiceOfferingService.oneGigaByteInMegaByte, 200, 3400l);
    }

    private static int CLUSTER_ID = 1;

    private static Cluster createClusterHostsHomogeneousConfigs(int numberOfHosts, long amoutOfMemoryInMb, int numberOfCores, long coreSpeedInMhz) {
        String clusterId = "cluster-" + CLUSTER_ID++;
        Cluster cluster = new Cluster(clusterId);
        for (int i = 0; i < numberOfHosts; i++) {
            cluster.getHosts().add(createHostWithConfig(amoutOfMemoryInMb, "host-" + (i + 1), numberOfCores, coreSpeedInMhz));
            for (Host h : cluster.getHosts()) {
                h.setClusterId(clusterId);
            }
        }
        long totalMemory = 0;
        long totalCpu = 0;
        for (Host h : cluster.getHosts()) {
            totalCpu += h.getTotalCpuPowerInMhz();
            totalMemory += h.getTotalMemoryInBytes();
        }
        cluster.setTotalCpuPowerInMhz(totalCpu);
        cluster.setTotalMemoryInBytes(totalMemory);
        return cluster;
    }

    private static Host createHostWithConfig(long amoutOfMemoryInMegaByte, String hostId, int numberOfCores, long coreSpeedInMhz) {
        Host host = new Host(hostId);
        host.setTotalCpuPowerInMhz(numberOfCores * coreSpeedInMhz);
        host.setTotalMemoryInBytes(amoutOfMemoryInMegaByte * 1024);
        return host;
    }

    private static Map<Integer, List<VirtualMachine>> createMapVirtualMachinesTaskExecutionByTime(Collection<VirtualMachine> virtualMachines) {
        Map<Integer, List<VirtualMachine>> mapVirtualMachinesTaskExecutionByTime = new HashMap<>();
        for (VirtualMachine virtualMachine : virtualMachines) {
            Map<Integer, List<GoogleTask>> mapTimeByTasks = virtualMachine.getGoogleJob().getMapTimeByTasks();
            for (Integer time : mapTimeByTasks.keySet()) {
                List<VirtualMachine> vmsWithTasksExecutionAtTime = mapVirtualMachinesTaskExecutionByTime.get(time);
                if (vmsWithTasksExecutionAtTime == null) {
                    vmsWithTasksExecutionAtTime = new ArrayList<>();
                    mapVirtualMachinesTaskExecutionByTime.put(time, vmsWithTasksExecutionAtTime);
                }
                vmsWithTasksExecutionAtTime.add(virtualMachine);
            }
        }
        return mapVirtualMachinesTaskExecutionByTime;
    }

    private static Collection<VirtualMachine> getAllVirtualMachinesFromCloudTraces(String cloudTraceFullQualifiedFilePath) {
        Map<String, VirtualMachine> poolOfVirtualMachines = new HashMap<>();
        try (BufferedReader bf = new BufferedReader(new FileReader(cloudTraceFullQualifiedFilePath))) {
            String line = bf.readLine();
            while (line != null) {
                if (StringUtils.trim(line).isEmpty() || StringUtils.startsWith(line, "#")) {
                    line = bf.readLine();
                    continue;
                }
                Matcher matcher = patternMatchVmsData.matcher(line);
                if (!matcher.matches()) {
                    throw new GoogleTracesToCloudTracesException(String.format("String [%s] does not meet the expected pattern.", line));
                }
                int time = Integer.parseInt(matcher.group(1));
                String vmId = matcher.group(2);
                VirtualMachine virtualMachine = poolOfVirtualMachines.get(vmId);
                if (virtualMachine == null) {
                    virtualMachine = createVirtualMachine(matcher);
                    poolOfVirtualMachines.put(vmId, virtualMachine);
                }
                loadTaskForTime(matcher, time, virtualMachine);
                line = bf.readLine();
            }
        } catch (IOException e) {
            throw new GoogleTracesToCloudTracesException(e);
        }
        return poolOfVirtualMachines.values();
    }

    private static void loadTaskForTime(Matcher matcher, int time, VirtualMachine virtualMachine) {
        GoogleJob googleJob = virtualMachine.getGoogleJob();
        int jobId = googleJob.getJobId();
        GoogleTask googleTask = createTask(matcher, time, jobId);

        List<GoogleTask> listTasksByTime = googleJob.getMapTimeByTasks().get(time);
        if (CollectionUtils.isEmpty(listTasksByTime)) {
            listTasksByTime = new ArrayList<>();
            googleJob.getMapTimeByTasks().put(time, listTasksByTime);
        } else {
            throw new GoogleTracesToCloudTracesException("this should not happen");
        }
        listTasksByTime.add(googleTask);
        googleJob.getTaks().add(googleTask);
    }

    private static GoogleTask createTask(Matcher matcher, int time, int jobId) {
        GoogleTask googleTask = new GoogleTask(jobId + time, time, jobId);
        googleTask.setCpuUsage(Double.parseDouble(matcher.group(9)));
        googleTask.setMemoryUsage(Double.parseDouble(matcher.group(11)));
        return googleTask;
    }

    private static VirtualMachine createVirtualMachine(Matcher matcher) {
        VirtualMachine virtualMachine = new VirtualMachine();
        virtualMachine.setVmId(matcher.group(2));
        virtualMachine.setDeployTime(Integer.parseInt(matcher.group(3)));
        virtualMachine.setDestroyTime(Integer.parseInt(matcher.group(4)));
        //Here the jobId do not matter anymore.
        int jobId = virtualMachine.getVmId().hashCode();
        GoogleJob googleJob = new GoogleJob(jobId);
        virtualMachine.setGoogleJob(googleJob);
        virtualMachine.setVmServiceOffering(createVmServiceOffering(matcher));
        return virtualMachine;
    }

    private static VmServiceOffering createVmServiceOffering(Matcher matcher) {
        VmServiceOffering vmServiceOffering = new VmServiceOffering();
        vmServiceOffering.setName(matcher.group(5));
        int numberOfCores = Integer.parseInt(matcher.group(6));
        vmServiceOffering.setNumberOfCores(numberOfCores);
        vmServiceOffering.setCoreSpeed(Integer.parseInt(matcher.group(7)) / numberOfCores);
        vmServiceOffering.setMemoryInMegaByte(Long.parseLong(matcher.group(8)));
        return vmServiceOffering;
    }

    private static void validateInputFile(String[] args) {
        if (args.length != 1) {
            throw new GoogleTracesToCloudTracesException("You should inform the full qualified path to the cloud traces data set.");
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