package my.edu.utar;

/**
 * FR7 - Generate Invoice.
 *
 * Receives a completed print order and produces an invoice containing the customer
 * details, the print order details, a breakdown of the printing charges, the optional
 * service charges, the discounts applied and the final total amount payable.
 *
 * The invoice is returned as a formatted String. Returning text rather than writing
 * to the screen keeps the method easy to verify with assertions in the test code.
 */
public class GenerateInvoice {

	public static final String INVOICE_PREFIX = "INV-";
	public static final String LINE = "==================================================";

	/**
	 * Builds the invoice number for an order, e.g. order ORD001 yields INV-ORD001.
	 *
	 * @throws IllegalArgumentException if the order is null
	 */
	public String getInvoiceNumber(PrintOrder printOrder) {

		if (printOrder == null)
			throw new IllegalArgumentException("Print order must not be null");

		return INVOICE_PREFIX + printOrder.getOrderId();
	}

	/**
	 * Generates the invoice for a completed print order.
	 *
	 * @param printOrder the order to invoice, whose charges have already been calculated
	 * @return the formatted invoice
	 * @throws IllegalArgumentException if the order is null or its printing charge has
	 *         not been calculated yet
	 */
	public String generateInvoice(PrintOrder printOrder) {

		if (printOrder == null)
			throw new IllegalArgumentException("Print order must not be null");

		// Appendix A : no invoice is generated when the charge was never calculated,
		// for example because no suitable printer was available.
		if (!printOrder.isChargeCalculated())
			throw new IllegalArgumentException(
					"Cannot generate an invoice before the printing charge has been calculated");

		Customer customer = printOrder.getCustomer();
		StringBuilder invoice = new StringBuilder();

		invoice.append(LINE).append("\n");
		invoice.append("PRINTMASTER PRINTING SERVICES\n");
		invoice.append("INVOICE ").append(getInvoiceNumber(printOrder)).append("\n");
		invoice.append(LINE).append("\n");

		invoice.append("CUSTOMER DETAILS\n");
		invoice.append("Customer ID   : ").append(customer.getCustomerId()).append("\n");
		invoice.append("Name          : ").append(customer.getName()).append("\n");
		invoice.append("Email         : ").append(customer.getEmailAddress()).append("\n");
		invoice.append("Phone         : ").append(customer.getPhoneNumber()).append("\n");
		invoice.append("Customer Type : ").append(customer.getCustomerType().getDescription()).append("\n");
		invoice.append(LINE).append("\n");

		invoice.append("PRINT ORDER DETAILS\n");
		invoice.append("Order ID      : ").append(printOrder.getOrderId()).append("\n");
		invoice.append("Print Type    : ").append(printOrder.getPrintType().getDescription()).append("\n");
		invoice.append("Paper Size    : ").append(printOrder.getPaperSize().getCode()).append("\n");
		invoice.append("Printing Side : ").append(printOrder.getPrintingSide().getDescription()).append("\n");
		invoice.append("Pages         : ").append(printOrder.getNumberOfPages()).append("\n");
		invoice.append("Copies        : ").append(printOrder.getNumberOfCopies()).append("\n");
		invoice.append("Binding       : ").append(printOrder.getBindingOption().getDescription()).append("\n");
		invoice.append("Lamination    : ").append(printOrder.isLaminationRequired() ? "Yes" : "No").append("\n");
		invoice.append("Express       : ").append(printOrder.isExpressPrintingRequired() ? "Yes" : "No").append("\n");
		invoice.append(LINE).append("\n");

		invoice.append("CHARGES\n");
		invoice.append(formatChargeLine("Base Printing Charge", printOrder.getBaseCharge()));
		invoice.append(formatChargeLine("Optional Services", printOrder.getOptionalServiceCharge()));
		invoice.append(formatChargeLine("Subtotal", printOrder.getSubtotal()));
		invoice.append(formatChargeLine("Discount", -printOrder.getDiscountAmount()));
		invoice.append(LINE).append("\n");
		invoice.append(formatChargeLine("TOTAL AMOUNT PAYABLE", printOrder.getTotalPrintingCharge()));
		invoice.append(LINE).append("\n");

		invoice.append("Order Status   : ").append(printOrder.getOrderStatus().getDescription()).append("\n");
		invoice.append("Payment Status : ").append(printOrder.getPaymentStatus().getDescription()).append("\n");
		invoice.append(LINE).append("\n");

		return invoice.toString();
	}

	/**
	 * Formats one charge line of the invoice, e.g. "Subtotal ......... RM298.00".
	 */
	private String formatChargeLine(String label, double amount) {
		return String.format("%-24s : RM%.2f", label, amount) + "\n";
	}
}
