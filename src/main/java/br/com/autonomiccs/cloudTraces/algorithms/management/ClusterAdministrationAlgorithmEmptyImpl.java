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
package br.com.autonomiccs.cloudTraces.algorithms.management;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.autonomiccs.cloudTraces.beans.Host;
import br.com.autonomiccs.cloudTraces.beans.VirtualMachine;

public class ClusterAdministrationAlgorithmEmptyImpl implements ClusterAdministrationAlgorithm {

    /**
     * This constant was created to divide a number (in Bytes) by 1.000.000 (resulting in a number
     * of Megabytes metrics).
     */
    protected final static int BYTES_TO_MEGA_BYTES = 1000000;

    @Override
    public List<Host> rankHosts(List<Host> hosts) {
        return hosts;
    }

    @Override
    public Map<VirtualMachine, Host> mapVMsToHost(List<Host> rankedHosts) {
        return new HashMap<>();
    }
}