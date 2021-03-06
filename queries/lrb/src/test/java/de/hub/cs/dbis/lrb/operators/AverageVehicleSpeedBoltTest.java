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
package de.hub.cs.dbis.lrb.operators;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Random;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import storm.lrb.tools.EntityHelper;
import backtype.storm.task.OutputCollector;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;
import de.hub.cs.dbis.aeolus.testUtils.TestDeclarer;
import de.hub.cs.dbis.aeolus.testUtils.TestOutputCollector;
import de.hub.cs.dbis.aeolus.utils.TimestampMerger;
import de.hub.cs.dbis.lrb.queries.utils.TopologyControl;
import de.hub.cs.dbis.lrb.types.PositionReport;
import de.hub.cs.dbis.lrb.types.internal.AvgVehicleSpeedTuple;
import de.hub.cs.dbis.lrb.types.util.SegmentIdentifier;
import de.hub.cs.dbis.lrb.util.Constants;
import de.hub.cs.dbis.lrb.util.Time;





/**
 * @author richter
 * @author mjsax
 */
public class AverageVehicleSpeedBoltTest {
	private long seed;
	private Random r;
	
	
	
	@Before
	public void prepare() {
		this.seed = System.currentTimeMillis();
		this.r = new Random(this.seed);
		System.out.println("Test seed: " + this.seed);
	}
	
	
	
	@Test
	public void testExecute() {
		AverageVehicleSpeedBolt bolt = new AverageVehicleSpeedBolt();
		TestOutputCollector collector = new TestOutputCollector();
		bolt.prepare(null, null, new OutputCollector(collector));
		
		final Tuple tuple = mock(Tuple.class);
		
		
		
		int vehicleID0, vehicleID1, vehicleID2;
		vehicleID0 = (int)(this.r.nextDouble() * Constants.NUMBER_OF_VIDS);
		do {
			vehicleID1 = (int)(this.r.nextDouble() * Constants.NUMBER_OF_VIDS);
		} while(vehicleID1 == vehicleID0);
		do {
			vehicleID2 = (int)(this.r.nextDouble() * Constants.NUMBER_OF_VIDS);
		} while(vehicleID2 == vehicleID1 || vehicleID2 == vehicleID0);
		
		short segment = (short)(this.r.nextDouble() * Constants.NUMBER_OF_SEGMENT);
		
		
		
		short time0 = (short)this.r.nextInt(60);
		PositionReport posReport0Stopped = EntityHelper.createPosReport(new Short(time0), new Short(segment), this.r,
			new Integer(vehicleID0), 0, // minSpeed
			0 // maxSpeed
			);
		when(tuple.getValues()).thenReturn(posReport0Stopped);
		when(tuple.getSourceStreamId()).thenReturn("streamId");
		
		bolt.execute(tuple);
		
		assertEquals(1, collector.acked.size());
		assertEquals(0, collector.output.size()); // one tuple doesn't trigger emission
		
		
		
		short time1 = (short)(time0 + 60); // step to next minute
		PositionReport posReport1Stopped = EntityHelper.createPosReport(new Short(time1), new Short(segment), this.r,
			new Integer(vehicleID0), 0, // minSpeed
			0 // maxSpeed
			);
		when(tuple.getValues()).thenReturn(posReport1Stopped);
		when(tuple.getSourceStreamId()).thenReturn("streamId");
		
		bolt.execute(tuple);
		
		assertEquals(2, collector.acked.size());
		// second tuple with another time should have triggered emission after one minute
		assertEquals(1, collector.output.get(Utils.DEFAULT_STREAM_ID).size());
		
		AvgVehicleSpeedTuple result = (AvgVehicleSpeedTuple)collector.output.get(Utils.DEFAULT_STREAM_ID).get(0);
		assertEquals(new SegmentIdentifier(posReport1Stopped), new SegmentIdentifier(result));
		assertEquals(time1 / 60, result.getMinute().shortValue());
		assertEquals(0.0, result.getAvgSpeed().doubleValue(), 0.0);
		
		assertEquals(1, collector.output.get(TimestampMerger.FLUSH_STREAM_ID).size());
		assertEquals(new Values(new Short(Time.getMinute(time1))), collector.output
			.get(TimestampMerger.FLUSH_STREAM_ID).get(0));
		
		// three cars
		int speed2 = this.r.nextInt(Constants.NUMBER_OF_SPEEDS);
		int speed3 = this.r.nextInt(Constants.NUMBER_OF_SPEEDS);
		int speed4 = this.r.nextInt(Constants.NUMBER_OF_SPEEDS);
		
		
		PositionReport posReport2Running = EntityHelper.createPosReport(new Short(time1), new Short(segment), this.r,
			new Integer(vehicleID1), speed2, // minSpeed
			speed2 // maxSpeed
			);
		when(tuple.getValues()).thenReturn(posReport2Running);
		when(tuple.getSourceStreamId()).thenReturn("streamId");
		
		bolt.execute(tuple);
		
		assertEquals(3, collector.acked.size());
		assertEquals(1, collector.output.get(Utils.DEFAULT_STREAM_ID).size());
		assertEquals(1, collector.output.get(TimestampMerger.FLUSH_STREAM_ID).size());
		
		
		PositionReport posReport3Running = EntityHelper.createPosReport(new Short(time1), new Short(segment), this.r,
			new Integer(vehicleID1), speed3, // minSpeed
			speed3 // maxSpeed
			);
		when(tuple.getValues()).thenReturn(posReport3Running);
		when(tuple.getSourceStreamId()).thenReturn("streamId");
		
		bolt.execute(tuple);
		
		assertEquals(4, collector.acked.size());
		assertEquals(1, collector.output.get(Utils.DEFAULT_STREAM_ID).size());
		assertEquals(1, collector.output.get(TimestampMerger.FLUSH_STREAM_ID).size());
		
		
		
		short time2 = (short)(time1 + 60); // step to next minute
		PositionReport posReport4Running = EntityHelper.createPosReport(new Short(time2), new Short(segment), this.r,
			new Integer(vehicleID2), speed4, // minSpeed
			speed4 // maxSpeed
			);
		when(tuple.getValues()).thenReturn(posReport4Running);
		when(tuple.getSourceStreamId()).thenReturn("streamId");
		
		bolt.execute(tuple);
		
		assertEquals(5, collector.acked.size());
		assertEquals(3, collector.output.get(Utils.DEFAULT_STREAM_ID).size());
		assertEquals(2, collector.output.get(TimestampMerger.FLUSH_STREAM_ID).size());
		assertEquals(new Values(new Short(Time.getMinute(time2))), collector.output
			.get(TimestampMerger.FLUSH_STREAM_ID).get(1));
		
		for(int i = 1; i < 3; ++i) {
			result = (AvgVehicleSpeedTuple)collector.output.get(Utils.DEFAULT_STREAM_ID).get(i);
			assertEquals(new SegmentIdentifier(posReport1Stopped), new SegmentIdentifier(result));
			// average update only occurs after emission
			if(result.getVid().intValue() == vehicleID0) {
				assertEquals(0.0, result.getAvgSpeed().doubleValue(), 0.0);
			} else {
				assertEquals(result.getVid().intValue(), vehicleID1);
				assertEquals((speed2 + speed3) / 2.0, result.getAvgSpeed().doubleValue(), 0.0);
			}
			assertEquals(time2 / 60, result.getMinute().shortValue());
		}
		
		Tuple flushTuple = mock(Tuple.class);
		when(flushTuple.getSourceStreamId()).thenReturn(TimestampMerger.FLUSH_STREAM_ID);
		bolt.execute(flushTuple);
		
		result = (AvgVehicleSpeedTuple)collector.output.get(Utils.DEFAULT_STREAM_ID).get(3);
		assertEquals(new SegmentIdentifier(posReport1Stopped), new SegmentIdentifier(result));
		assertEquals(result.getVid().intValue(), vehicleID2);
		assertEquals(speed4, result.getAvgSpeed().doubleValue(), 0.0);
		assertEquals((time2 + 60) / 60, result.getMinute().shortValue());
		
		assertEquals(3, collector.output.get(TimestampMerger.FLUSH_STREAM_ID).size());
		assertEquals(new Values((Object)null), collector.output.get(TimestampMerger.FLUSH_STREAM_ID).get(2));
		
		assertEquals(2, collector.output.size());
	}
	
	@Test
	public void testDeclareOutputFields() {
		AverageVehicleSpeedBolt bolt = new AverageVehicleSpeedBolt();
		
		TestDeclarer declarer = new TestDeclarer();
		bolt.declareOutputFields(declarer);
		
		Assert.assertEquals(2, declarer.streamIdBuffer.size());
		Assert.assertEquals(2, declarer.schemaBuffer.size());
		Assert.assertEquals(2, declarer.directBuffer.size());
		
		Assert.assertNull(declarer.streamIdBuffer.get(0));
		Assert.assertEquals(new Fields(TopologyControl.VEHICLE_ID_FIELD_NAME, TopologyControl.MINUTE_FIELD_NAME,
			TopologyControl.XWAY_FIELD_NAME, TopologyControl.SEGMENT_FIELD_NAME, TopologyControl.DIRECTION_FIELD_NAME,
			TopologyControl.AVERAGE_VEHICLE_SPEED_FIELD_NAME).toList(), declarer.schemaBuffer.get(0).toList());
		Assert.assertEquals(new Boolean(false), declarer.directBuffer.get(0));
		
		Assert.assertEquals(TimestampMerger.FLUSH_STREAM_ID, declarer.streamIdBuffer.get(1));
		Assert.assertEquals(new Fields("ts").toList(), declarer.schemaBuffer.get(1).toList());
		Assert.assertEquals(new Boolean(false), declarer.directBuffer.get(1));
	}
	
}
