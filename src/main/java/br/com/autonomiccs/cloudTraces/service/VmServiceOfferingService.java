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
package br.com.autonomiccs.cloudTraces.service;

import java.util.ArrayList;
import java.util.List;

import br.com.autonomiccs.cloudTraces.beans.VmServiceOffering;

public class VmServiceOfferingService {

    public static long oneGigaByteInMegaByte = 1024;

    private static int coreSpeedInMhz = 3400;

    /**
     * You should let the smallest service offerings at the top of the list (lowest positions).
     * Position 0 (zero) will have the smallest possible service offering, and position {@link List#size()} -1 will have the biggest.
     */
    public static List<VmServiceOffering> getVmServiceOfferings() {
        List<VmServiceOffering> vmServiceOfferings = new ArrayList<>();
        // These value were taken from https://aws.amazon.com/ec2/pricing/, using the general purpose instance configs
        addMicroServiceOffering(vmServiceOfferings);
        addSmallServiceOffering(vmServiceOfferings);
        addMediumServiceOffering(vmServiceOfferings);
        addLargeServiceOffering(vmServiceOfferings);
        addXLargeServiceOffering(vmServiceOfferings);
        add2XLargeServiceOffering(vmServiceOfferings);
        add4XLargeServiceOffering(vmServiceOfferings);
        add10XLargeServiceOffering(vmServiceOfferings);
        add15XLargeServiceOffering(vmServiceOfferings);
        add20XLargeServiceOffering(vmServiceOfferings);
        add25XLargeServiceOffering(vmServiceOfferings);
        add30XLargeServiceOffering(vmServiceOfferings);
        return vmServiceOfferings;
    }

    private static void add30XLargeServiceOffering(List<VmServiceOffering> vmServiceOfferings) {
        vmServiceOfferings.add(createVmServiceOffering(200, "30Xlarge", 380 * oneGigaByteInMegaByte));
    }

    private static void add25XLargeServiceOffering(List<VmServiceOffering> vmServiceOfferings) {
        vmServiceOfferings.add(createVmServiceOffering(160, "25Xlarge", 300 * oneGigaByteInMegaByte));
    }

    private static void add20XLargeServiceOffering(List<VmServiceOffering> vmServiceOfferings) {
        vmServiceOfferings.add(createVmServiceOffering(120, "20Xlarge", 260 * oneGigaByteInMegaByte));
    }

    private static void add15XLargeServiceOffering(List<VmServiceOffering> vmServiceOfferings) {
        vmServiceOfferings.add(createVmServiceOffering(80, "15Xlarge", 200 * oneGigaByteInMegaByte));
    }

    private static void add10XLargeServiceOffering(List<VmServiceOffering> vmServiceOfferings) {
        vmServiceOfferings.add(createVmServiceOffering(40, "10Xlarge", 160 * oneGigaByteInMegaByte));
    }

    private static void add4XLargeServiceOffering(List<VmServiceOffering> vmServiceOfferings) {
        vmServiceOfferings.add(createVmServiceOffering(16, "4Xlarge", 64 * oneGigaByteInMegaByte));
    }

    private static void add2XLargeServiceOffering(List<VmServiceOffering> vmServiceOfferings) {
        vmServiceOfferings.add(createVmServiceOffering(8, "2Xlarge", 32 * oneGigaByteInMegaByte));
    }

    private static void addXLargeServiceOffering(List<VmServiceOffering> vmServiceOfferings) {
        vmServiceOfferings.add(createVmServiceOffering(4, "Xlarge", 16 * oneGigaByteInMegaByte));
    }

    private static void addLargeServiceOffering(List<VmServiceOffering> vmServiceOfferings) {
        vmServiceOfferings.add(createVmServiceOffering(2, "large", 8 * oneGigaByteInMegaByte));
    }

    private static void addMediumServiceOffering(List<VmServiceOffering> vmServiceOfferings) {
        vmServiceOfferings.add(createVmServiceOffering(2, "medium", 4 * oneGigaByteInMegaByte));
    }

    private static void addSmallServiceOffering(List<VmServiceOffering> vmServiceOfferings) {
        vmServiceOfferings.add(createVmServiceOffering(1, "small", 2 * oneGigaByteInMegaByte));
    }

    private static void addMicroServiceOffering(List<VmServiceOffering> vmServiceOfferings) {
        vmServiceOfferings.add(createVmServiceOffering(1, "micro", 1 * oneGigaByteInMegaByte));
    }

    private static VmServiceOffering createVmServiceOffering(int numberOfCores, String name, long amountOfRamInMegabytes) {
        VmServiceOffering vmServiceOffering = new VmServiceOffering();
        vmServiceOffering.setName(name);
        vmServiceOffering.setNumberOfCores(numberOfCores);
        vmServiceOffering.setCoreSpeed(coreSpeedInMhz);
        vmServiceOffering.setMemoryInMegaByte(amountOfRamInMegabytes);
        return vmServiceOffering;
    }

}
