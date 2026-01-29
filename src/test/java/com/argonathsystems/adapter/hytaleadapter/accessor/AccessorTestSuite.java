package com.argonathsystems.adapter.hytaleadapter.accessor;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Test suite for all accessor implementations.
 * 
 * <p>Runs comprehensive tests for:
 * <ul>
 *   <li>HytalePlayerAccessor - Player data and operations</li>
 *   <li>HytaleItemAccessor - Item registry and creation</li>
 *   <li>HytaleSchedulerAccessor - Task scheduling</li>
 *   <li>HytaleEventAccessor - Event registration and emission</li>
 * </ul>
 * 
 * @author Argonath Systems Team
 * @version 2.0.0
 */
@Suite
@SelectClasses({
    HytalePlayerAccessorTest.class,
    HytaleItemAccessorTest.class,
    HytaleSchedulerAccessorTest.class,
    HytaleEventAccessorTest.class
})
public class AccessorTestSuite {
}
