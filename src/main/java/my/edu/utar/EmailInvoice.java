package my.edu.utar;

/**
 * FR9 - Email Invoice (Not to be developed).
 *
 * Once payment is successful an invoice is emailed to the customer. The email
 * contains the customer details, the print order details, a breakdown of the printing
 * charges, the discounts applied and the final amount paid.
 *
 * As instructed by the assignment specification, only the required method signatures
 * are declared here. The method bodies are NOT implemented because this class is used
 * as a test double during testing.
 */
public class EmailInvoice {

	/**
	 * Emails the invoice to the customer.
	 *
	 * @param emailAddress the recipient's email address
	 * @param invoiceContent the invoice to be sent, in PDF format in the live system
	 * @return true when the email has been sent successfully
	 */
	public boolean sendInvoice(String emailAddress, String invoiceContent) {
		throw new UnsupportedOperationException(
				"emailInvoice is not to be developed in this assignment and must be replaced by a test double");
	}

	/**
	 * @return true if the most recent invoice email was sent successfully
	 */
	public boolean isEmailSent() {
		throw new UnsupportedOperationException(
				"emailInvoice is not to be developed in this assignment and must be replaced by a test double");
	}
}
