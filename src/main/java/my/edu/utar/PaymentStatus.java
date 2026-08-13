package my.edu.utar;

/**
 * Payment states of a print order.
 */
public enum PaymentStatus {

	UNPAID("Unpaid"),
	SUCCESSFUL("Successful"),
	UNSUCCESSFUL("Unsuccessful");

	private final String description;

	private PaymentStatus(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public String toString() {
		return description;
	}
}
