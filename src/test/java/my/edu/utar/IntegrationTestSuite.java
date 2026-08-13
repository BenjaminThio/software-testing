package my.edu.utar;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Integration test suite.
 *
 * Run this suite ONLY after {@link RegressionUnitTestSuite} has passed. Every module
 * involved has its own unit tests, so a failure reported here points at the
 * interaction between two modules rather than at the behaviour of any single one.
 */
@RunWith(value = Suite.class)
@SuiteClasses(value = { PrintingServiceIntegrationTests.class })
public class IntegrationTestSuite {

}
