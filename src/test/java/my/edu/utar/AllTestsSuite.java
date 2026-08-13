package my.edu.utar;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Convenience suite that runs the unit tests first and the integration tests
 * afterwards, in the order required by assessment component C.5.
 */
@RunWith(value = Suite.class)
@SuiteClasses(value = {
		RegressionUnitTestSuite.class,
		IntegrationTestSuite.class })
public class AllTestsSuite {

}
