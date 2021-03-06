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
package storm.lrb;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import storm.lrb.bolt.AccountBalanceBolt;
import storm.lrb.bolt.DailyExpenditureBolt;
import storm.lrb.tools.Helper;
import storm.lrb.tools.StopWatch;
import backtype.storm.Config;
import backtype.storm.generated.StormTopology;
import backtype.storm.topology.IRichSpout;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;
import de.hub.cs.dbis.aeolus.sinks.FileSinkBolt;
import de.hub.cs.dbis.lrb.operators.AccidentDetectionBolt;
import de.hub.cs.dbis.lrb.operators.AccidentNotificationBolt;
import de.hub.cs.dbis.lrb.operators.AverageVehicleSpeedBolt;
import de.hub.cs.dbis.lrb.operators.DispatcherBolt;
import de.hub.cs.dbis.lrb.operators.LatestAverageVelocityBolt;
import de.hub.cs.dbis.lrb.operators.TollNotificationBolt;
import de.hub.cs.dbis.lrb.queries.utils.TopologyControl;
import de.hub.cs.dbis.lrb.toll.MemoryTollDataStore;





/**
 * An encapsulation of parameters which allows fast switches of the spout. Does everything essential for the cluster and
 * allow to manipulate {@link Config} and {@link StormTopology} at will after creation in constructor.
 * 
 * @author richter
 * @author mjsax
 */
public class LRBTopology {
	private final static Logger LOGGER = LoggerFactory.getLogger(LRBTopology.class);
	private final StormTopology stormTopology;
	private final String tollDataStoreClass = MemoryTollDataStore.class.getName();
	
	/**
	 * performs the assembly of the topology which can be retrieved with {@link #getStormTopology() }.
	 * 
	 * @param nameext
	 * @param xways
	 * @param workers
	 * @param tasks
	 * @param executors
	 * @param offset
	 * @param spout
	 * @param stormTimer
	 * @param local
	 * @param histFile
	 * @param topologyNamePrefix
	 * @param stormConfig
	 *            an existing {@link Config} object to be filled with serialization and custom bolt information
	 */
	public LRBTopology(String nameext, int xways, int workers, int tasks, int executors, int offset, IRichSpout spout,
		StopWatch stormTimer, boolean local, String histFile, String topologyNamePrefix, Config stormConfig) {
		List<String> fields = new LinkedList<String>(Arrays.asList(TopologyControl.XWAY_FIELD_NAME,
			TopologyControl.DIRECTION_FIELD_NAME));
		TopologyBuilder builder = new TopologyBuilder();
		
		builder.setSpout(TopologyControl.SPOUT_NAME, spout, 1);
		
		builder.setBolt(TopologyControl.SPLIT_STREAM_BOLT_NAME, new DispatcherBolt(), 2 * xways).setNumTasks(4 * xways)
			.shuffleGrouping(TopologyControl.SPOUT_NAME);// .allGrouping("Spout",
															// "stormtimer");
		
		builder.setBolt(TopologyControl.AVERAGE_SPEED_BOLT_NAME, new AverageVehicleSpeedBolt(), xways * 3)
			.fieldsGrouping(
				TopologyControl.SPLIT_STREAM_BOLT_NAME,
				TopologyControl.POSITION_REPORTS_STREAM_ID,
				new Fields(TopologyControl.XWAY_FIELD_NAME, TopologyControl.SEGMENT_FIELD_NAME,
					TopologyControl.DIRECTION_FIELD_NAME));
		
		builder.setBolt(TopologyControl.LATEST_AVERAGE_SPEED_BOLT_NAME, new LatestAverageVelocityBolt(), xways * 3)
			.fieldsGrouping(
				TopologyControl.AVERAGE_SPEED_BOLT_NAME,
				TopologyControlOld.LAST_AVERAGE_SPEED_STREAM_ID,
				new Fields(TopologyControl.XWAY_FIELD_NAME, TopologyControl.SEGMENT_FIELD_NAME,
					TopologyControl.DIRECTION_FIELD_NAME));
		
		
		builder.setBolt(TopologyControl.TOLL_ASSESSMENTS_FILE_WRITER_BOLT_NAME,
			new FileSinkBolt(topologyNamePrefix + "_toll"), 1).allGrouping(TopologyControl.TOLL_NOTIFICATION_BOLT_NAME,
			TopologyControl.TOLL_ASSESSMENTS_STREAM_ID);
		
		// builder.setBolt("lavBolt", new SegmentStatsBolt(0), cmd.xways*3)
		// .fieldsGrouping("SplitStreamBolt", "PosReports", new Fields("xsd"));
		builder
			.setBolt(TopologyControl.TOLL_NOTIFICATION_BOLT_NAME, new TollNotificationBolt(), executors)
			.setNumTasks(tasks)
			.fieldsGrouping(TopologyControl.LATEST_AVERAGE_SPEED_BOLT_NAME,
				TopologyControlOld.LAST_AVERAGE_SPEED_STREAM_ID, new Fields(fields))
			.fieldsGrouping(
				TopologyControl.ACCIDENT_DETECTION_BOLT_NAME,
				TopologyControlOld.ACCIDENT_INFO_STREAM_ID,
				new Fields(TopologyControlOld.POS_REPORT_FIELD_NAME, TopologyControl.SEGMENT_FIELD_NAME,
					TopologyControlOld.ACCIDENT_INFO_FIELD_NAME))
			.fieldsGrouping(TopologyControl.SPLIT_STREAM_BOLT_NAME, TopologyControl.POSITION_REPORTS_STREAM_ID,
				new Fields(fields));
		
		builder.setBolt(TopologyControl.ACCIDENT_DETECTION_BOLT_NAME, new AccidentDetectionBolt(), xways)
			.fieldsGrouping(TopologyControl.SPLIT_STREAM_BOLT_NAME, TopologyControl.POSITION_REPORTS_STREAM_ID,
				new Fields(TopologyControl.XWAY_FIELD_NAME, TopologyControl.DIRECTION_FIELD_NAME));
		
		builder.setBolt(TopologyControl.ACCIDENT_NOTIFICATION_BOLT_NAME, new AccidentNotificationBolt(), xways)
			.fieldsGrouping(TopologyControl.ACCIDENT_DETECTION_BOLT_NAME, TopologyControlOld.ACCIDENT_INFO_STREAM_ID, // streamId
				new Fields(TopologyControlOld.POS_REPORT_FIELD_NAME))
			.fieldsGrouping(TopologyControl.SPLIT_STREAM_BOLT_NAME, TopologyControl.POSITION_REPORTS_STREAM_ID, // streamId
				new Fields(TopologyControl.XWAY_FIELD_NAME, TopologyControl.DIRECTION_FIELD_NAME));
		
		builder.setBolt(TopologyControl.ACCIDENT_FILE_WRITER_BOLT_NAME, new FileSinkBolt(topologyNamePrefix + "_acc"),
			1).allGrouping(TopologyControl.ACCIDENT_DETECTION_BOLT_NAME, TopologyControlOld.ACCIDENT_INFO_STREAM_ID);
		
		builder
			.setBolt(TopologyControlOld.ACCOUNT_BALANCE_BOLT_NAME, new AccountBalanceBolt(), xways)
			.fieldsGrouping(TopologyControl.SPLIT_STREAM_BOLT_NAME, TopologyControl.ACCOUNT_BALANCE_REQUESTS_STREAM_ID,
				new Fields(TopologyControl.VEHICLE_ID_FIELD_NAME))
			.fieldsGrouping(TopologyControl.TOLL_NOTIFICATION_BOLT_NAME, TopologyControl.TOLL_ASSESSMENTS_STREAM_ID,
				new Fields(TopologyControlOld.ACCOUNT_BALANCE_REQUEST_FIELD_NAME));
		
		builder.setBolt(TopologyControlOld.ACCOUNT_BALANCE_FILE_WRITER_BOLT_NAME,
			new FileSinkBolt(topologyNamePrefix + "_bal"), 1).allGrouping(TopologyControlOld.ACCOUNT_BALANCE_BOLT_NAME);
		
		builder.setBolt(TopologyControlOld.DAILY_EXPEDITURE_BOLT_NAME, new DailyExpenditureBolt(), xways * 1)
			.shuffleGrouping(TopologyControl.SPLIT_STREAM_BOLT_NAME,
				TopologyControl.DAILY_EXPEDITURE_REQUESTS_STREAM_ID);
		
		builder.setBolt(TopologyControlOld.DAILY_EXPEDITURE_FILE_WRITER_BOLT_NAME,
			new FileSinkBolt(topologyNamePrefix + "_exp"), 1)
			.allGrouping(TopologyControlOld.DAILY_EXPEDITURE_BOLT_NAME);
		
		
		stormConfig.registerSerialization(de.hub.cs.dbis.lrb.types.PositionReport.class);
		stormConfig.registerSerialization(de.hub.cs.dbis.lrb.types.AccountBalanceRequest.class);
		stormConfig.registerSerialization(de.hub.cs.dbis.lrb.types.DailyExpenditureRequest.class);
		stormConfig.registerSerialization(de.hub.cs.dbis.lrb.types.TravelTimeRequest.class);
		stormConfig.registerSerialization(storm.lrb.model.Accident.class);
		stormConfig.registerSerialization(storm.lrb.model.VehicleInfo.class);
		stormConfig.registerSerialization(storm.lrb.tools.StopWatch.class);
		stormConfig.registerSerialization(storm.lrb.model.AccidentImmutable.class);
		stormConfig.put(Helper.TOLL_DATA_STORE_CONF_KEY, this.tollDataStoreClass);
		
		this.stormTopology = builder.createTopology();
		LOGGER.info(String.format("successfully created storm topology '%s'", TopologyControl.TOPOLOGY_NAME));
	}
	
	public StormTopology getStormTopology() {
		return this.stormTopology;
	}
	
}
