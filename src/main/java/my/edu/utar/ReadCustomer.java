package my.edu.utar;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * FR4 - Retrieve Existing Customer.
 *
 * Reads customer information from a plain text file (customer.txt) and returns the
 * customer details for a given customer ID.
 *
 * Each line of the file holds one customer record in the following comma separated
 * format:
 *
 *   customerId,name,emailAddress,phoneNumber,customerType,previousOrderCount
 *
 * Example:
 *
 *   C0001,Ali Bin Ahmad,ali.ahmad@example.com,0123456789,Student,3
 *
 * Blank lines and lines beginning with a hash (#) are treated as comments and are
 * skipped, which allows the data file to carry a readable column header.
 */
public class ReadCustomer {

	/** Default name of the customer data file, stored in the project directory. */
	public static final String DEFAULT_FILE_NAME = "customer.txt";

	/** Number of comma separated fields expected in one customer record. */
	public static final int NUMBER_OF_FIELDS = 6;

	private String fileName;

	/**
	 * Creates a reader for the default customer data file, customer.txt.
	 */
	public ReadCustomer() {
		this(DEFAULT_FILE_NAME);
	}

	/**
	 * Creates a reader for the given customer data file. Allowing the file name to be
	 * injected keeps the class testable, because the test code can point it at a
	 * temporary file instead of the live customer.txt.
	 *
	 * @throws IllegalArgumentException if the file name is null or blank
	 */
	public ReadCustomer(String fileName) {
		setFileName(fileName);
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {

		if (fileName == null || fileName.trim().isEmpty())
			throw new IllegalArgumentException("File name must not be empty");

		this.fileName = fileName.trim();
	}

	/**
	 * Reads every customer record held in the data file.
	 *
	 * @return a list of customers, empty if the file holds no records
	 * @throws IllegalArgumentException if the file does not exist or a record is malformed
	 */
	public List<Customer> readAllCustomers() {

		List<Customer> customers = new ArrayList<Customer>();

		File fileToRead = new File(fileName);
		Scanner inputStream = null;

		try {
			inputStream = new Scanner(fileToRead);
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("Customer file does not exist : " + fileName);
		}

		// the file must be closed even when a malformed record aborts the read, otherwise
		// the handle stays open and the file can no longer be written to or deleted
		try {
			int lineNumber = 0;
			while (inputStream.hasNextLine()) {

				String lineRead = inputStream.nextLine();
				lineNumber++;

				// skip blank lines and comment/header lines
				if (lineRead.trim().isEmpty() || lineRead.trim().startsWith("#"))
					continue;

				customers.add(parseCustomerRecord(lineRead, lineNumber));
			}
		} finally {
			inputStream.close();
		}

		return customers;
	}

	/**
	 * Converts one line of the customer data file into a Customer object.
	 *
	 * @throws IllegalArgumentException if the record does not hold exactly six fields
	 *         or if any field value is invalid
	 */
	private Customer parseCustomerRecord(String record, int lineNumber) {

		String[] fields = record.split(",");

		if (fields.length != NUMBER_OF_FIELDS)
			throw new IllegalArgumentException("Malformed customer record at line " + lineNumber
					+ " of " + fileName + " : expected " + NUMBER_OF_FIELDS + " fields but found "
					+ fields.length);

		String customerId = fields[0].trim();
		String name = fields[1].trim();
		String emailAddress = fields[2].trim();
		String phoneNumber = fields[3].trim();
		CustomerType customerType = CustomerType.fromString(fields[4].trim());

		int previousOrderCount;
		try {
			previousOrderCount = Integer.parseInt(fields[5].trim());
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Malformed customer record at line " + lineNumber
					+ " of " + fileName + " : previous order count is not a whole number : " + fields[5]);
		}

		return new Customer(customerId, name, emailAddress, phoneNumber, customerType, previousOrderCount);
	}

	/**
	 * Returns the customer details for the given customer ID.
	 *
	 * @param customerId the ID of the customer to retrieve
	 * @return the matching Customer, or null if no customer holds that ID
	 * @throws IllegalArgumentException if the customer ID is null, blank or badly
	 *         formatted, or if the customer file cannot be read
	 */
	public Customer getCustomerById(String customerId) {

		if (customerId == null || customerId.trim().isEmpty())
			throw new IllegalArgumentException("Customer ID must not be empty");

		String cleanedId = customerId.trim().toUpperCase();
		if (!cleanedId.matches(Customer.CUSTOMER_ID_PATTERN))
			throw new IllegalArgumentException("Invalid customer ID format : " + customerId);

		for (Customer customer : readAllCustomers()) {
			if (customer.getCustomerId().equals(cleanedId))
				return customer;
		}
		return null;
	}

	/**
	 * @return true if a customer with the given ID exists in the data file
	 * @throws IllegalArgumentException if the customer ID is invalid or the file cannot be read
	 */
	public boolean customerExists(String customerId) {
		return getCustomerById(customerId) != null;
	}

	/**
	 * @return the number of customer records currently held in the data file
	 * @throws IllegalArgumentException if the file cannot be read
	 */
	public int getCustomerCount() {
		return readAllCustomers().size();
	}
}
