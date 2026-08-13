package my.edu.utar;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * FR3 - Register New Customer.
 *
 * Adds a new customer's information to the customer data file (customer.txt).
 * A new customer ID is generated automatically by taking the highest existing ID
 * and adding one, so the caller only needs to supply the name, email address,
 * phone number and customer type.
 *
 * The class collaborates with {@link ReadCustomer} to obtain the existing records.
 * That collaborator is injected through the constructor, which allows the test code
 * to replace it with a Mockito test double.
 */
public class AddNewCustomer {

	/** The first ID issued when the customer file holds no records yet. */
	public static final String FIRST_CUSTOMER_ID = "C0001";

	/** The largest customer ID the four digit numbering scheme can represent. */
	public static final int MAX_CUSTOMER_NUMBER = 9999;

	private String fileName;
	private ReadCustomer readCustomer;

	/**
	 * Creates a registration service that writes to the default customer.txt file.
	 */
	public AddNewCustomer() {
		this(ReadCustomer.DEFAULT_FILE_NAME, new ReadCustomer());
	}

	/**
	 * Creates a registration service.
	 *
	 * @param fileName the customer data file to append to
	 * @param readCustomer the collaborator used to read the existing records
	 * @throws IllegalArgumentException if the file name is null or blank, or the
	 *         reader is null
	 */
	public AddNewCustomer(String fileName, ReadCustomer readCustomer) {

		if (fileName == null || fileName.trim().isEmpty())
			throw new IllegalArgumentException("File name must not be empty");
		if (readCustomer == null)
			throw new IllegalArgumentException("ReadCustomer must not be null");

		this.fileName = fileName.trim();
		this.readCustomer = readCustomer;
	}

	public String getFileName() {
		return fileName;
	}

	public ReadCustomer getReadCustomer() {
		return readCustomer;
	}

	/**
	 * Works out the next customer ID by finding the highest numeric part currently
	 * held in the data file and adding one.
	 *
	 * @return the next customer ID, e.g. C0004 when C0003 is the highest existing ID
	 * @throws IllegalStateException if the four digit numbering scheme is exhausted
	 */
	public String generateNextCustomerId() {

		List<Customer> existingCustomers = readCustomer.readAllCustomers();

		if (existingCustomers.isEmpty())
			return FIRST_CUSTOMER_ID;

		int highestNumber = 0;
		for (Customer customer : existingCustomers) {
			int number = Integer.parseInt(customer.getCustomerId().substring(1));
			if (number > highestNumber)
				highestNumber = number;
		}

		if (highestNumber >= MAX_CUSTOMER_NUMBER)
			throw new IllegalStateException("Customer ID range has been exhausted");

		return String.format("C%04d", highestNumber + 1);
	}

	/**
	 * Registers a new customer. The customer ID is generated automatically and the
	 * completed record is appended to the customer data file.
	 *
	 * @param name the customer's name
	 * @param emailAddress the customer's email address
	 * @param phoneNumber the customer's phone number
	 * @param customerType the customer's type
	 * @return the newly created Customer, including the generated customer ID
	 * @throws IllegalArgumentException if any supplied value is invalid, or if the
	 *         email address is already registered
	 */
	public Customer addCustomer(String name, String emailAddress, String phoneNumber,
			CustomerType customerType) {

		// Customer performs the field level validation, so an invalid name, email
		// address or phone number is rejected before anything is written to the file.
		Customer newCustomer = new Customer(generateNextCustomerId(), name, emailAddress,
				phoneNumber, customerType, 0);

		if (isEmailAlreadyRegistered(newCustomer.getEmailAddress()))
			throw new IllegalArgumentException("Email address is already registered : " + emailAddress);

		writeCustomerRecord(newCustomer);
		return newCustomer;
	}

	/**
	 * Appends an already constructed customer to the data file.
	 *
	 * @param customer the customer to add
	 * @return true when the record has been written
	 * @throws IllegalArgumentException if the customer is null or its ID is already in use
	 */
	public boolean addCustomer(Customer customer) {

		if (customer == null)
			throw new IllegalArgumentException("Customer must not be null");

		if (readCustomer.customerExists(customer.getCustomerId()))
			throw new IllegalArgumentException("Customer ID is already in use : "
					+ customer.getCustomerId());

		writeCustomerRecord(customer);
		return true;
	}

	/**
	 * @return true if the email address already belongs to a registered customer
	 */
	public boolean isEmailAlreadyRegistered(String emailAddress) {

		if (emailAddress == null || emailAddress.trim().isEmpty())
			throw new IllegalArgumentException("Email address must not be empty");

		for (Customer customer : readCustomer.readAllCustomers()) {
			if (customer.getEmailAddress().equalsIgnoreCase(emailAddress.trim()))
				return true;
		}
		return false;
	}

	/**
	 * Appends one customer record to the end of the data file.
	 *
	 * @throws IllegalArgumentException if the file cannot be opened for writing
	 */
	private void writeCustomerRecord(Customer customer) {

		File theFile = new File(fileName);
		PrintWriter output = null;

		try {
			// the true argument opens the file in append mode so existing records are kept
			output = new PrintWriter(new FileWriter(theFile, true));
			output.println(customer.toFileRecord());
		} catch (IOException e) {
			throw new IllegalArgumentException("Problem opening file for writing : " + fileName);
		} finally {
			if (output != null)
				output.close();
		}
	}
}
