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
package br.com.autonomiccs.cloudTraces.beans;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;

public class GoogleJob {

    private final static Logger logger = Logger.getLogger(GoogleJob.class);

    private int startTime;
    private int endTime;
    private int jobId;
    private int jobType;

    private double maximumCpuUsageAtTime = NumberUtils.DOUBLE_MINUS_ONE;
    private double maximumMemoryUsageAtTime = NumberUtils.DOUBLE_MINUS_ONE;

    private int timeWithPeakCpuUsage;
    private int timeWithPeakMemoryUsage;

    private Queue<GoogleTask> tasks = new PriorityQueue<>();
    private Map<Integer, List<GoogleTask>> mapTimeByTasks = new HashMap<>();

    public GoogleJob(Integer jobId) {
        this.jobId = jobId;
    }

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public int getEndTime() {
        return endTime;
    }

    public void setEndTime(int endTime) {
        this.endTime = endTime;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public int getJobType() {
        return jobType;
    }

    public void setJobType(int jobType) {
        this.jobType = jobType;
    }

    public Queue<GoogleTask> getTasks() {
        return tasks;
    }

    public Map<Integer, List<GoogleTask>> getMapTimeByTasks() {
        return mapTimeByTasks;
    }

    public double getMaximumCpuUsageAtTime() {
        return maximumCpuUsageAtTime;
    }

    public void setMaximumCpuUsageAtTime(double maximumCpuUsageAtTime) {
        this.maximumCpuUsageAtTime = maximumCpuUsageAtTime;
    }

    public double getMaximumMemoryUsageAtTime() {
        return maximumMemoryUsageAtTime;
    }

    public void setMaximumMemoryUsageAtTime(double maximumMemoryUsageAtTime) {
        this.maximumMemoryUsageAtTime = maximumMemoryUsageAtTime;
    }

    public int getTimeWithPeakCpuUsage() {
        return timeWithPeakCpuUsage;
    }

    public void setTimeWithPeakCpuUsage(int timeWithPeakCpuUsage) {
        this.timeWithPeakCpuUsage = timeWithPeakCpuUsage;
    }

    public int getTimeWithPeakMemoryUsage() {
        return timeWithPeakMemoryUsage;
    }

    public void setTimeWithPeakMemoryUsage(int timeWithPeakMemoryUsage) {
        this.timeWithPeakMemoryUsage = timeWithPeakMemoryUsage;
    }

    @Override
    public String toString() {
        logger.debug(String.format("toString of: Job id [%s], number of tasks (with duplicated ones) [%d]", jobId, tasks.size()));
        int numberOfDuplicates = 0;
        Set<Integer> taskIds = new HashSet<>(tasks.size());
        for (GoogleTask t : tasks) {
            if (!taskIds.contains(t.getTaskId())) {
                taskIds.add(t.getTaskId());
                continue;
            }
            numberOfDuplicates++;
        }
        return String.format(
                "id [%d], start time [%d], end time [%d], type [%d], cpu usage peak [%.10f], memory usage peak [%.10f], time cpu peak [%d], time memory peak [%d], amount of tasks [%d]",
                jobId, startTime, endTime, jobType, maximumCpuUsageAtTime, maximumMemoryUsageAtTime, timeWithPeakCpuUsage, timeWithPeakMemoryUsage,
                tasks.size() - numberOfDuplicates);
    }
}
