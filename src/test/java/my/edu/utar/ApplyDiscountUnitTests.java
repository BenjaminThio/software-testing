package my.edu.utar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.FileParameters;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.mappers.CsvWithHeaderMapper;

/**
 * Unit tests for ApplyDiscount (FR5 - Discounts, Table 4).
 *
 * Test design techniques applied in this class:
 *   - DECISION TABLE. Table 4 has three independent conditions - the customer type,
 *     whether the subtotal exceeds RM300 and whether the customer has more than 20
 *     previous orders - and the discounts are cumulative. The parameterised tests
 *     below walk the rules of the decision table recorded in
 *     UECS2354_GroupNumber_01_DecisionTableEPBVA.xlsx, sheet "Decision Table #2".
 *   - BOUNDARY VALUE ANALYSIS on the RM300 subtotal threshold (299.99 / 300.00 /
 *     300.01) and on the 20 previous order threshold (19 / 20 / 21), because both
 *     conditions are written as strict inequalities in the specification.
 *   - Equivalence Partitioning on the subtotal (negative, zero, positive).
 *   - The discount test values are READ FROM A TEXT FILE (discount-test-data.csv).
 */
@RunWith(JUnitParamsRunner.class)
public class ApplyDiscountUnitTests {

	private static final double DELTA = 0.001;

	private ApplyDiscount applyDiscount;

	@Before
	public void setupForAllTests() {
		applyDiscount = new ApplyDiscount();
	}

	// ----------------------------------------------------------------- VALID CASES

	/**
	 * VALID CASE - parameterised, test data read from an external text file.
	 * Each row of discount-test-data.csv is one rule of the decision table for Table 4.
	 */
	@Test
	@FileParameters(value = "discount-test-data.csv", mapper = CsvWithHeaderMapper.class)
	public void testCalculateDiscountFollowsTable4(String customerType, double subtotal,
			int previousOrderCount, double expectedDiscount) {

		assertEquals(expectedDiscount, applyDiscount.calculateDiscount(
				CustomerType.fromString(customerType), subtotal, previousOrderCount), DELTA);
	}

	/**
	 * VALID CASE - DECISION TABLE walkthrough.
	 * Every rule is listed explicitly so the mapping between the spreadsheet and the
	 * code is obvious. The columns are: customer type, subtotal, previous order count,
	 * expected discount amount, expected amount still payable.
	 *
	 * Rules R1 to R4  - Regular customer, with and without the two additional discounts
	 * Rules R5 to R8  - Student customer, with and without the two additional discounts
	 * Rules R9 to R12 - Corporate customer, with and without the two additional discounts
	 */
	@Test
	@Parameters({
			// R1  Regular, subtotal not over 300, 20 or fewer previous orders : no discount
			"Regular,   200.00,  5,   0.00,   200.00",
			// R2  Regular, subtotal over 300 : additional 5%
			"Regular,   400.00,  5,  20.00,   380.00",
			// R3  Regular, more than 20 previous orders : additional 5%
			"Regular,   200.00, 25,  10.00,   190.00",
			// R4  Regular, both additional discounts, applied sequentially
			"Regular,   400.00, 25,  39.00,   361.00",
			// R5  Student, neither additional discount : 10%
			"Student,   200.00,  5,  20.00,   180.00",
			// R6  Student, subtotal over 300 : 10% then a further 5%
			"Student,   400.00,  5,  58.00,   342.00",
			// R7  Student, more than 20 previous orders : 10% then a further 5%
			"Student,   200.00, 25,  29.00,   171.00",
			// R8  Student, all three discounts
			"Student,   400.00, 25,  75.10,   324.90",
			// R9  Corporate, neither additional discount : 15%
			"Corporate, 200.00,  5,  30.00,   170.00",
			// R10 Corporate, subtotal over 300 : 15% then a further 5%
			"Corporate, 400.00,  5,  77.00,   323.00",
			// R11 Corporate, more than 20 previous orders : 15% then a further 5%
			"Corporate, 200.00, 25,  38.50,   161.50",
			// R12 Corporate, all three discounts
			"Corporate, 400.00, 25,  93.15,   306.85" })
	public void testDecisionTableForTable4(String customerType, double subtotal,
			int previousOrderCount, double expectedDiscount, double expectedAmountPayable) {

		CustomerType type = CustomerType.fromString(customerType);

		assertEquals("discount amount", expectedDiscount,
				applyDiscount.calculateDiscount(type, subtotal, previousOrderCount), DELTA);
		assertEquals("amount still payable", expectedAmountPayable,
				applyDiscount.getAmountAfterDiscount(type, subtotal, previousOrderCount), DELTA);
	}

	/**
	 * VALID CASE - BOUNDARY VALUE ANALYSIS on the RM300 subtotal threshold.
	 * Table 4 says the discount applies when the subtotal EXCEEDS RM300, so RM300.00
	 * itself must NOT attract the additional 5% while RM300.01 must.
	 */
	@Test
	@Parameters({ "299.99, false",
			"300.00, false",
			"300.01, true",
			"301.00, true",
			"0.00,   false" })
	public void testLargeOrderDiscountBoundary(double subtotal, boolean expectedToBeEligible) {

		assertEquals(expectedToBeEligible, applyDiscount.isEligibleForLargeOrderDiscount(subtotal));
	}

	/**
	 * VALID CASE - BOUNDARY VALUE ANALYSIS on the RM300 threshold, verified through the
	 * calculated discount rather than through the eligibility flag alone.
	 * A regular customer earns nothing at RM300.00 but 5% at RM300.01.
	 */
	@Test
	@Parameters({ "299.99,  0.00",
			"300.00,  0.00",
			"300.01, 15.00",
			"400.00, 20.00" })
	public void testLargeOrderDiscountBoundaryAffectsTheAmount(double subtotal,
			double expectedDiscount) {

		assertEquals(expectedDiscount, applyDiscount.calculateDiscount(CustomerType.REGULAR,
				subtotal, 0), DELTA);
	}

	/**
	 * VALID CASE - BOUNDARY VALUE ANALYSIS on the 20 previous order threshold.
	 * Table 4 says MORE THAN 20 previous orders, so 20 must NOT be eligible and 21 must.
	 */
	@Test
	@Parameters({ "0,  false",
			"19, false",
			"20, false",
			"21, true",
			"50, true" })
	public void testLoyalCustomerDiscountBoundary(int previousOrderCount,
			boolean expectedToBeEligible) {

		assertEquals(expectedToBeEligible,
				applyDiscount.isEligibleForLoyalCustomerDiscount(previousOrderCount));
	}

	/**
	 * VALID CASE - BOUNDARY VALUE ANALYSIS on the 20 previous order threshold, verified
	 * through the calculated discount.
	 */
	@Test
	@Parameters({ "19,  0.00",
			"20,  0.00",
			"21, 10.00",
			"45, 10.00" })
	public void testLoyalCustomerDiscountBoundaryAffectsTheAmount(int previousOrderCount,
			double expectedDiscount) {

		assertEquals(expectedDiscount, applyDiscount.calculateDiscount(CustomerType.REGULAR,
				200.00, previousOrderCount), DELTA);
	}

	/**
	 * VALID CASE - the discounts are cumulative and applied SEQUENTIALLY, not summed.
	 * A corporate customer with more than 20 previous orders on a subtotal over RM300
	 * pays 450.00 x 0.85 x 0.95 x 0.95 = RM345.21, NOT 450.00 x (1 - 0.25) = RM337.50.
	 * This test exists specifically to pin down that interpretation.
	 */
	@Test
	public void testDiscountsAreAppliedSequentiallyAndNotSummed() {

		double sequentialResult = applyDiscount.getAmountAfterDiscount(CustomerType.CORPORATE,
				450.00, 25);
		double summedResult = CalculatePrintingCharge.roundToTwoDecimals(450.00 * (1 - 0.25));

		assertEquals(345.21, sequentialResult, DELTA);
		assertEquals(337.50, summedResult, DELTA);
		org.junit.Assert.assertNotEquals(summedResult, sequentialResult, DELTA);
	}

	/**
	 * VALID CASE - the customer based overload reads the type and the previous order
	 * count straight off the Customer object and produces the same answer as the raw
	 * overload.
	 */
	@Test
	@Parameters({ "Student,   3,  298.00,  29.80",
			"Corporate, 25, 450.00, 104.79",
			"Regular,   12, 100.00,   0.00",
			"Student,  21,  400.00,  75.10" })
	public void testCalculateDiscountFromACustomerObject(String customerType,
			int previousOrderCount, double subtotal, double expectedDiscount) {

		Customer customer = new Customer("C0001", "Valid Name", "valid@example.com", "0123334444",
				CustomerType.fromString(customerType), previousOrderCount);

		assertEquals(expectedDiscount, applyDiscount.calculateDiscount(customer, subtotal), DELTA);
	}

	/**
	 * VALID CASE - a subtotal of exactly zero is legal and attracts no discount.
	 * This is the lower boundary of the valid subtotal partition.
	 */
	@Test
	@Parameters({ "Regular", "Student", "Corporate" })
	public void testZeroSubtotalAttractsNoDiscount(String customerType) {

		assertEquals(0.00, applyDiscount.calculateDiscount(CustomerType.fromString(customerType),
				0.00, 0), DELTA);
	}

	/**
	 * VALID CASE - the discount rates held in the code match Table 4 exactly.
	 */
	@Test
	public void testDiscountRatesMatchTable4() {

		assertEquals(0.10, ApplyDiscount.STUDENT_DISCOUNT_RATE, DELTA);
		assertEquals(0.15, ApplyDiscount.CORPORATE_DISCOUNT_RATE, DELTA);
		assertEquals(0.05, ApplyDiscount.LARGE_ORDER_DISCOUNT_RATE, DELTA);
		assertEquals(0.05, ApplyDiscount.LOYAL_CUSTOMER_DISCOUNT_RATE, DELTA);
		assertEquals(300.00, ApplyDiscount.LARGE_ORDER_THRESHOLD, DELTA);
		assertEquals(20, ApplyDiscount.LOYAL_CUSTOMER_MIN_ORDERS);

		assertTrue(applyDiscount.isEligibleForLargeOrderDiscount(300.01));
		assertFalse(applyDiscount.isEligibleForLargeOrderDiscount(300.00));
	}

	// --------------------------------------------------------------- INVALID CASES

	/**
	 * INVALID CASE - Equivalence Partitioning on the subtotal. Any negative subtotal
	 * lies in the invalid partition and is rejected, with -0.01 being the boundary
	 * value immediately below zero.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters({ "-0.01", "-1.00", "-100.00", "-99999.99" })
	public void testNegativeSubtotalIsRejected(double subtotal) {

		applyDiscount.calculateDiscount(CustomerType.STUDENT, subtotal, 0);
	}

	/**
	 * INVALID CASE - a negative previous order count is rejected, with -1 being the
	 * boundary value immediately below zero.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters({ "-1", "-20", "-1000" })
	public void testNegativePreviousOrderCountIsRejected(int previousOrderCount) {

		applyDiscount.calculateDiscount(CustomerType.REGULAR, 100.00, previousOrderCount);
	}

	/**
	 * INVALID CASE - a null customer type cannot be priced.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters(method = "getNullCustomerType")
	public void testNullCustomerTypeIsRejected(CustomerType customerType) {

		applyDiscount.calculateDiscount(customerType, 100.00, 0);
	}

	private Object[] getNullCustomerType() {
		return new Object[] { new Object[] { (CustomerType) null } };
	}

	/**
	 * INVALID CASE - a null customer cannot be priced either, and the message says so.
	 */
	@Test
	public void testNullCustomerIsRejected() {

		try {
			applyDiscount.calculateDiscount((Customer) null, 100.00);
			org.junit.Assert.fail("An IllegalArgumentException was expected for a null customer");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("Customer must not be null"));
		}
	}

	/**
	 * INVALID CASE - the invalid inputs are also rejected by the amount payable
	 * overload, so no caller can slip past the validation.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters({ "-1.00, 0", "100.00, -1", "-50.00, -5" })
	public void testGetAmountAfterDiscountRejectsInvalidInput(double subtotal,
			int previousOrderCount) {

		applyDiscount.getAmountAfterDiscount(CustomerType.STUDENT, subtotal, previousOrderCount);
	}
}
