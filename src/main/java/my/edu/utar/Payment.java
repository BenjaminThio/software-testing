package my.edu.utar;

/**
 * FR8 - Payment (Not to be developed).
 *
 * Payment can be made using e-Wallet, Credit Card or Online Banking. The class stores
 * the payment amount, payment method and payment status. Once payment is successful
 * the order status is updated to Completed; if payment is unsuccessful the order
 * status is updated to Pending Payment.
 *
 * As instructed by the assignment specification, only the required method signatures
 * are declared here. The method bodies are NOT implemented because this class is used
 * as a test double during testing. Every method therefore throws
 * UnsupportedOperationException, which makes it obvious that any test relying on real
 * behaviour has forgotten to substitute a test double.
 */
public class Payment {

	/**
	 * Makes a payment for the given amount using the given payment method.
	 *
	 * @param amount the amount to be paid
	 * @param paymentMethod the method chosen by the customer
	 * @return true when the payment is successful
	 */
	public boolean makePayment(double amount, PaymentMethod paymentMethod) {
		throw new UnsupportedOperationException(
				"payment is not to be developed in this assignment and must be replaced by a test double");
	}

	/**
	 * @return the amount of the most recent payment
	 */
	public double getPaymentAmount() {
		throw new UnsupportedOperationException(
				"payment is not to be developed in this assignment and must be replaced by a test double");
	}

	/**
	 * @return the method used for the most recent payment
	 */
	public PaymentMethod getPaymentMethod() {
		throw new UnsupportedOperationException(
				"payment is not to be developed in this assignment and must be replaced by a test double");
	}

	/**
	 * @return the status of the most recent payment
	 */
	public PaymentStatus getPaymentStatus() {
		throw new UnsupportedOperationException(
				"payment is not to be developed in this assignment and must be replaced by a test double");
	}
}
