package my.edu.utar;

/**
 * FR2 - Customer Details.
 *
 * Holds the information of a single PrintMaster customer: customer ID, name,
 * email address, phone number, customer type and the number of orders the
 * customer has placed previously.
 *
 * The previous order count is required by Table 4 of the specification, which
 * grants an additional 5% discount to an existing customer with more than 20
 * previous orders.
 *
 * All validation is performed in the setters so that a Customer object can never
 * be constructed in an invalid state. Invalid data causes an
 * IllegalArgumentException to be thrown.
 */
public class Customer {

	/** Customer IDs are of the form C followed by exactly four digits, e.g. C0001. */
	public static final String CUSTOMER_ID_PATTERN = "C\\d{4}";

	public static final int MIN_NAME_LENGTH = 2;
	public static final int MAX_NAME_LENGTH = 50;

	private String customerId;
	private String name;
	private String emailAddress;
	private String phoneNumber;
	private CustomerType customerType;
	private int previousOrderCount;

	/**
	 * Creates a customer who has not placed any previous order.
	 */
	public Customer(String customerId, String name, String emailAddress, String phoneNumber,
			CustomerType customerType) {
		this(customerId, name, emailAddress, phoneNumber, customerType, 0);
	}

	/**
	 * Creates a customer.
	 *
	 * @throws IllegalArgumentException if any of the supplied values is invalid
	 */
	public Customer(String customerId, String name, String emailAddress, String phoneNumber,
			CustomerType customerType, int previousOrderCount) {
		setCustomerId(customerId);
		setName(name);
		setEmailAddress(emailAddress);
		setPhoneNumber(phoneNumber);
		setCustomerType(customerType);
		setPreviousOrderCount(previousOrderCount);
	}

	public String getCustomerId() {
		return customerId;
	}

	/**
	 * @throws IllegalArgumentException if the ID is null, blank or does not match C9999
	 */
	public void setCustomerId(String customerId) {

		if (customerId == null || customerId.trim().isEmpty())
			throw new IllegalArgumentException("Customer ID must not be empty");

		String cleanedId = customerId.trim().toUpperCase();
		if (!cleanedId.matches(CUSTOMER_ID_PATTERN))
			throw new IllegalArgumentException("Invalid customer ID format : " + customerId
					+ " (expected format is C followed by 4 digits, e.g. C0001)");

		this.customerId = cleanedId;
	}

	public String getName() {
		return name;
	}

	/**
	 * @throws IllegalArgumentException if the name is null, blank, shorter than 2
	 *         characters, longer than 50 characters, or contains a character that is
	 *         not a letter, space, apostrophe, full stop or hyphen
	 */
	public void setName(String name) {

		if (name == null || name.trim().isEmpty())
			throw new IllegalArgumentException("Customer name must not be empty");

		String cleanedName = name.trim();
		if (cleanedName.length() < MIN_NAME_LENGTH || cleanedName.length() > MAX_NAME_LENGTH)
			throw new IllegalArgumentException("Customer name must be between " + MIN_NAME_LENGTH
					+ " and " + MAX_NAME_LENGTH + " characters : " + name);

		if (!cleanedName.matches("[A-Za-z][A-Za-z .'-]*"))
			throw new IllegalArgumentException("Customer name contains invalid characters : " + name);

		this.name = cleanedName;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	/**
	 * @throws IllegalArgumentException if the email address is null, blank or not a
	 *         well formed address of the form local-part@domain.tld
	 */
	public void setEmailAddress(String emailAddress) {

		if (emailAddress == null || emailAddress.trim().isEmpty())
			throw new IllegalArgumentException("Email address must not be empty");

		String cleanedEmail = emailAddress.trim();
		if (!cleanedEmail.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"))
			throw new IllegalArgumentException("Invalid email address : " + emailAddress);

		this.emailAddress = cleanedEmail;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	/**
	 * Accepts Malaysian mobile numbers with or without a hyphen, for example
	 * 012-3456789 or 0123456789. The number is stored without the hyphen.
	 *
	 * @throws IllegalArgumentException if the phone number is null, blank or not a
	 *         valid 10 or 11 digit Malaysian mobile number beginning with 01
	 */
	public void setPhoneNumber(String phoneNumber) {

		if (phoneNumber == null || phoneNumber.trim().isEmpty())
			throw new IllegalArgumentException("Phone number must not be empty");

		String digitsOnly = phoneNumber.trim().replace("-", "").replace(" ", "");
		if (!digitsOnly.matches("01\\d{8,9}"))
			throw new IllegalArgumentException("Invalid phone number : " + phoneNumber
					+ " (expected a 10 or 11 digit number beginning with 01)");

		this.phoneNumber = digitsOnly;
	}

	public CustomerType getCustomerType() {
		return customerType;
	}

	/**
	 * @throws IllegalArgumentException if the customer type is null
	 */
	public void setCustomerType(CustomerType customerType) {

		if (customerType == null)
			throw new IllegalArgumentException("Customer type must not be null");

		this.customerType = customerType;
	}

	public int getPreviousOrderCount() {
		return previousOrderCount;
	}

	/**
	 * @throws IllegalArgumentException if the previous order count is negative
	 */
	public void setPreviousOrderCount(int previousOrderCount) {

		if (previousOrderCount < 0)
			throw new IllegalArgumentException("Previous order count must not be negative : "
					+ previousOrderCount);

		this.previousOrderCount = previousOrderCount;
	}

	/**
	 * Converts this customer into the single line record format used in customer.txt.
	 *
	 * @return customerId,name,emailAddress,phoneNumber,customerType,previousOrderCount
	 */
	public String toFileRecord() {
		return customerId + "," + name + "," + emailAddress + "," + phoneNumber + ","
				+ customerType.getDescription() + "," + previousOrderCount;
	}

	/**
	 * Two customers are considered equal when every stored attribute matches.
	 * Defining equals allows assertEquals and assertArrayEquals to be used directly
	 * on Customer objects in the test code.
	 */
	@Override
	public boolean equals(Object other) {

		if (this == other)
			return true;
		if (other == null || getClass() != other.getClass())
			return false;

		Customer otherCustomer = (Customer) other;
		return customerId.equals(otherCustomer.customerId)
				&& name.equals(otherCustomer.name)
				&& emailAddress.equals(otherCustomer.emailAddress)
				&& phoneNumber.equals(otherCustomer.phoneNumber)
				&& customerType == otherCustomer.customerType
				&& previousOrderCount == otherCustomer.previousOrderCount;
	}

	@Override
	public int hashCode() {
		return customerId.hashCode();
	}

	@Override
	public String toString() {
		return "Customer [" + customerId + ", " + name + ", " + emailAddress + ", " + phoneNumber
				+ ", " + customerType.getDescription() + ", previous orders = " + previousOrderCount + "]";
	}
}
