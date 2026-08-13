package my.edu.utar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.FileParameters;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.mappers.CsvWithHeaderMapper;

/**
 * Unit tests for ReadCustomer (FR4 - Retrieve Existing Customer).
 *
 * Test design techniques applied in this class:
 *   - Equivalence Partitioning on the customer ID argument: valid and existing,
 *     valid but not present in the file, and badly formatted;
 *   - Equivalence Partitioning on the state of the data file itself: populated,
 *     empty, missing and malformed;
 *   - The expected customer details are READ FROM A TEXT FILE
 *     (customer-read-test-data.csv) and compared against the records the class reads
 *     from a second text file (customer-test-records.txt), so neither the input nor
 *     the expected output is hardcoded in the test code.
 */
@RunWith(JUnitParamsRunner.class)
public class ReadCustomerUnitTests {

	/** Fixture file holding the customer records the tests expect to find. */
	private static final String TEST_RECORDS_FILE = "customer-test-records.txt";

	/** Scratch file created and removed by individual tests. */
	private static final String SCRATCH_FILE = "readcustomer-scratch.txt";

	private ReadCustomer readCustomer;

	@Before
	public void setupForAllTests() {
		readCustomer = new ReadCustomer(TEST_RECORDS_FILE);
	}

	@After
	public void removeScratchFile() {
		File scratchFile = new File(SCRATCH_FILE);
		if (scratchFile.exists())
			scratchFile.delete();
	}

	// ----------------------------------------------------------------- VALID CASES

	/**
	 * VALID CASE - parameterised, expected values read from an external text file.
	 * Equivalence Partitioning : a customer ID that is well formed AND present in the
	 * data file must return the complete, correctly parsed record.
	 */
	@Test
	@FileParameters(value = "customer-read-test-data.csv", mapper = CsvWithHeaderMapper.class)
	public void testGetCustomerByIdReturnsTheStoredDetails(String customerId, String expectedName,
			String expectedEmail, String expectedPhone, String expectedType,
			int expectedPreviousOrders) {

		Customer customer = readCustomer.getCustomerById(customerId);

		assertNotNull("Customer " + customerId + " should be found in " + TEST_RECORDS_FILE, customer);
		assertEquals(customerId, customer.getCustomerId());
		assertEquals(expectedName, customer.getName());
		assertEquals(expectedEmail, customer.getEmailAddress());
		assertEquals(expectedPhone, customer.getPhoneNumber());
		assertEquals(CustomerType.fromString(expectedType), customer.getCustomerType());
		assertEquals(expectedPreviousOrders, customer.getPreviousOrderCount());
	}

	/**
	 * VALID CASE - the whole file is read, comment and header lines are skipped and
	 * the records are returned in file order.
	 */
	@Test
	public void testReadAllCustomersSkipsCommentLines() {

		List<Customer> customers = readCustomer.readAllCustomers();

		assertEquals(5, customers.size());
		assertEquals("C0001", customers.get(0).getCustomerId());
		assertEquals("C0005", customers.get(4).getCustomerId());
	}

	/**
	 * VALID CASE - the customer ID lookup is case insensitive and tolerates
	 * surrounding blanks, because staff type the ID by hand.
	 */
	@Test
	@Parameters(method = "getUntidyCustomerIds")
	public void testGetCustomerByIdAcceptsUntidyInput(String customerId) {

		Customer customer = readCustomer.getCustomerById(customerId);

		assertNotNull(customer);
		assertEquals("C0001", customer.getCustomerId());
		assertEquals("Ali Bin Ahmad", customer.getName());
	}

	private Object[] getUntidyCustomerIds() {
		// supplied from a parameter method because JUnitParams trims the values of an
		// inline @Parameters string, which would remove the very blanks being tested
		return new Object[] {
				new Object[] { "C0001" },
				new Object[] { "c0001" },
				new Object[] { " C0001 " },
				new Object[] { "\tc0001\t" }
		};
	}

	/**
	 * VALID CASE - customerExists reports true for a stored ID and false for a well
	 * formed ID that is not in the file. Both partitions are covered in one
	 * parameterised test.
	 */
	@Test
	@Parameters({ "C0001, true",
			"C0003, true",
			"C0005, true",
			"C0006, false",
			"C9999, false" })
	public void testCustomerExists(String customerId, boolean expectedToExist) {

		assertEquals(expectedToExist, readCustomer.customerExists(customerId));
	}

	/**
	 * VALID CASE - an empty data file is a legal state; it yields no records rather
	 * than an error.
	 */
	@Test
	public void testReadAllCustomersFromAnEmptyFile() throws Exception {

		writeScratchFile("# only a comment line, no records");
		ReadCustomer emptyFileReader = new ReadCustomer(SCRATCH_FILE);

		assertTrue(emptyFileReader.readAllCustomers().isEmpty());
		assertEquals(0, emptyFileReader.getCustomerCount());
	}

	/**
	 * VALID CASE - the loyalty relevant field is parsed correctly, since ApplyDiscount
	 * depends on it for the Table 4 additional 5% discount.
	 */
	@Test
	public void testPreviousOrderCountIsParsedFromTheFile() {

		assertEquals(25, readCustomer.getCustomerById("C0003").getPreviousOrderCount());
		assertEquals(21, readCustomer.getCustomerById("C0004").getPreviousOrderCount());
		assertEquals(20, readCustomer.getCustomerById("C0005").getPreviousOrderCount());
	}

	// --------------------------------------------------------------- INVALID CASES

	/**
	 * INVALID CASE - a well formed customer ID that is not present in the file is NOT
	 * an error; the method returns null so the caller can offer to register the
	 * customer instead. assertNull is the meaningful assertion here.
	 */
	@Test
	@Parameters({ "C0006", "C0099", "C9999", "C0000" })
	public void testGetCustomerByIdReturnsNullWhenNotFound(String customerId) {

		assertNull(readCustomer.getCustomerById(customerId));
		assertFalse(readCustomer.customerExists(customerId));
	}

	/**
	 * INVALID CASE - a badly formatted customer ID IS an error and is rejected before
	 * the file is opened at all.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters({ "C001",
			"C00001",
			"X0001",
			"0001",
			"CUSTOMER",
			"" })
	public void testGetCustomerByIdRejectsABadlyFormattedId(String customerId) {

		readCustomer.getCustomerById(customerId);
	}

	/**
	 * INVALID CASE - a null customer ID is supplied from a parameter method, because a
	 * null cannot be written inside an inline @Parameters string.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters(method = "getNullCustomerId")
	public void testGetCustomerByIdRejectsANullId(String customerId) {

		readCustomer.getCustomerById(customerId);
	}

	private Object[] getNullCustomerId() {
		return new Object[] { new Object[] { (String) null } };
	}

	/**
	 * INVALID CASE - reading a file that does not exist throws
	 * IllegalArgumentException, and the message names the missing file so the failure
	 * can be diagnosed.
	 */
	@Test
	public void testReadAllCustomersFromAMissingFile() {

		ReadCustomer missingFileReader = new ReadCustomer("somecrazyfilenamethatdoesnotexist.txt");

		try {
			missingFileReader.readAllCustomers();
			org.junit.Assert.fail("An IllegalArgumentException was expected for a missing file");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("somecrazyfilenamethatdoesnotexist.txt"));
		}
	}

	/**
	 * INVALID CASE - a record holding the wrong number of fields is rejected.
	 * Boundary Value Analysis on the field count: 5 fields is one too few and 7 fields
	 * is one too many, against the required 6.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters(method = "getRecordsWithTheWrongFieldCount")
	public void testMalformedRecordFieldCountIsRejected(String malformedRecord) throws Exception {

		writeScratchFile(malformedRecord);
		new ReadCustomer(SCRATCH_FILE).readAllCustomers();
	}

	private Object[] getRecordsWithTheWrongFieldCount() {
		// the whole record is one parameter, so it must be supplied from a parameter
		// method: JUnitParams would split an inline @Parameters string at every comma
		return new Object[] {
				// 5 fields, one below the required 6
				new Object[] { "C0001,Ali Bin Ahmad,ali@example.com,0123456789,Student" },
				// 7 fields, one above the required 6
				new Object[] { "C0001,Ali Bin Ahmad,ali@example.com,0123456789,Student,3,extra" },
				// 1 field
				new Object[] { "C0001" }
		};
	}

	/**
	 * INVALID CASE - a record whose field values break the Customer validation rules,
	 * or whose previous order count is not a whole number, is rejected while reading.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters(method = "getRecordsWithAnInvalidFieldValue")
	public void testMalformedFieldValueIsRejected(String malformedRecord) throws Exception {

		writeScratchFile(malformedRecord);
		new ReadCustomer(SCRATCH_FILE).readAllCustomers();
	}

	private Object[] getRecordsWithAnInvalidFieldValue() {
		return new Object[] {
				// previous order count is not a whole number
				new Object[] { "C0001,Ali Bin Ahmad,ali@example.com,0123456789,Student,many" },
				// email address is malformed
				new Object[] { "C0001,Ali Bin Ahmad,not-an-email,0123456789,Student,3" },
				// phone number is too short
				new Object[] { "C0001,Ali Bin Ahmad,ali@example.com,12345,Student,3" },
				// customer ID does not follow the C9999 pattern
				new Object[] { "BADID,Ali Bin Ahmad,ali@example.com,0123456789,Student,3" },
				// customer type is not supported
				new Object[] { "C0001,Ali Bin Ahmad,ali@example.com,0123456789,Platinum,3" },
				// previous order count is negative
				new Object[] { "C0001,Ali Bin Ahmad,ali@example.com,0123456789,Student,-2" }
		};
	}

	/**
	 * INVALID CASE - the file name the reader is built with must itself be usable.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters({ "", "   " })
	public void testConstructorRejectsABlankFileName(String fileName) {

		new ReadCustomer(fileName);
	}

	// ---------------------------------------------------------------------- HELPER

	/**
	 * Writes a single line into the scratch data file used by the malformed record
	 * tests. The file is removed again by the @After method.
	 */
	private void writeScratchFile(String content) throws Exception {

		PrintWriter output = new PrintWriter(new File(SCRATCH_FILE));
		output.println(content);
		output.close();
	}
}
