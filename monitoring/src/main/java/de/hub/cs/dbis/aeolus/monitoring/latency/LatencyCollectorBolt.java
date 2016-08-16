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
package de.hub.cs.dbis.aeolus.monitoring.latency;

import java.util.Map;

import org.apache.storm.guava.collect.TreeMultiset;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import de.hub.cs.dbis.aeolus.monitoring.MonitoringTopoloyBuilder;





/**
 * {@link LatencyCollectorBolt} appends a "create timestamp" attribute to all emitted tuples. The appended timestamps
 * are taken from the current input tuple's create timestamp attribute (see {@link SpoutTimestampAppender}).
 * 
 * @author Matthias J. Sax
 */
public class LatencyCollectorBolt implements IRichBolt {
	private static final long serialVersionUID = -4286831058178290593L;
	
	/** The original user bolt. */
	private final IRichBolt userBolt;
	
	/** The collector that provide the "finished processing" timestamp. */
	private BoltEndTimestampCollector collector;
	
	int maxSize = 1000;
	private final TreeMultiset<Long> latencies = TreeMultiset.create();
	private final Long[] asArray = new Long[this.maxSize];
	private long sum = 0;
	
	/**
	 * Instantiate a new {@link LatencyCollectorBolt} that appends "create timestamps" to all emitted tuples of the
	 * given bolt.
	 * 
	 * @param userBolt
	 *            The user bolt to be monitored.
	 */
	public LatencyCollectorBolt(IRichBolt userBolt) {
		this.userBolt = userBolt;
	}
	
	@Override
	public void prepare(@SuppressWarnings("rawtypes") Map stormConf, TopologyContext context, @SuppressWarnings("hiding") OutputCollector collector) {
		this.collector = new BoltEndTimestampCollector(collector);
		this.userBolt.prepare(stormConf, context, this.collector);
	}
	
	@Override
	public void execute(Tuple input) {
		long createTimestamp = input.getLong(input.size() - 1).longValue();
		this.userBolt.execute(input);
		
		long latency = this.collector.endTimestamp - createTimestamp;
		this.sum += latency;
		this.latencies.add(new Long(latency));
		if(this.latencies.size() == this.maxSize) {
			this.latencies.toArray(this.asArray);
			
			System.err.println(System.currentTimeMillis() + " max: " + this.asArray[this.maxSize - 1] + "; 99: "
				+ this.asArray[(this.maxSize * 99) / 100 - 1] + "; 95: " + this.asArray[(this.maxSize * 95) / 100 - 1]
				+ "; med: " + this.asArray[this.maxSize / 2 - 1] + "; avg: " + (this.sum / this.maxSize) + "; min: "
				+ this.asArray[0]);
			
			this.latencies.clear();
			this.sum = 0;
		}
	}
	
	@Override
	public void cleanup() {
		this.userBolt.cleanup();
	}
	
	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		this.userBolt.declareOutputFields(declarer);
		declarer.declareStream(MonitoringTopoloyBuilder.DEFAULT_LATANCY_STREAM, new Fields("ts", "max", "99", "95",
			"med", "avg", "min"));
	}
	
	@Override
	public Map<String, Object> getComponentConfiguration() {
		return this.userBolt.getComponentConfiguration();
	}
	
}
