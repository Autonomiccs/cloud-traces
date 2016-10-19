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

public class GoogleTrace {

    private int time;
    private int jobId;
    private int taskId;
    private int jobType;
    private double normalizedTaskCores;
    private double normalizedTaskMemory;

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public int getJobType() {
        return jobType;
    }

    public void setJobType(int jobType) {
        this.jobType = jobType;
    }

    public double getNormalizedTaskCores() {
        return normalizedTaskCores;
    }

    public void setNormalizedTaskCores(double normalizedTaskCores) {
        this.normalizedTaskCores = normalizedTaskCores;
    }

    public double getNormalizedTaskMemory() {
        return normalizedTaskMemory;
    }

    public void setNormalizedTaskMemory(double normalizedTaskMemory) {
        this.normalizedTaskMemory = normalizedTaskMemory;
    }

    @Override
    public String toString() {
        return String.format("Task id [%d], job id [%d], time [%d], job type [%d], normalizedTaskCore [%.10f], normalizedTaskMemory [%.10f]", taskId,
                jobId, time, jobType, normalizedTaskCores, normalizedTaskMemory);
    }

}
