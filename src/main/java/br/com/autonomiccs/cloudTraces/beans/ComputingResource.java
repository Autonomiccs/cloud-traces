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

public abstract class ComputingResource implements Cloneable {

    protected static final long NUMBER_OF_BYTES_IN_ONE_MEGA_BYTE = 1024l;

    private String id;

    private long totalMemoryInBytes;
    private long totalCpuPowerInMhz;

    private long memoryAllocatedInBytes;
    private long cpuAllocatedInMhz;

    private long memoryUsedInBytes;
    private long cpuUsedInMhz;

    public ComputingResource(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return String.format(
                "id [%s], total memory [%dMB], total cpu [%dMhz], allocated memory [%dMB], allocated cpu [%dMhz], allocated memory in %% [%.2f], allocated cpu in %% [%.2f], memory used [%dMb], cpu used [%dMhz], used memory in %% [%.2f], used cpu in %% [%.2f]",
                id, getTotalMemoryInMib(), totalCpuPowerInMhz, getMemoryAllocatedInMib(), cpuAllocatedInMhz, getMemoryAllocatedAsPercentage(), getCpuAllocatedAsPercentage(),
                getMemoryUsedInMib(), cpuUsedInMhz, getUsedMemoryAsPercentage(), getUsedCpuAsPercentage());
    }

    private double getUsedMemoryAsPercentage() {
        return getMemoryAllocatedInBytes() > 0 ? (getMemoryUsedInBytes() / getLongAsDouble(getMemoryAllocatedInBytes())) * 100 : 0;
    }

    private double getUsedCpuAsPercentage() {
        return cpuAllocatedInMhz > 0 ? (getCpuUsedInMhz() / getLongAsDouble(cpuAllocatedInMhz)) * 100 : 0;
    }

    private double getMemoryAllocatedAsPercentage() {
        return (getMemoryAllocatedInBytes() / getLongAsDouble(getTotalMemoryInBytes())) * 100;
    }

    private double getCpuAllocatedAsPercentage() {
        return (cpuAllocatedInMhz / getLongAsDouble(totalCpuPowerInMhz)) * 100;
    }

    private double getLongAsDouble(long value) {
        return value * 1d;
    }

    public long getTotalMemoryInMib() {
        return totalMemoryInBytes / NUMBER_OF_BYTES_IN_ONE_MEGA_BYTE;
    }

    public long getMemoryUsedInMib() {
        return memoryUsedInBytes / NUMBER_OF_BYTES_IN_ONE_MEGA_BYTE;
    }

    public long getMemoryAllocatedInMib() {
        return memoryAllocatedInBytes / NUMBER_OF_BYTES_IN_ONE_MEGA_BYTE;
    }

    public String getId() {
        return id;
    }

    public long getTotalMemoryInBytes() {
        return totalMemoryInBytes;
    }

    public void setTotalMemoryInBytes(long totalMemoryInBytes) {
        this.totalMemoryInBytes = totalMemoryInBytes;
    }

    public long getTotalCpuPowerInMhz() {
        return totalCpuPowerInMhz;
    }

    public void setTotalCpuPowerInMhz(long totalCpuPowerInMhz) {
        this.totalCpuPowerInMhz = totalCpuPowerInMhz;
    }

    public long getMemoryAllocatedInBytes() {
        return memoryAllocatedInBytes;
    }

    public void setMemoryAllocatedInBytes(long memoryAllocatedInBytes) {
        this.memoryAllocatedInBytes = memoryAllocatedInBytes;
    }

    public long getCpuAllocatedInMhz() {
        return cpuAllocatedInMhz;
    }

    public void setCpuAllocatedInMhz(long cpuAllocatedInMhz) {
        this.cpuAllocatedInMhz = cpuAllocatedInMhz;
    }

    public long getMemoryUsedInBytes() {
        return memoryUsedInBytes;
    }

    public void setMemoryUsedInMiB(long memoryUsedInBytes) {
        this.memoryUsedInBytes = memoryUsedInBytes * NUMBER_OF_BYTES_IN_ONE_MEGA_BYTE;
    }

    public void setMemoryUsedInBytes(long memoryUsedInBytes) {
        this.memoryUsedInBytes = memoryUsedInBytes;
    }

    public long getCpuUsedInMhz() {
        return cpuUsedInMhz;
    }

    public void setCpuUsedInMhz(long cpuUsedInMhz) {
        this.cpuUsedInMhz = cpuUsedInMhz;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
