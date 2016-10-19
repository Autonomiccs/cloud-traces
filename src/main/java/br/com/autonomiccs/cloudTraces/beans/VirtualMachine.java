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

public class VirtualMachine {

    private String vmId;

    private int deployTime;
    private int destroyTime;

    private VmServiceOffering vmServiceOffering;
    private GoogleJob googleJob;

    private Host host;

    public int getDeployTime() {
        return deployTime;
    }

    public void setDeployTime(int deployTime) {
        this.deployTime = deployTime;
    }

    public int getDestroyTime() {
        return destroyTime;
    }

    public void setDestroyTime(int destroyTime) {
        this.destroyTime = destroyTime;
    }

    public VmServiceOffering getVmServiceOffering() {
        return vmServiceOffering;
    }

    public void setVmServiceOffering(VmServiceOffering vmServiceOffering) {
        this.vmServiceOffering = vmServiceOffering;
    }

    public GoogleJob getGoogleJob() {
        return googleJob;
    }

    public void setVmId(String vmId) {
        this.vmId = vmId;
    }

    public String getVmId() {
        return vmId;
    }

    public void setGoogleJob(GoogleJob googleJob) {
        this.googleJob = googleJob;
    }

    public void setHost(Host host) {
        this.host = host;
    }

    public Host getHost() {
        return host;
    }

    @Override
    public String toString() {
        return String.format("id [%s], deploy time [%d], destroy time [%d], host id [%s], allocated cpu [%dMhz], allocated memory [%dMB]", vmId, deployTime, destroyTime,
                host != null ? host.getId() : null, vmServiceOffering.getCoreSpeed() * vmServiceOffering.getNumberOfCores(), vmServiceOffering.getMemoryInMegaByte());
    }
}
