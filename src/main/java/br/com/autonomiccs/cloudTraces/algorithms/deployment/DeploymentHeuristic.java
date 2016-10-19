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
package br.com.autonomiccs.cloudTraces.algorithms.deployment;

import java.util.List;

import br.com.autonomiccs.cloudTraces.beans.Cluster;
import br.com.autonomiccs.cloudTraces.beans.Host;
import br.com.autonomiccs.cloudTraces.beans.VirtualMachine;

public interface DeploymentHeuristic {

    /**
     * This method will sort the given cluster list. It has to return the very same objects. Therefore, only shallo copies are allowed here.
     * The clusters at the top (lowest positions) are the most interesting to deploy the VM, whereas the ones at the bottom are the least interesting ones.
     */
    public List<Cluster> getRankedClustersToDeployVirtualMachine(List<Cluster> clusters, VirtualMachine virtualMachine);

    /**
    * This method will sort the given host list. It has to return the very same objects. Therefore, only shallo copies are allowed here.
    * The hosts at the top (lowest positions) are the most interesting to deploy the VM, whereas the ones at the bottom are the least interesting ones.
    */
    public List<Host> getRankedHostsToDeployVirtualMachie(List<Host> hosts, VirtualMachine virtualMachine);
}
