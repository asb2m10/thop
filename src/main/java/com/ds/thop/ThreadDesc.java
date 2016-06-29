/*
 * Copyright 2016 Pascal Gauthier.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ds.thop;

public class ThreadDesc {
    /**
     * Name of the thread given by the program
     */
    String name;

    /**
     * Current stacktrace when snapshot was taken
     */
    String stack;

    /**
     * Thread state (running/waiting/blocked/limbo)
     */
    Thread.State state;

    /**
     * Internal thread id
     */
    long id;

    /**
     * CPU time consumed for this thtread since start.
     */
    long cpuTm;

    /**
     * The delta of the cpu tm since last snapshot
     */
    double cpuDelta;

    /**
     * Usage percentage (calculated from cpuDelta and the interval between the last snapshot.
     */
    double per;
}
