package my.edu.utar;

/**
 * Thrown when the external printer availability module reports that no suitable
 * printer is available for the selected paper size and print type.
 *
 * Appendix A of the specification states that in this situation the system shall
 * display the message "Selected printer is currently unavailable.", terminate the
 * print order creation process, not calculate the printing charges and not generate
 * the invoice. Throwing an unchecked exception from
 * {@link CalculatePrintingCharge#calculateTotalCharge(PrintOrder)} terminates the
 * calculation and lets the caller report the failure.
 */
public class PrinterUnavailableException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PrinterUnavailableException() {
		super(PrinterAvailability.UNAVAILABLE_MESSAGE);
	}

	public PrinterUnavailableException(String message) {
		super(message);
	}
}
