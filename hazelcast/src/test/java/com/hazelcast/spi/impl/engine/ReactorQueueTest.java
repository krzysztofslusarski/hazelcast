package com.hazelcast.spi.impl.engine;

import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.annotation.ParallelJVMTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(HazelcastParallelClassRunner.class)
@Category({QuickTest.class, ParallelJVMTest.class})
public class ReactorQueueTest {

    @Test
    public void test(){
        ReactorQueue reactorQueue = new ReactorQueue(16);

        assertNull(reactorQueue.poll());

        assertFalse(reactorQueue.addAndMarkedBlocked("1"));
        assertEquals("1", reactorQueue.poll());
        assertTrue(reactorQueue.commitAndMarkBlocked());

        assertTrue(reactorQueue.addAndMarkedBlocked("2"));
        assertFalse(reactorQueue.addAndMarkedBlocked("3"));
        assertEquals("2", reactorQueue.poll());
        assertEquals("3", reactorQueue.poll());
        assertFalse(reactorQueue.addAndMarkedBlocked("4"));
        assertFalse(reactorQueue.commitAndMarkBlocked());
        assertEquals("4",reactorQueue.poll());
        assertTrue(reactorQueue.commitAndMarkBlocked());
    }
}
