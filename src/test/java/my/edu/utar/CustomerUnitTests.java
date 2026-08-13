package my.edu.utar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.FileParameters;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.mappers.CsvWithHeaderMapper;

/**
 * Unit tests for Customer (FR2 - Customer Details).
 *
 * Test design techniques applied in this class:
 *   - Equivalence Partitioning on customer ID, name, email address, phone number and
 *     customer type (valid partition and several invalid partitions per field);
 *   - Boundary Value Analysis on the name length (1 / 2 / 50 / 51 characters), on the
 *     phone number length (9 / 10 / 11 / 12 digits) and on the previous order count
 *     (-1 / 0);
 *   - Test values for the main valid and invalid partitions are READ FROM A TEXT FILE
 *     (customer-valid-test-data.csv and customer-invalid-test-data.csv) rather than
 *     being hardcoded, so new cases can be added without touching the test code.
 */
@RunWith(JUnitParamsRunner.class)
public class CustomerUnitTests {

	// ----------------------------------------------------------------- VALID CASES

	/**
	 * VALID CASE - parameterised, test data read from an external text file.
	 * Equivalence Partitioning : one representative from each valid partition of the
	 * name, email address, phone number and customer type fields.
	 *
	 * Verifies that a customer built from valid details keeps every value, and that a
	 * phone number typed with a hyphen is stored in its normalised, digits only form.
	 */
	@Test
	@FileParameters(value = "customer-valid-test-data.csv", mapper = CsvWithHeaderMapper.class)
	public void testCreateCustomerWithValidDetails(String name, String emailAddress,
			String phoneNumber, String customerType, String expectedStoredPhone) {

		Customer customer = new Customer("C0001", name, emailAddress, phoneNumber,
				CustomerType.fromString(customerType));

		assertEquals("C0001", customer.getCustomerId());
		assertEquals(name, customer.getName());
		assertEquals(emailAddress, customer.getEmailAddress());
		assertEquals(expectedStoredPhone, customer.getPhoneNumber());
		assertEquals(CustomerType.fromString(customerType), customer.getCustomerType());
		assertEquals(0, customer.getPreviousOrderCount());
	}

	/**
	 * VALID CASE - Boundary Value Analysis on the customer ID format.
	 * C0000 and C9999 are the lower and upper boundaries of the four digit scheme.
	 * A lower case id is accepted and normalised to upper case.
	 */
	@Test
	@Parameters({ "C0000, C0000",
			"C0001, C0001",
			"C9999, C9999",
			"c0007, C0007" })
	public void testValidCustomerIdIsAccepted(String customerId, String expectedStoredId) {

		Customer customer = new Customer(customerId, "Valid Name", "valid@example.com",
				"0123334444", CustomerType.REGULAR);

		assertEquals(expectedStoredId, customer.getCustomerId());
	}

	/**
	 * VALID CASE - Boundary Value Analysis on the name length.
	 * The valid partition is 2 to 50 characters, so 2 and 50 are the boundary values.
	 */
	@Test
	@Parameters(method = "getValidNameLengthBoundaries")
	public void testNameLengthBoundariesAreAccepted(String name, int expectedLength) {

		Customer customer = new Customer("C0001", name, "valid@example.com", "0123334444",
				CustomerType.STUDENT);

		assertEquals(expectedLength, customer.getName().length());
	}

	private Object[] getValidNameLengthBoundaries() {
		return new Object[] {
				new Object[] { buildName(2), 2 },    // lower boundary
				new Object[] { buildName(3), 3 },    // just inside the lower boundary
				new Object[] { buildName(49), 49 },  // just inside the upper boundary
				new Object[] { buildName(50), 50 }   // upper boundary
		};
	}

	/**
	 * VALID CASE - Boundary Value Analysis on the previous order count.
	 * The count feeds the Table 4 loyalty discount, so 0, 20 and 21 are all meaningful.
	 */
	@Test
	@Parameters({ "0", "1", "20", "21", "1000" })
	public void testValidPreviousOrderCountIsAccepted(int previousOrderCount) {

		Customer customer = new Customer("C0001", "Valid Name", "valid@example.com", "0123334444",
				CustomerType.REGULAR, previousOrderCount);

		assertEquals(previousOrderCount, customer.getPreviousOrderCount());
	}

	/**
	 * VALID CASE - verifies the record format written to customer.txt, which
	 * ReadCustomer must be able to read back.
	 */
	@Test
	public void testToFileRecordProducesTheStoredFormat() {

		Customer customer = new Customer("C0003", "Tan Wei Ming", "weiming.tan@printhub.com.my",
				"01123456789", CustomerType.CORPORATE, 25);

		assertEquals("C0003,Tan Wei Ming,weiming.tan@printhub.com.my,01123456789,Corporate,25",
				customer.toFileRecord());
	}

	/**
	 * VALID CASE - equals is relied on by assertEquals and assertArrayEquals in the
	 * ReadCustomer tests, so its behaviour is verified explicitly here.
	 */
	@Test
	public void testEqualsComparesEveryAttribute() {

		Customer first = new Customer("C0001", "Ali Bin Ahmad", "ali@example.com", "0123456789",
				CustomerType.STUDENT, 3);
		Customer identical = new Customer("C0001", "Ali Bin Ahmad", "ali@example.com", "0123456789",
				CustomerType.STUDENT, 3);
		Customer differentType = new Customer("C0001", "Ali Bin Ahmad", "ali@example.com",
				"0123456789", CustomerType.REGULAR, 3);

		assertEquals(first, identical);
		assertEquals(first.hashCode(), identical.hashCode());
		assertNotEquals(first, differentType);
		assertNotEquals(first, null);
	}

	/**
	 * VALID CASE - verifies the discount rates held against each customer type, which
	 * ApplyDiscount depends on (Table 4).
	 */
	@Test
	@Parameters({ "Regular, 0.00",
			"Student, 0.10",
			"Corporate, 0.15" })
	public void testCustomerTypeCarriesTheCorrectDiscountRate(String customerType,
			double expectedRate) {

		assertEquals(expectedRate, CustomerType.fromString(customerType).getDiscountRate(), 0.001);
	}

	// --------------------------------------------------------------- INVALID CASES

	/**
	 * INVALID CASE - parameterised, test data read from an external text file.
	 * Each row of customer-invalid-test-data.csv sits in a different invalid
	 * equivalence partition (name too short, name too long, name with digits,
	 * malformed email, malformed phone number, unsupported customer type) and the
	 * final column records why the row must be rejected.
	 *
	 * The expected exception is declared on the @Test annotation.
	 */
	@Test(expected = IllegalArgumentException.class)
	@FileParameters(value = "customer-invalid-test-data.csv", mapper = CsvWithHeaderMapper.class)
	public void testCreateCustomerWithInvalidDetails(String name, String emailAddress,
			String phoneNumber, String customerType, String reasonForRejection) {

		new Customer("C0001", name, emailAddress, phoneNumber, CustomerType.fromString(customerType));
	}

	/**
	 * INVALID CASE - Equivalence Partitioning and Boundary Value Analysis on the
	 * customer ID format: too few digits, too many digits, wrong prefix, no prefix and
	 * an empty string are all rejected.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters({ "C001",
			"C00001",
			"X0001",
			"0001",
			"C 001",
			"CUSTOMER1",
			"" })
	public void testInvalidCustomerIdIsRejected(String customerId) {

		new Customer(customerId, "Valid Name", "valid@example.com", "0123334444",
				CustomerType.REGULAR);
	}

	/**
	 * INVALID CASE - Boundary Value Analysis on the name length.
	 * 1 character is just below the lower boundary and 51 characters is just above the
	 * upper boundary, so both must be rejected.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters(method = "getInvalidNameLengthBoundaries")
	public void testNameLengthOutsideBoundariesIsRejected(String name) {

		new Customer("C0001", name, "valid@example.com", "0123334444", CustomerType.REGULAR);
	}

	private Object[] getInvalidNameLengthBoundaries() {
		return new Object[] {
				new Object[] { buildName(1) },   // just below the lower boundary
				new Object[] { buildName(51) },  // just above the upper boundary
				new Object[] { buildName(80) }   // well inside the invalid high partition
		};
	}

	/**
	 * INVALID CASE - null values fall into their own invalid partition and cannot be
	 * expressed in a CSV file, so they are supplied from a parameter method.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters(method = "getNullAndBlankFieldCombinations")
	public void testNullOrBlankFieldIsRejected(String customerId, String name,
			String emailAddress, String phoneNumber, CustomerType customerType) {

		new Customer(customerId, name, emailAddress, phoneNumber, customerType);
	}

	private Object[] getNullAndBlankFieldCombinations() {
		return new Object[] {
				new Object[] { null, "Valid Name", "valid@example.com", "0123334444", CustomerType.REGULAR },
				new Object[] { "C0001", null, "valid@example.com", "0123334444", CustomerType.REGULAR },
				new Object[] { "C0001", "Valid Name", null, "0123334444", CustomerType.REGULAR },
				new Object[] { "C0001", "Valid Name", "valid@example.com", null, CustomerType.REGULAR },
				new Object[] { "C0001", "Valid Name", "valid@example.com", "0123334444", null },
				new Object[] { "   ", "Valid Name", "valid@example.com", "0123334444", CustomerType.REGULAR },
				new Object[] { "C0001", "   ", "valid@example.com", "0123334444", CustomerType.REGULAR },
				new Object[] { "C0001", "Valid Name", "   ", "0123334444", CustomerType.REGULAR },
				new Object[] { "C0001", "Valid Name", "valid@example.com", "   ", CustomerType.REGULAR }
		};
	}

	/**
	 * INVALID CASE - Boundary Value Analysis on the phone number length.
	 * The valid partition is 10 or 11 digits beginning with 01, so 9 digits is just
	 * below and 12 digits is just above.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters({ "012345678",
			"012345678901",
			"0221234567",
			"1123456789",
			"01-2345678" })
	public void testInvalidPhoneNumberIsRejected(String phoneNumber) {

		new Customer("C0001", "Valid Name", "valid@example.com", phoneNumber, CustomerType.REGULAR);
	}

	/**
	 * INVALID CASE - Boundary Value Analysis on the previous order count.
	 * -1 is the value immediately below the valid partition.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters({ "-1", "-20" })
	public void testNegativePreviousOrderCountIsRejected(int previousOrderCount) {

		new Customer("C0001", "Valid Name", "valid@example.com", "0123334444", CustomerType.REGULAR,
				previousOrderCount);
	}

	/**
	 * INVALID CASE - unsupported customer type values are rejected by
	 * CustomerType.fromString. The exception message is asserted as well as the
	 * exception type, so the test proves that the rejected value is reported back to
	 * the user rather than being silently swallowed.
	 */
	@Test
	@Parameters({ "Platinum", "VIP", "Gold", "Lecturer" })
	public void testUnsupportedCustomerTypeIsRejected(String customerType) {

		try {
			CustomerType.fromString(customerType);
			fail("An IllegalArgumentException was expected for the customer type " + customerType);
		} catch (IllegalArgumentException e) {
			assertTrue("The message should name the rejected value",
					e.getMessage().contains(customerType));
		}
	}

	// ---------------------------------------------------------------------- HELPER

	/**
	 * Builds a name of exactly the requested length, used by the boundary value tests.
	 */
	private String buildName(int length) {

		StringBuilder name = new StringBuilder();
		for (int i = 0; i < length; i++)
			name.append('A');
		return name.toString();
	}
}
