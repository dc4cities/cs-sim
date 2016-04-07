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

package eu.dc4cities.benchcs.mock;

import java.util.HashMap;
import java.util.Map;

import javax.measure.quantity.Frequency;
import javax.measure.quantity.Power;

import org.jscience.physics.amount.Amount;

import eu.dc4cities.easc.activity.DataCenterWorkingModes;
import eu.dc4cities.easc.Application;
import eu.dc4cities.easc.activity.Activity;
import eu.dc4cities.easc.monitoring.MonitorActivityWorkingMode;
import eu.dc4cities.easc.monitoring.WorkDone;
import eu.dc4cities.easc.workingmode.WorkingMode;

public class SimMonitorActivityWorkingMode extends WorkDone implements MonitorActivityWorkingMode {

	private Map<WmKey, WorkingMode> wmMap = new HashMap<>();
	
	public SimMonitorActivityWorkingMode(Application appConfig) {
		for (Activity activity : appConfig.getActivities()) {
			for (DataCenterWorkingModes dc : activity.getDataCenters()) {
				for (WorkingMode wm : dc.getWorkingModes()) {
					WmKey wmKey = new WmKey(activity.getName(), dc.getDataCenterName(), wm.getName());
					wmMap.put(wmKey, wm);
				}
			}
		}
	}
	
	private WorkingMode getWm(String activity, String dataCenter, String wm) {
		return wmMap.get(new WmKey(activity, dataCenter, wm));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Amount<Frequency> getInstantBusinessPerformance(String activity, String datacenter, String wm) {
		return (Amount<Frequency>) getWm(activity, datacenter, wm).getPerformanceLevels().get(0)
				.getBusinessPerformance();
	}

	@Override
	public Amount<Power> getWMPower(String activity, String datacenter,	String wm) {
		return getWm(activity, datacenter, wm).getPerformanceLevels().get(0).getPower();
	}

	private static class WmKey {
		
		public String activity;
		public String dataCenter;
		public String wm;
		
		public WmKey(String activity, String dataCenter, String wm) {
			this.activity = activity;
			this.dataCenter = dataCenter;
			this.wm = wm;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof WmKey)) {
				return false;
			}
			WmKey other = (WmKey) obj;
			return other.activity.equals(activity) && other.dataCenter.equals(dataCenter) && other.wm.equals(wm);
		}

		@Override
		public int hashCode() {
			return activity.hashCode() + dataCenter.hashCode() + wm.hashCode();
		}
		
	}
	
}
