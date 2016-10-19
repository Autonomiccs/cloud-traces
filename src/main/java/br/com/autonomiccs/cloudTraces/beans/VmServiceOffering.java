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

/**
 *  This Class represents the amount of resource that is offerted for the VM
 */
public class VmServiceOffering {

    private String name;

    private int numberOfCores;
    private int coreSpeed;
    private long memoryInMegaByte;

    public int getNumberOfCores() {
        return numberOfCores;
    }

    public void setNumberOfCores(int numberOfCores) {
        this.numberOfCores = numberOfCores;
    }

    public int getCoreSpeed() {
        return coreSpeed;
    }

    public void setCoreSpeed(int coreSpeed) {
        this.coreSpeed = coreSpeed;
    }

    public long getMemoryInMegaByte() {
        return memoryInMegaByte;
    }

    public void setMemoryInMegaByte(long memoryInMegaByte) {
        this.memoryInMegaByte = memoryInMegaByte;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return String.format("name [%s], #cores[%d], coreSpeed [%d], memory [%d]", name, numberOfCores, coreSpeed, memoryInMegaByte);
    }
}
