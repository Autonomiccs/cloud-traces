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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Cloud extends ComputingResource {

    private List<Cluster> clusters = new ArrayList<>();
    private Set<VirtualMachine> virtualMachines = new HashSet<>();

    public Cloud(String id) {
        super(id);
    }

    public List<Cluster> getClusters() {
        return clusters;
    }

    public void addVirtualMachine(VirtualMachine vm) {
        virtualMachines.add(vm);
    }

    public void destroyVirtualMachine(VirtualMachine vm) {
        boolean removedVm = virtualMachines.remove(vm);
        assert removedVm;
    }

    public Set<VirtualMachine> getVirtualMachines() {
        return virtualMachines;
    }

    @Override
    public String toString() {
        return String.format("Cloud %s, #clusters[%d]", super.toString(), clusters.size());
    }

}
