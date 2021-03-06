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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.storm.guava.collect.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.OngoingStubbing;

import backtype.storm.task.OutputCollector;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;
import de.hub.cs.dbis.aeolus.testUtils.TestDeclarer;
import de.hub.cs.dbis.aeolus.testUtils.TestOutputCollector;
import de.hub.cs.dbis.aeolus.utils.TimestampMerger;
import de.hub.cs.dbis.lrb.queries.utils.TopologyControl;
import de.hub.cs.dbis.lrb.types.internal.AvgSpeedTuple;
import de.hub.cs.dbis.lrb.types.internal.AvgVehicleSpeedTuple;
import de.hub.cs.dbis.lrb.util.Constants;





/**
 * @author mjsax
 */
public class AverageSpeedBoltTest {
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
		TestOutputCollector collector = new TestOutputCollector();
		collector.output.put(Utils.DEFAULT_STREAM_ID, new LinkedList<List<Object>>());
		
		AverageSpeedBolt bolt = new AverageSpeedBolt();
		bolt.prepare(null, null, new OutputCollector(collector));
		
		final Tuple tuple = mock(Tuple.class);
		when(tuple.getSourceStreamId()).thenReturn("streamId");
		OngoingStubbing<List<Object>> tupleStub = when(tuple.getValues());
		
		final int numberOfVehicles = 1 + this.r.nextInt(50);
		
		Integer[] vids = new Integer[numberOfVehicles];
		for(int i = 0; i < numberOfVehicles; ++i) {
			vids[i] = new Integer(this.r.nextInt(Integer.MAX_VALUE));
			
			// avoid duplicates
			for(int j = 0; j < i; ++j) {
				if(vids[i].intValue() == vids[j].intValue()) {
					// duplicate found, try again
					--i;
					break;
				}
			}
		}
		
		Short[] segment = new Short[numberOfVehicles];
		for(int i = 0; i < numberOfVehicles; ++i) {
			segment[i] = new Short((short)(Constants.MAX_SEGMENT / 3 + this.r.nextInt(Constants.MAX_SEGMENT / 3)));
		}
		
		final int numberOfMinutes = 2 + this.r.nextInt(8);
		@SuppressWarnings("unchecked")
		final HashMap<Short, List<Integer>>[] speedValuesPerSegment1 = new HashMap[numberOfMinutes];
		@SuppressWarnings("unchecked")
		final HashMap<Short, List<Integer>>[] speedValuesPerSegment2 = new HashMap[numberOfMinutes];
		
		for(short minute = 1; minute <= numberOfMinutes; ++minute) {
			speedValuesPerSegment1[minute - 1] = new HashMap<Short, List<Integer>>();
			speedValuesPerSegment2[minute - 1] = new HashMap<Short, List<Integer>>();
			
			// randomize vehicle order for each minute
			LinkedList<Integer> index = new LinkedList<Integer>();
			for(int i = 0; i < numberOfVehicles; ++i) {
				index.add(new Integer(i));
			}
			
			while(index.size() > 0) {
				int idx = index.remove(this.r.nextInt(index.size())).intValue();
				int speed = this.r.nextInt(Constants.MAX_SPEED);
				
				if(idx < numberOfVehicles / 2) {
					tupleStub = tupleStub.thenReturn(new AvgVehicleSpeedTuple(vids[idx], new Short(minute),
						new Integer(1), segment[idx], new Short((short)0), new Double(speed)));
					
					this.addToSpeedList(speedValuesPerSegment1[minute - 1], segment[idx], speed);
					if(this.r.nextInt(5) > 0) {
						segment[idx] = new Short((short)(segment[idx].shortValue() + 1));
					}
				} else {
					tupleStub = tupleStub.thenReturn(new AvgVehicleSpeedTuple(vids[idx], new Short(minute),
						new Integer(1), segment[idx], new Short((short)1), new Double(speed)));
					
					this.addToSpeedList(speedValuesPerSegment2[minute - 1], segment[idx], speed);
					if(this.r.nextInt(5) > 0) {
						segment[idx] = new Short((short)(segment[idx].shortValue() - 1));
					}
				}
			}
			
		}
		
		
		
		final HashSet<AvgSpeedTuple> expectedResult = new HashSet<AvgSpeedTuple>();
		final LinkedList<Values> expectedFlushs = new LinkedList<Values>();
		
		for(short minute = 1; minute <= numberOfMinutes; ++minute) {
			for(int i = 0; i < numberOfVehicles; ++i) {
				bolt.execute(tuple);
			}
			expectedFlushs.add(new Values(new Short(minute)));
			
			Assert.assertEquals(2, collector.output.size());
			Assert.assertEquals(expectedResult, Sets.newHashSet(collector.output.get(Utils.DEFAULT_STREAM_ID)));
			Assert.assertEquals(expectedFlushs, collector.output.get(TimestampMerger.FLUSH_STREAM_ID));
			
			Assert.assertEquals(minute * numberOfVehicles, collector.acked.size());
			Assert.assertEquals(0, collector.failed.size());
			
			collector.output.clear();
			collector.output.put(Utils.DEFAULT_STREAM_ID, new LinkedList<List<Object>>());
			expectedResult.clear();
			expectedFlushs.clear();
			this.addToExpectedResult(speedValuesPerSegment1[minute - 1], minute, (short)0, expectedResult);
			this.addToExpectedResult(speedValuesPerSegment2[minute - 1], minute, (short)1, expectedResult);
		}
		
		Assert.assertNull(collector.output.get(TimestampMerger.FLUSH_STREAM_ID));
		
		Tuple flushTuple = mock(Tuple.class);
		when(flushTuple.getSourceStreamId()).thenReturn(TimestampMerger.FLUSH_STREAM_ID);
		bolt.execute(flushTuple);
		
		Assert.assertEquals(2, collector.output.size());
		
		Assert.assertEquals(expectedResult, Sets.newHashSet(collector.output.get(Utils.DEFAULT_STREAM_ID)));
		Assert.assertEquals(numberOfMinutes * numberOfVehicles + 1, collector.acked.size());
		
		Assert.assertEquals(1, collector.output.get(TimestampMerger.FLUSH_STREAM_ID).size());
		Assert.assertEquals(new Values((Object)null), collector.output.get(TimestampMerger.FLUSH_STREAM_ID).get(0));
		
		Assert.assertEquals(0, collector.failed.size());
	}
	
	private void addToSpeedList(HashMap<Short, List<Integer>> speedValuesPerSegment, Short seg, int speed) {
		List<Integer> s = speedValuesPerSegment.get(seg);
		if(s == null) {
			s = new LinkedList<Integer>();
			speedValuesPerSegment.put(seg, s);
		}
		s.add(new Integer(speed));
	}
	
	private void addToExpectedResult(HashMap<Short, List<Integer>> speedValuesPerSegment, short minute, short direction, HashSet<AvgSpeedTuple> expectedResult) {
		for(Entry<Short, List<Integer>> segment : speedValuesPerSegment.entrySet()) {
			int sumSpeed = 0;
			int cnt = 0;
			
			for(Integer speed : segment.getValue()) {
				sumSpeed += speed.intValue();
				++cnt;
			}
			expectedResult.add(new AvgSpeedTuple(new Short(minute), new Integer(1), segment.getKey(), new Short(
				direction), new Double(sumSpeed / (double)cnt)));
		}
		
		speedValuesPerSegment.clear();
	}
	
	@Test
	public void testDeclareOutputFields() {
		AverageSpeedBolt bolt = new AverageSpeedBolt();
		
		TestDeclarer declarer = new TestDeclarer();
		bolt.declareOutputFields(declarer);
		
		Assert.assertEquals(2, declarer.streamIdBuffer.size());
		Assert.assertEquals(2, declarer.schemaBuffer.size());
		Assert.assertEquals(2, declarer.directBuffer.size());
		
		Assert.assertNull(declarer.streamIdBuffer.get(0));
		Assert.assertEquals(new Fields(TopologyControl.MINUTE_FIELD_NAME, TopologyControl.XWAY_FIELD_NAME,
			TopologyControl.SEGMENT_FIELD_NAME, TopologyControl.DIRECTION_FIELD_NAME,
			TopologyControl.AVERAGE_SPEED_FIELD_NAME).toList(), declarer.schemaBuffer.get(0).toList());
		Assert.assertEquals(new Boolean(false), declarer.directBuffer.get(0));
		
		Assert.assertEquals(TimestampMerger.FLUSH_STREAM_ID, declarer.streamIdBuffer.get(1));
		Assert.assertEquals(new Fields("ts").toList(), declarer.schemaBuffer.get(1).toList());
		Assert.assertEquals(new Boolean(false), declarer.directBuffer.get(1));
	}
	
}
