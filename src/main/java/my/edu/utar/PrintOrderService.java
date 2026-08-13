package my.edu.utar;

/**
 * FR1 - Create Print Order.
 *
 * This class is an ADDITION to the class list given in the assignment specification.
 * FR1 (Create Print Order) is a required feature but the specification does not name a
 * class for it, so the co-ordination logic that ties the individual modules together
 * lives here. The addition is recorded in the Assumptions and Changes section of the
 * report and is shown in the class diagram.
 *
 * The class carries out the end to end order flow:
 *
 *   1. select the customer, either an existing customer retrieved through readCustomer
 *      or a newly registered customer created through addNewCustomer;
 *   2. build the printOrder from the selected printing options;
 *   3. ask calculatePrintingCharge for the total charge, which in turn consults the
 *      external printerAvailability module and the applyDiscount module;
 *   4. ask generateInvoice for the invoice;
 *   5. hand the order to the external payment module and update the order status;
 *   6. hand the invoice to the external emailInvoice module when payment succeeded.
 *
 * Every collaborator is injected through the constructor, so the integration tests can
 * assemble the real, already unit tested modules while still replacing the three
 * external modules (printerAvailability, payment and emailInvoice) with test doubles.
 */
public class PrintOrderService {

	private ReadCustomer readCustomer;
	private AddNewCustomer addNewCustomer;
	private CalculatePrintingCharge calculatePrintingCharge;
	private GenerateInvoice generateInvoice;
	private Payment payment;
	private EmailInvoice emailInvoice;

	/**
	 * @throws IllegalArgumentException if any collaborator is null
	 */
	public PrintOrderService(ReadCustomer readCustomer, AddNewCustomer addNewCustomer,
			CalculatePrintingCharge calculatePrintingCharge, GenerateInvoice generateInvoice,
			Payment payment, EmailInvoice emailInvoice) {

		if (readCustomer == null)
			throw new IllegalArgumentException("ReadCustomer must not be null");
		if (addNewCustomer == null)
			throw new IllegalArgumentException("AddNewCustomer must not be null");
		if (calculatePrintingCharge == null)
			throw new IllegalArgumentException("CalculatePrintingCharge must not be null");
		if (generateInvoice == null)
			throw new IllegalArgumentException("GenerateInvoice must not be null");
		if (payment == null)
			throw new IllegalArgumentException("Payment must not be null");
		if (emailInvoice == null)
			throw new IllegalArgumentException("EmailInvoice must not be null");

		this.readCustomer = readCustomer;
		this.addNewCustomer = addNewCustomer;
		this.calculatePrintingCharge = calculatePrintingCharge;
		this.generateInvoice = generateInvoice;
		this.payment = payment;
		this.emailInvoice = emailInvoice;
	}

	/**
	 * Creates a print order for an EXISTING customer, whose details are retrieved from
	 * customer.txt using the customer ID, and calculates its total printing charge.
	 *
	 * @return the priced print order
	 * @throws IllegalArgumentException if the customer ID is invalid or unknown, or if
	 *         any printing option breaks a business rule
	 * @throws PrinterUnavailableException if no suitable printer is available
	 */
	public PrintOrder createOrderForExistingCustomer(String orderId, String customerId,
			PrintType printType, PaperSize paperSize, PrintingSide printingSide, int numberOfPages,
			int numberOfCopies, BindingOption bindingOption, boolean laminationRequired,
			boolean expressPrintingRequired) {

		Customer customer = readCustomer.getCustomerById(customerId);

		if (customer == null)
			throw new IllegalArgumentException("No customer is registered with the ID : " + customerId);

		return createOrder(orderId, customer, printType, paperSize, printingSide, numberOfPages,
				numberOfCopies, bindingOption, laminationRequired, expressPrintingRequired);
	}

	/**
	 * Registers a NEW customer, stores the details in customer.txt, then creates and
	 * prices a print order for that customer.
	 *
	 * @return the priced print order
	 * @throws IllegalArgumentException if any customer detail or printing option is invalid
	 * @throws PrinterUnavailableException if no suitable printer is available
	 */
	public PrintOrder createOrderForNewCustomer(String orderId, String name, String emailAddress,
			String phoneNumber, CustomerType customerType, PrintType printType, PaperSize paperSize,
			PrintingSide printingSide, int numberOfPages, int numberOfCopies,
			BindingOption bindingOption, boolean laminationRequired, boolean expressPrintingRequired) {

		Customer customer = addNewCustomer.addCustomer(name, emailAddress, phoneNumber, customerType);

		return createOrder(orderId, customer, printType, paperSize, printingSide, numberOfPages,
				numberOfCopies, bindingOption, laminationRequired, expressPrintingRequired);
	}

	/**
	 * Builds a print order for a known customer and prices it.
	 *
	 * @throws PrinterUnavailableException if no suitable printer is available, in which
	 *         case the order is left with the status Cancelled and no charge is calculated
	 */
	public PrintOrder createOrder(String orderId, Customer customer, PrintType printType,
			PaperSize paperSize, PrintingSide printingSide, int numberOfPages, int numberOfCopies,
			BindingOption bindingOption, boolean laminationRequired, boolean expressPrintingRequired) {

		PrintOrder printOrder = new PrintOrder(orderId, customer, printType, paperSize, printingSide,
				numberOfPages, numberOfCopies, bindingOption, laminationRequired,
				expressPrintingRequired);

		calculatePrintingCharge.calculateTotalCharge(printOrder);

		return printOrder;
	}

	/**
	 * Produces the invoice for a priced order.
	 *
	 * @throws IllegalArgumentException if the order is null or has not been priced
	 */
	public String issueInvoice(PrintOrder printOrder) {
		return generateInvoice.generateInvoice(printOrder);
	}

	/**
	 * Completes an order: takes payment through the external payment module, updates
	 * the order and payment status, and emails the invoice when payment succeeded.
	 *
	 * Following FR8, a successful payment sets the order status to Completed and an
	 * unsuccessful payment sets it to Pending Payment. Following FR9, the invoice is
	 * emailed ONLY when payment was successful.
	 *
	 * @param printOrder the priced order to complete
	 * @param paymentMethod the method chosen by the customer
	 * @return true when payment succeeded and the invoice was emailed
	 * @throws IllegalArgumentException if the order is null, has not been priced, or the
	 *         payment method is null
	 */
	public boolean completeOrder(PrintOrder printOrder, PaymentMethod paymentMethod) {

		if (printOrder == null)
			throw new IllegalArgumentException("Print order must not be null");
		if (!printOrder.isChargeCalculated())
			throw new IllegalArgumentException(
					"Cannot take payment before the printing charge has been calculated");
		if (paymentMethod == null)
			throw new IllegalArgumentException("Payment method must not be null");

		boolean paymentSuccessful = payment.makePayment(printOrder.getTotalPrintingCharge(),
				paymentMethod);

		if (!paymentSuccessful) {
			printOrder.setPaymentStatus(PaymentStatus.UNSUCCESSFUL);
			printOrder.setOrderStatus(OrderStatus.PENDING_PAYMENT);
			return false;
		}

		printOrder.setPaymentStatus(PaymentStatus.SUCCESSFUL);
		printOrder.setOrderStatus(OrderStatus.COMPLETED);

		String invoice = generateInvoice.generateInvoice(printOrder);
		return emailInvoice.sendInvoice(printOrder.getCustomer().getEmailAddress(), invoice);
	}

	public ReadCustomer getReadCustomer() {
		return readCustomer;
	}

	public AddNewCustomer getAddNewCustomer() {
		return addNewCustomer;
	}

	public CalculatePrintingCharge getCalculatePrintingCharge() {
		return calculatePrintingCharge;
	}

	public GenerateInvoice getGenerateInvoice() {
		return generateInvoice;
	}

	public Payment getPayment() {
		return payment;
	}

	public EmailInvoice getEmailInvoice() {
		return emailInvoice;
	}
}
