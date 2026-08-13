package my.edu.utar;

/**
 * Customer types recognised by the system, together with the discount rate that
 * each type attracts. Refer to Table 4 (Discounts) of the assignment specification.
 *
 * A Regular customer attracts no customer type discount, but may still qualify for
 * the additional order value and loyalty discounts handled by {@link ApplyDiscount}.
 */
public enum CustomerType {

	REGULAR("Regular", 0.00),
	STUDENT("Student", 0.10),
	CORPORATE("Corporate", 0.15);

	private final String description;
	private final double discountRate;

	private CustomerType(String description, double discountRate) {
		this.description = description;
		this.discountRate = discountRate;
	}

	public String getDescription() {
		return description;
	}

	/**
	 * @return the discount rate as a fraction, e.g. 0.10 for the 10% student discount
	 */
	public double getDiscountRate() {
		return discountRate;
	}

	/**
	 * Converts a text value into a CustomerType.
	 *
	 * @param value the customer type as text, case insensitive
	 * @return the matching CustomerType
	 * @throws IllegalArgumentException if the value is null, blank or not a supported customer type
	 */
	public static CustomerType fromString(String value) {

		if (value == null || value.trim().isEmpty())
			throw new IllegalArgumentException("Customer type must not be empty");

		String cleanedValue = value.trim();
		for (CustomerType customerType : values()) {
			if (customerType.description.equalsIgnoreCase(cleanedValue)
					|| customerType.name().equalsIgnoreCase(cleanedValue))
				return customerType;
		}
		throw new IllegalArgumentException("Invalid customer type : " + value);
	}

	@Override
	public String toString() {
		return description;
	}
}
