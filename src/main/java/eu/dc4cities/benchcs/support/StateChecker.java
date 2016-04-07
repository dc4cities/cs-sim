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

import eu.dc4cities.controlsystem.model.TimeSlotBasedEntity;
import eu.dc4cities.controlsystem.model.easc.*;
import eu.dc4cities.controlsystem.modules.optionconsolidator.opcp2.*;
import org.joda.time.DateTime;
import org.jscience.physics.amount.Amount;

import javax.measure.unit.SI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;

/**
 * Created by fhermeni on 17/09/2015.
 */
public class StateChecker {

    public static void toCSV(Collection<State> states) {
        System.out.println("name;perf;power");
        for (State st : states) {
            System.out.println(st.name() + ";" + st.perf() + ";" + st.power());
        }
    }

    public static void perfModel(EascActivitySpecifications specs) {
        for (ActivitySpecification a : specs.getActivitySpecifications()) {
            //System.out.println("- Activity:"  + a .getActivityName() + " --- ");
            perfModel(specs.getEascName(), a);
        }
    }

    public static boolean perfModel(String easc, ActivitySpecification spec) {

        TimeSlotBasedEntity range = new TimeSlotBasedEntity();
        range.setTimeSlotDuration(Amount.valueOf(15 * 60, SI.SECOND));

        //Pick the largest video possible
        DateTime from = spec.getServiceLevelObjectives().get(0).getDateFrom();
        DateTime to = spec.getServiceLevelObjectives().get(0).getDateTo();

        for (ServiceLevelObjective slo : spec.getServiceLevelObjectives()) {
            if (slo.getDateFrom().isBefore(from)) {
                from = slo.getDateFrom();
            }
            if (slo.getDateTo().isAfter(to)) {
                to = slo.getDateTo();
            }
        }
        range.setDateFrom(from);
        range.setDateTo(to);

        ActivityMetrics am = new ActivityMetrics(spec.getActivityName());
        am.setDataCenters(new ArrayList<>());
        for (DataCenterSpecification p : spec.getDataCenters()) {
            ActivityDataCenterMetrics dm = new ActivityDataCenterMetrics(p.getDataCenterName());
            dm.setWorkingModeName(p.getDefaultWorkingMode());
            am.getDataCenters().add(dm);
        }
        MyActivity ma = Converter.myActivity(range, easc, spec, new ArrayList<>(), am);
        ActivityAutomaton auto = new ActivityAutomaton(ma);
        TreeSet<State> states = new TreeSet<>(new StatePerfComparator());
        for (int i = 0; i < auto.nbStates(); i++) {
            states.add(auto.state(i));
        }

        State s = null;
        for (State st : states) {
            if (s != null) {
                if (s.power() >= st.power()) {
                    System.out.println(st + " is not a viable state wrt. " + s);
                    return false;
                }
            } else {
                s = st;
            }
        }

        toCSV(states);
        return true;
    }
}
