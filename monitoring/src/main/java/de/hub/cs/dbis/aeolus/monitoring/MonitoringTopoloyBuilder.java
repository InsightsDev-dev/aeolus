/*
 * #!
 * %
 * Copyright (C) 2014 - 2016 Humboldt-Universität zu Berlin
 * %
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
 * #_
 */
package de.hub.cs.dbis.aeolus.monitoring;

import backtype.storm.topology.BoltDeclarer;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.IRichSpout;
import backtype.storm.topology.SpoutDeclarer;
import backtype.storm.topology.TopologyBuilder;
import de.hub.cs.dbis.aeolus.monitoring.latency.LatencyBolt;
import de.hub.cs.dbis.aeolus.monitoring.latency.LatencyCollectorBolt;
import de.hub.cs.dbis.aeolus.monitoring.latency.LatencySpout;





/**
 * {@link MonitoringTopoloyBuilder} allows to automatically insert monitoring wrapper spouts and bolt into a topology to
 * collect throughput and latency statistics.
 * 
 * @author Matthias J. Sax
 */
public class MonitoringTopoloyBuilder extends TopologyBuilder {
	/** The default ID of the throughput report stream. */
	public final static String DEFAULT_THROUGHPUT_STREAM = "aeolus::throughput";
	/** The default ID of the latency report stream. */
	public final static String DEFAULT_LATANCY_STREAM = "aeolus::latency";
	
	private final boolean meassureThroughput;
	private final boolean meassureLatency;
	
	
	
	public MonitoringTopoloyBuilder() {
		this(true, true);
	}
	
	public MonitoringTopoloyBuilder(boolean meassureThroughput, boolean meassureLatency) {
		this.meassureThroughput = meassureThroughput;
		this.meassureLatency = meassureLatency;
	}
	
	
	
	@Override
	public SpoutDeclarer setSpout(String id, IRichSpout spout, Number parallelismHint) {
		if(this.meassureThroughput) {
			// TODO
		}
		if(this.meassureLatency) {
			spout = new LatencySpout(spout);
		}
		
		return super.setSpout(id, spout, parallelismHint);
	}
	
	@Override
	public BoltDeclarer setBolt(String id, IRichBolt bolt, Number parallelismHint) {
		if(this.meassureThroughput) {
			// TODO
		}
		if(this.meassureLatency) {
			bolt = new LatencyBolt(bolt);
		}
		
		return super.setBolt(id, bolt, parallelismHint);
	}
	
	public BoltDeclarer setSink(String id, IRichBolt bolt) {
		return this.setSink(id, bolt, null);
	}
	
	public BoltDeclarer setSink(String id, IRichBolt bolt, Number parallelismHint) {
		if(this.meassureThroughput) {
			// TODO
		}
		if(this.meassureLatency) {
			bolt = new LatencyCollectorBolt(bolt);
		}
		
		return super.setBolt(id, bolt, parallelismHint);
	}
	
}
