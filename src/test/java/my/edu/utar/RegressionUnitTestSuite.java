package my.edu.utar;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Regression suite holding every UNIT test in the project.
 *
 * The suite is run first, and only once it passes are the integration tests run
 * through {@link IntegrationTestSuite}. This ordering is the one required by
 * assessment component C.5: integration testing is performed AFTER the individual
 * units have been verified.
 *
 * The classes are listed in dependency order, so the module that everything else
 * builds on is verified first:
 *
 *   1. Customer                  - the data held about a customer
 *   2. ReadCustomer              - reading those records from customer.txt
 *   3. AddNewCustomer            - writing new records to customer.txt
 *   4. PrintOrder                - the order and its business rules
 *   5. ApplyDiscount             - Table 4 discount rules
 *   6. CalculatePrintingCharge   - Table 2 and Table 3 charge calculations
 *   7. GenerateInvoice           - the invoice built from a priced order
 *   8. CalculatePrintingCharge   - the collaborations, using test doubles
 *   9. Payment and EmailInvoice  - the external modules, using test doubles
 */
@RunWith(value = Suite.class)
@SuiteClasses(value = {
		CustomerUnitTests.class,
		ReadCustomerUnitTests.class,
		AddNewCustomerUnitTests.class,
		PrintOrderUnitTests.class,
		ApplyDiscountUnitTests.class,
		CalculatePrintingChargeUnitTests.class,
		GenerateInvoiceUnitTests.class,
		CalculatePrintingChargeTestDoubleTests.class,
		PaymentAndEmailInvoiceTestDoubleTests.class })
public class RegressionUnitTestSuite {

}
