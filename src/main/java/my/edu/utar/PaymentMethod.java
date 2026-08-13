package my.edu.utar;

/**
 * Payment methods accepted by the (external) payment module.
 * Refer to FR8 of the assignment specification.
 */
public enum PaymentMethod {

	E_WALLET("e-Wallet"),
	CREDIT_CARD("Credit Card"),
	ONLINE_BANKING("Online Banking");

	private final String description;

	private PaymentMethod(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	/**
	 * Converts a text value into a PaymentMethod.
	 *
	 * @param value the payment method as text, case insensitive
	 * @return the matching PaymentMethod
	 * @throws IllegalArgumentException if the value is null, blank or not a supported payment method
	 */
	public static PaymentMethod fromString(String value) {

		if (value == null || value.trim().isEmpty())
			throw new IllegalArgumentException("Payment method must not be empty");

		String cleanedValue = value.trim();
		for (PaymentMethod paymentMethod : values()) {
			if (paymentMethod.description.equalsIgnoreCase(cleanedValue)
					|| paymentMethod.name().equalsIgnoreCase(cleanedValue))
				return paymentMethod;
		}
		throw new IllegalArgumentException("Invalid payment method : " + value);
	}

	@Override
	public String toString() {
		return description;
	}
}
