/*
 * #!
 * %
 * Copyright (C) 2014 - 2015 Humboldt-Universität zu Berlin
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
package de.hub.cs.dbis.lrb.operators;

import static de.hub.cs.dbis.lrb.util.Constants.l0;
import static de.hub.cs.dbis.lrb.util.Constants.l1;
import static de.hub.cs.dbis.lrb.util.Constants.l2;
import static de.hub.cs.dbis.lrb.util.Constants.l4;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.stubbing.OngoingStubbing;

import storm.lrb.TopologyControl;
import backtype.storm.task.OutputCollector;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import de.hub.cs.dbis.aeolus.testUtils.TestDeclarer;
import de.hub.cs.dbis.aeolus.testUtils.TestOutputCollector;
import de.hub.cs.dbis.lrb.types.PositionReport;
import de.hub.cs.dbis.lrb.types.TollNotification;
import de.hub.cs.dbis.lrb.types.internal.AccidentTuple;
import de.hub.cs.dbis.lrb.types.internal.CountTuple;
import de.hub.cs.dbis.lrb.types.internal.LavTuple;
import de.hub.cs.dbis.lrb.util.Constants;





/**
 * @author mjsax
 */
public class TollNotificationBoltTest {
	private final static Integer vid = new Integer(21);
	private final static Integer xway = new Integer(1);
	private final static Short d = Constants.EASTBOUND; // direction
	private final static Integer pos = new Integer(0);
	private final static Integer spd = new Integer(0);
	
	@SuppressWarnings("boxing")
	@Test
	public void testExecute() {
		Tuple tuple = mock(Tuple.class);
		OngoingStubbing<List<Object>> valueStub = when(tuple.getValues());
		
		List<TollNotification> expectedNotifications = new ArrayList<TollNotification>();
		List<TollNotification> expectedAssessments = new ArrayList<TollNotification>();
		
		List<String> streamIdsToBeMocked = new LinkedList<String>();
		short time;
		
		// test 1: empty highway -> no toll
		
		// entering highway
		time = (short)0;
		streamIdsToBeMocked.add(TopologyControl.POSITION_REPORTS_STREAM_ID);
		valueStub = valueStub.thenReturn(new PositionReport(time, vid, spd, xway, l0, d, (short)5, pos));
		expectedNotifications.add(new TollNotification(time, time, vid, 0, 0));
		
		// same segment different lane
		time = (short)30;
		streamIdsToBeMocked.add(TopologyControl.POSITION_REPORTS_STREAM_ID);
		valueStub = valueStub.thenReturn(new PositionReport(time, vid, spd, xway, l1, d, (short)5, pos));
		
		streamIdsToBeMocked.add(TopologyControl.CAR_COUNTS_STREAM_ID);
		valueStub = valueStub.thenReturn(new CountTuple((short)1, xway, (short)6, d, 50));
		
		// crossing segment same lane
		time = (short)60;
		streamIdsToBeMocked.add(TopologyControl.POSITION_REPORTS_STREAM_ID);
		valueStub = valueStub.thenReturn(new PositionReport(time, vid, spd, xway, l1, d, (short)6, pos));
		expectedNotifications.add(new TollNotification(time, time, vid, 0, 0));
		
		// crossing segment different lane
		time = (short)90;
		streamIdsToBeMocked.add(TopologyControl.POSITION_REPORTS_STREAM_ID);
		valueStub = valueStub.thenReturn(new PositionReport(time, vid, spd, xway, l2, d, (short)7, pos));
		expectedNotifications.add(new TollNotification(time, time, vid, 0, 0));
		
		// crossing segment but exit highway
		time = (short)120;
		streamIdsToBeMocked.add(TopologyControl.POSITION_REPORTS_STREAM_ID);
		valueStub = valueStub.thenReturn(new PositionReport(time, vid, spd, xway, l4, d, (short)8, pos));
		
		
		
		// test 2: full and slow highway -> toll
		
		streamIdsToBeMocked.add(TopologyControl.CAR_COUNTS_STREAM_ID);
		valueStub = valueStub.thenReturn(new CountTuple((short)3, xway, (short)10, d, 51));
		
		// entering highway again
		time = (short)200;
		streamIdsToBeMocked.add(TopologyControl.POSITION_REPORTS_STREAM_ID);
		valueStub = valueStub.thenReturn(new PositionReport(time, vid, spd, xway, l0, d, (short)10, pos));
		expectedNotifications.add(new TollNotification(time, time, vid, 0, 2));
		
		// same segment different lane
		time = (short)230;
		streamIdsToBeMocked.add(TopologyControl.POSITION_REPORTS_STREAM_ID);
		valueStub = valueStub.thenReturn(new PositionReport(time, vid, spd, xway, l1, d, (short)10, pos));
		
		streamIdsToBeMocked.add(TopologyControl.CAR_COUNTS_STREAM_ID);
		valueStub = valueStub.thenReturn(new CountTuple((short)4, xway, (short)11, d, 52));
		streamIdsToBeMocked.add(TopologyControl.LAVS_STREAM_ID);
		valueStub = valueStub.thenReturn(new LavTuple((short)5, xway, (short)11, d, 39));
		// accident already passed; no effect
		streamIdsToBeMocked.add(TopologyControl.ACCIDENTS_STREAM_ID);
		valueStub = valueStub.thenReturn(new AccidentTuple((short)4, xway, (short)10, d));
		// accident too far downstream; no effect
		streamIdsToBeMocked.add(TopologyControl.ACCIDENTS_STREAM_ID);
		valueStub = valueStub.thenReturn(new AccidentTuple((short)4, xway, (short)16, d));
		
		// crossing segment same lane
		time = (short)260;
		streamIdsToBeMocked.add(TopologyControl.POSITION_REPORTS_STREAM_ID);
		valueStub = valueStub.thenReturn(new PositionReport(time, vid, spd, xway, l1, d, (short)11, pos));
		expectedNotifications.add(new TollNotification(time, time, vid, 39, 8));
		expectedAssessments.add(new TollNotification((short)200, (short)200, vid, 0, 2));
		
		// crossing segment but exit highway
		time = (short)290;
		streamIdsToBeMocked.add(TopologyControl.POSITION_REPORTS_STREAM_ID);
		valueStub = valueStub.thenReturn(new PositionReport(time, vid, spd, xway, l4, d, (short)12, pos));
		expectedAssessments.add(new TollNotification((short)260, (short)260, vid, 39, 8));
		
		
		
		// test 2: full but fast highway -> no toll
		
		streamIdsToBeMocked.add(TopologyControl.CAR_COUNTS_STREAM_ID);
		valueStub = valueStub.thenReturn(new CountTuple((short)6, xway, (short)20, d, 51));
		streamIdsToBeMocked.add(TopologyControl.LAVS_STREAM_ID);
		valueStub = valueStub.thenReturn(new LavTuple((short)7, xway, (short)20, d, 40));
		
		// entering highway again
		time = (short)400;
		streamIdsToBeMocked.add(TopologyControl.POSITION_REPORTS_STREAM_ID);
		valueStub = valueStub.thenReturn(new PositionReport(time, vid, spd, xway, l0, d, (short)20, pos));
		expectedNotifications.add(new TollNotification(time, time, vid, 40, 0));
		
		
		
		// test 4: full and slow highway but accident (same segment)
		
		streamIdsToBeMocked.add(TopologyControl.CAR_COUNTS_STREAM_ID);
		valueStub = valueStub.thenReturn(new CountTuple((short)8, xway, (short)30, d, 51));
		
		// entering highway again (no accident yet -> toll)
		time = (short)500;
		streamIdsToBeMocked.add(TopologyControl.POSITION_REPORTS_STREAM_ID);
		valueStub = valueStub.thenReturn(new PositionReport(time, vid, spd, xway, l0, d, (short)30, pos));
		expectedNotifications.add(new TollNotification(time, time, vid, 0, 2));
		
		// same segment different lane
		time = (short)530;
		streamIdsToBeMocked.add(TopologyControl.POSITION_REPORTS_STREAM_ID);
		valueStub = valueStub.thenReturn(new PositionReport(time, vid, spd, xway, l1, d, (short)30, pos));
		
		streamIdsToBeMocked.add(TopologyControl.CAR_COUNTS_STREAM_ID);
		valueStub = valueStub.thenReturn(new CountTuple((short)9, xway, (short)31, d, 52));
		streamIdsToBeMocked.add(TopologyControl.LAVS_STREAM_ID);
		valueStub = valueStub.thenReturn(new LavTuple((short)10, xway, (short)31, d, 39));
		streamIdsToBeMocked.add(TopologyControl.ACCIDENTS_STREAM_ID);
		valueStub = valueStub.thenReturn(new AccidentTuple((short)9, xway, (short)31, d));
		
		// crossing segment same lane (accident same segment -> no toll)
		time = (short)560;
		streamIdsToBeMocked.add(TopologyControl.POSITION_REPORTS_STREAM_ID);
		valueStub = valueStub.thenReturn(new PositionReport(time, vid, spd, xway, l1, d, (short)31, pos));
		expectedNotifications.add(new TollNotification(time, time, vid, 39, 0));
		expectedAssessments.add(new TollNotification((short)500, (short)500, vid, 0, 2));
		
		// crossing segment but exit highway
		time = (short)590;
		streamIdsToBeMocked.add(TopologyControl.POSITION_REPORTS_STREAM_ID);
		valueStub = valueStub.thenReturn(new PositionReport(time, vid, spd, xway, l4, d, (short)32, pos));
		
		
		
		// test 4: full and slow highway but accident (segment most far down stream)
		
		streamIdsToBeMocked.add(TopologyControl.CAR_COUNTS_STREAM_ID);
		valueStub = valueStub.thenReturn(new CountTuple((short)10, xway, (short)30, d, 51));
		
		// entering highway again (no accident yet -> toll)
		time = (short)600;
		streamIdsToBeMocked.add(TopologyControl.POSITION_REPORTS_STREAM_ID);
		valueStub = valueStub.thenReturn(new PositionReport(time, vid, spd, xway, l0, d, (short)30, pos));
		expectedNotifications.add(new TollNotification(time, time, vid, 0, 2));
		
		// same segment different lane
		time = (short)630;
		streamIdsToBeMocked.add(TopologyControl.POSITION_REPORTS_STREAM_ID);
		valueStub = valueStub.thenReturn(new PositionReport(time, vid, spd, xway, l1, d, (short)30, pos));
		
		streamIdsToBeMocked.add(TopologyControl.CAR_COUNTS_STREAM_ID);
		valueStub = valueStub.thenReturn(new CountTuple((short)11, xway, (short)31, d, 52));
		streamIdsToBeMocked.add(TopologyControl.LAVS_STREAM_ID);
		valueStub = valueStub.thenReturn(new LavTuple((short)12, xway, (short)31, d, 39));
		streamIdsToBeMocked.add(TopologyControl.ACCIDENTS_STREAM_ID);
		valueStub = valueStub.thenReturn(new AccidentTuple((short)11, xway, (short)35, d));
		
		// crossing segment same lane (accident 4 segments down -> no toll)
		time = (short)660;
		streamIdsToBeMocked.add(TopologyControl.POSITION_REPORTS_STREAM_ID);
		valueStub = valueStub.thenReturn(new PositionReport(time, vid, spd, xway, l1, d, (short)31, pos));
		expectedNotifications.add(new TollNotification(time, time, vid, 39, 0));
		expectedAssessments.add(new TollNotification((short)600, (short)600, vid, 0, 2));
		
		// crossing segment but exit highway
		time = (short)690;
		streamIdsToBeMocked.add(TopologyControl.POSITION_REPORTS_STREAM_ID);
		valueStub = valueStub.thenReturn(new PositionReport(time, vid, spd, xway, l4, d, (short)32, pos));
		
		valueStub.thenReturn(null);
		
		
		OngoingStubbing<String> streamStub = when(tuple.getSourceStreamId());
		for(String streamId : streamIdsToBeMocked) {
			streamStub = streamStub.thenReturn(streamId);
		}
		streamStub.thenReturn(null);
		
		TollNotificationBolt bolt = new TollNotificationBolt();
		TestOutputCollector collector = new TestOutputCollector();
		bolt.prepare(null, null, new OutputCollector(collector));
		
		for(int i = 0; i < streamIdsToBeMocked.size(); ++i) {
			bolt.execute(tuple);
		}
		
		Assert.assertEquals(2, collector.output.size());
		Assert.assertEquals(expectedNotifications, collector.output.get(TopologyControl.TOLL_NOTIFICATIONS_STREAM_ID));
		Assert.assertEquals(expectedAssessments, collector.output.get(TopologyControl.TOLL_ASSESSMENTS_STREAM_ID));
	}
	
	@Test
	public void testInputStreams() {
		TollNotificationBolt bolt = new TollNotificationBolt();
		
		String[] streamIds = new String[] {TopologyControl.POSITION_REPORTS_STREAM_ID,
			TopologyControl.ACCIDENTS_STREAM_ID, TopologyControl.CAR_COUNTS_STREAM_ID, TopologyControl.LAVS_STREAM_ID,
			"invalidStreamId"};
		
		Tuple tuple = mock(Tuple.class);
		OngoingStubbing<String> stub = when(tuple.getSourceStreamId());
		for(int i = 0; i < streamIds.length; ++i) {
			stub = stub.thenReturn(streamIds[i]);
		}
		stub.thenReturn(null);
		
		// first 4 calls should throw due to invalid tuple, but no due to invalid stream id
		for(int i = 0; i < streamIds.length - 1; ++i) {
			try {
				bolt.execute(tuple);
				fail();
			} catch(RuntimeException e) {
				if(e.getMessage().startsWith("Unknown input stream: '" + streamIds[i] + "' for tuple ")) {
					e.printStackTrace();
					fail();
				}
			}
		}
		
		// last call should throw due to invalid stream id
		try {
			bolt.execute(tuple);
			fail();
		} catch(RuntimeException e) {
			if(!e.getMessage().startsWith("Unknown input stream: 'invalidStreamId' for tuple ")) {
				e.printStackTrace();
				fail();
			}
		}
	}
	
	@Test
	public void testDeclareOutputFields() {
		TollNotificationBolt bolt = new TollNotificationBolt();
		
		TestDeclarer declarer = new TestDeclarer();
		bolt.declareOutputFields(declarer);
		
		final int numberOfOutputStreams = 2;
		
		Assert.assertEquals(numberOfOutputStreams, declarer.streamIdBuffer.size());
		Assert.assertEquals(numberOfOutputStreams, declarer.schemaBuffer.size());
		Assert.assertEquals(numberOfOutputStreams, declarer.directBuffer.size());
		
		Fields schema = new Fields(TopologyControl.TYPE_FIELD_NAME, TopologyControl.TIME_FIELD_NAME,
			TopologyControl.EMIT_FIELD_NAME, TopologyControl.VEHICLE_ID_FIELD_NAME, TopologyControl.SPEED_FIELD_NAME,
			TopologyControl.TOLL_FIELD_NAME);
		
		HashSet<String> expectedStreams = new HashSet<String>();
		expectedStreams.add(TopologyControl.TOLL_NOTIFICATIONS_STREAM_ID);
		expectedStreams.add(TopologyControl.TOLL_ASSESSMENTS_STREAM_ID);
		
		Assert.assertEquals(expectedStreams, new HashSet<String>(declarer.streamIdBuffer));
		
		for(int i = 0; i < numberOfOutputStreams; ++i) {
			Assert.assertEquals(schema.toList(), declarer.schemaBuffer.get(i).toList());
			Assert.assertEquals(new Boolean(false), declarer.directBuffer.get(i));
		}
	}
	
}
