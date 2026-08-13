package my.edu.utar;

/**
 * Life cycle states of a print order.
 *
 * NEW               - the order has been created but the charge has not been calculated yet.
 * CONFIRMED         - the printer is available and the total printing charge has been calculated.
 * PENDING_PAYMENT   - payment was attempted but was not successful.
 * COMPLETED         - payment was successful.
 * CANCELLED         - the order could not proceed, e.g. no suitable printer was available.
 */
public enum OrderStatus {

	NEW("New"),
	CONFIRMED("Confirmed"),
	PENDING_PAYMENT("Pending Payment"),
	COMPLETED("Completed"),
	CANCELLED("Cancelled");

	private final String description;

	private OrderStatus(String description) {
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
