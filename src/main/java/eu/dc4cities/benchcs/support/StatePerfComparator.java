/*
 * Copyright 2012 The DC4Cities author.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.dc4cities.benchcs.support;

import eu.dc4cities.controlsystem.modules.optionconsolidator.opcp2.State;

import java.util.Comparator;

/**
 * Created by fhermeni on 17/09/2015.
 */
public class StatePerfComparator implements Comparator<State> {

    @Override
    public int compare(State o1, State o2) {
        return o1.perf() - o2.perf();
    }
}
