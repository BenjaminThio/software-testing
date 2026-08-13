package my.edu.utar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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
 * Unit tests for AddNewCustomer (FR3 - Register New Customer).
 *
 * Test design techniques applied in this class:
 *   - Equivalence Partitioning and Boundary Value Analysis on the registration
 *     details, with the test values read from external text files;
 *   - TEST DOUBLES: the ReadCustomer collaborator is replaced by a Mockito test
 *     double. It is used as a STUB when a test needs the existing record list to hold
 *     a particular value (for example to drive customer ID generation), and as a MOCK
 *     when a test needs to verify that the collaborator was actually consulted.
 *     Using a test double keeps these unit tests independent of the real
 *     customer.txt, so they cannot be broken by other tests adding records to it.
 */
@RunWith(JUnitParamsRunner.class)
public class AddNewCustomerUnitTests {

	/** Scratch output file, recreated before every test and removed afterwards. */
	private static final String OUTPUT_FILE = "addnewcustomer-scratch.txt";

	private ReadCustomer readCustomerMock;
	private AddNewCustomer addNewCustomer;

	private Customer existingCustomerOne;
	private Customer existingCustomerTwo;

	@Before
	public void setupForAllTests() {

		removeOutputFile();

		existingCustomerOne = new Customer("C0001", "Ali Bin Ahmad", "ali.ahmad@example.com",
				"0123456789", CustomerType.STUDENT, 3);
		existingCustomerTwo = new Customer("C0002", "Siti Nurhaliza", "siti.n@example.com",
				"0139876543", CustomerType.REGULAR, 12);

		// create the test double for the collaborator that reads the customer file
		readCustomerMock = mock(ReadCustomer.class);
		addNewCustomer = new AddNewCustomer(OUTPUT_FILE, readCustomerMock);
	}

	@After
	public void removeOutputFile() {
		File outputFile = new File(OUTPUT_FILE);
		if (outputFile.exists())
			outputFile.delete();
	}

	/**
	 * Makes the test double behave as a STUB that returns the supplied records
	 * whenever the class under test asks for the existing customers.
	 */
	private void stubExistingCustomers(Customer... customers) {
		when(readCustomerMock.readAllCustomers())
				.thenReturn(new ArrayList<Customer>(Arrays.asList(customers)));
	}

	// ----------------------------------------------------------------- VALID CASES

	/**
	 * VALID CASE with a TEST DOUBLE used as a STUB.
	 * The stubbed record list determines the highest existing customer ID, so the
	 * next ID can be verified without touching the real customer.txt.
	 * Boundary Value Analysis is applied to the four digit numbering scheme: an empty
	 * file yields C0001 and C9998 yields C9999, the highest ID the scheme allows.
	 */
	@Test
	@Parameters(method = "getExistingIdsAndTheExpectedNextId")
	public void testGenerateNextCustomerId(String[] existingIds, String expectedNextId) {

		List<Customer> existingCustomers = new ArrayList<Customer>();
		for (String existingId : existingIds)
			existingCustomers.add(new Customer(existingId, "Valid Name", "valid@example.com",
					"0123334444", CustomerType.REGULAR));

		// STUB : fix the answer the collaborator gives
		when(readCustomerMock.readAllCustomers()).thenReturn(existingCustomers);

		assertEquals(expectedNextId, addNewCustomer.generateNextCustomerId());

		// MOCK : the collaborator must actually have been consulted exactly once
		verify(readCustomerMock, times(1)).readAllCustomers();
	}

	private Object[] getExistingIdsAndTheExpectedNextId() {
		return new Object[] {
				// empty file, lower boundary of the numbering scheme
				new Object[] { new String[] {}, "C0001" },
				new Object[] { new String[] { "C0001" }, "C0002" },
				new Object[] { new String[] { "C0001", "C0002", "C0003" }, "C0004" },
				// the highest ID is not necessarily the last record in the file
				new Object[] { new String[] { "C0005", "C0001", "C0003" }, "C0006" },
				// crossing a digit boundary
				new Object[] { new String[] { "C0009" }, "C0010" },
				new Object[] { new String[] { "C0099" }, "C0100" },
				// upper boundary of the numbering scheme
				new Object[] { new String[] { "C9998" }, "C9999" }
		};
	}

	/**
	 * VALID CASE - parameterised, test data read from an external text file.
	 * A new customer is registered, given the next available ID, and the record is
	 * appended to the data file so that it can be read back.
	 */
	@Test
	@FileParameters(value = "customer-valid-test-data.csv", mapper = CsvWithHeaderMapper.class)
	public void testAddCustomerWithValidDetails(String name, String emailAddress,
			String phoneNumber, String customerType, String expectedStoredPhone) {

		stubExistingCustomers(existingCustomerOne, existingCustomerTwo);

		Customer newCustomer = addNewCustomer.addCustomer(name, emailAddress, phoneNumber,
				CustomerType.fromString(customerType));

		// the generated ID follows on from the highest stubbed ID, C0002
		assertEquals("C0003", newCustomer.getCustomerId());
		assertEquals(name, newCustomer.getName());
		assertEquals(emailAddress, newCustomer.getEmailAddress());
		assertEquals(expectedStoredPhone, newCustomer.getPhoneNumber());
		assertEquals(CustomerType.fromString(customerType), newCustomer.getCustomerType());
		assertEquals(0, newCustomer.getPreviousOrderCount());

		// the record must now be present in the data file
		assertTrue("The customer record should have been written to " + OUTPUT_FILE,
				new File(OUTPUT_FILE).exists());
		assertEquals(newCustomer, readBackTheOnlyRecord());
	}

	/**
	 * VALID CASE - an already constructed Customer can be appended directly, which is
	 * the path used when migrating records.
	 */
	@Test
	public void testAddAnAlreadyConstructedCustomer() {

		Customer customer = new Customer("C0007", "Rajesh Kumar", "rajesh.k@bizprint.com.my",
				"0134445555", CustomerType.CORPORATE, 8);

		// STUB : the ID is not yet in use
		when(readCustomerMock.customerExists("C0007")).thenReturn(false);

		assertTrue(addNewCustomer.addCustomer(customer));
		assertEquals(customer, readBackTheOnlyRecord());

		// MOCK : the duplicate check must have been performed with that exact ID
		verify(readCustomerMock, times(1)).customerExists("C0007");
	}

	/**
	 * VALID CASE - a brand new email address is not reported as already registered.
	 */
	@Test
	@Parameters({ "brandnew@example.com, false",
			"ali.ahmad@example.com, true",
			"ALI.AHMAD@EXAMPLE.COM, true",
			"siti.n@example.com, true",
			"unknown@example.com, false" })
	public void testIsEmailAlreadyRegistered(String emailAddress, boolean expectedResult) {

		stubExistingCustomers(existingCustomerOne, existingCustomerTwo);

		assertEquals(expectedResult, addNewCustomer.isEmailAlreadyRegistered(emailAddress));
	}

	// --------------------------------------------------------------- INVALID CASES

	/**
	 * INVALID CASE - parameterised, test data read from an external text file.
	 * Every row of customer-invalid-test-data.csv must be rejected, and crucially
	 * NOTHING may be written to the data file when the details are rejected.
	 */
	@Test
	@FileParameters(value = "customer-invalid-test-data.csv", mapper = CsvWithHeaderMapper.class)
	public void testAddCustomerWithInvalidDetailsWritesNothing(String name, String emailAddress,
			String phoneNumber, String customerType, String reasonForRejection) {

		stubExistingCustomers(existingCustomerOne);

		try {
			addNewCustomer.addCustomer(name, emailAddress, phoneNumber,
					CustomerType.fromString(customerType));
			org.junit.Assert.fail("Registration should have been rejected because the "
					+ reasonForRejection);
		} catch (IllegalArgumentException e) {
			// expected : the details are invalid
		}

		assertFalse("No record may be written when the details are rejected : " + reasonForRejection,
				new File(OUTPUT_FILE).exists());
	}

	/**
	 * INVALID CASE - registering an email address that already belongs to another
	 * customer is rejected, and no record is written.
	 */
	@Test
	public void testDuplicateEmailAddressIsRejected() {

		stubExistingCustomers(existingCustomerOne, existingCustomerTwo);

		try {
			addNewCustomer.addCustomer("New Person", "ali.ahmad@example.com", "0123334444",
					CustomerType.REGULAR);
			org.junit.Assert.fail("An IllegalArgumentException was expected for a duplicate email");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("already registered"));
		}

		assertFalse(new File(OUTPUT_FILE).exists());
	}

	/**
	 * INVALID CASE - adding a Customer whose ID is already in use is rejected.
	 * The MOCK verifies that the duplicate was detected by asking the collaborator,
	 * not by some other means.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testDuplicateCustomerIdIsRejected() {

		when(readCustomerMock.customerExists("C0001")).thenReturn(true);

		addNewCustomer.addCustomer(existingCustomerOne);
	}

	/**
	 * INVALID CASE - a null Customer is rejected before the collaborator is consulted
	 * at all. verify(..., never()) proves that no interaction took place.
	 */
	@Test
	public void testNullCustomerIsRejectedWithoutConsultingTheCollaborator() {

		try {
			addNewCustomer.addCustomer(null);
			org.junit.Assert.fail("An IllegalArgumentException was expected for a null customer");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("must not be null"));
		}

		verify(readCustomerMock, never()).customerExists(anyString());
		verify(readCustomerMock, never()).readAllCustomers();
	}

	/**
	 * INVALID CASE - the constructor rejects a missing file name or a missing
	 * collaborator, supplied here from a parameter method so that nulls can be used.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters(method = "getInvalidConstructorArguments")
	public void testConstructorRejectsInvalidArguments(String fileName, ReadCustomer readCustomer) {

		new AddNewCustomer(fileName, readCustomer);
	}

	private Object[] getInvalidConstructorArguments() {
		return new Object[] {
				new Object[] { null, mock(ReadCustomer.class) },
				new Object[] { "", mock(ReadCustomer.class) },
				new Object[] { "   ", mock(ReadCustomer.class) },
				new Object[] { OUTPUT_FILE, null }
		};
	}

	/**
	 * INVALID CASE - a blank email address cannot be checked for duplication.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters({ "", "   " })
	public void testIsEmailAlreadyRegisteredRejectsABlankAddress(String emailAddress) {

		addNewCustomer.isEmailAlreadyRegistered(emailAddress);
	}

	/**
	 * INVALID CASE - the four digit numbering scheme is exhausted once C9999 exists.
	 * Boundary Value Analysis on the upper boundary of the ID range.
	 */
	@Test(expected = IllegalStateException.class)
	public void testGenerateNextCustomerIdWhenTheRangeIsExhausted() {

		stubExistingCustomers(new Customer("C9999", "Last Customer", "last@example.com",
				"0123334444", CustomerType.REGULAR));

		addNewCustomer.generateNextCustomerId();
	}

	// ---------------------------------------------------------------------- HELPER

	/**
	 * Reads the scratch output file back with a real ReadCustomer and returns the
	 * single record it holds. This closes the loop: what AddNewCustomer wrote must be
	 * exactly what ReadCustomer can read.
	 */
	private Customer readBackTheOnlyRecord() {

		List<Customer> writtenRecords = new ReadCustomer(OUTPUT_FILE).readAllCustomers();
		assertEquals("Exactly one record should have been written", 1, writtenRecords.size());
		return writtenRecords.get(0);
	}
}
