package my.edu.utar;

/**
 * FR1 - Create Print Order.
 *
 * Holds the details of a single print order together with the charges that have
 * been calculated for it.
 *
 * Order details : customer, print type, paper size, printing side, number of pages,
 * number of copies, binding option, lamination option and express printing option.
 *
 * Calculated values : base printing charge, additional service charges, discount
 * amount, total printing charge, order status and payment status.
 *
 * The business rules enforced by this class are:
 *   - at least one page and one copy must be printed
 *   - the maximum number of pages per print order is 500
 *   - the maximum number of copies per print order is 1,000
 *   - only one binding option may be selected for each print order (enforced by
 *     modelling binding as a single BindingOption value)
 */
public class PrintOrder {

	public static final int MIN_PAGES = 1;
	public static final int MAX_PAGES = 500;
	public static final int MIN_COPIES = 1;
	public static final int MAX_COPIES = 1000;

	private String orderId;
	private Customer customer;
	private PrintType printType;
	private PaperSize paperSize;
	private PrintingSide printingSide;
	private int numberOfPages;
	private int numberOfCopies;
	private BindingOption bindingOption;
	private boolean laminationRequired;
	private boolean expressPrintingRequired;

	private double baseCharge;
	private double optionalServiceCharge;
	private double discountAmount;
	private double totalPrintingCharge;
	private boolean chargeCalculated;

	private OrderStatus orderStatus;
	private PaymentStatus paymentStatus;

	/**
	 * Creates a print order with no optional services selected.
	 */
	public PrintOrder(String orderId, Customer customer, PrintType printType, PaperSize paperSize,
			PrintingSide printingSide, int numberOfPages, int numberOfCopies) {
		this(orderId, customer, printType, paperSize, printingSide, numberOfPages, numberOfCopies,
				BindingOption.NONE, false, false);
	}

	/**
	 * Creates a print order.
	 *
	 * @throws IllegalArgumentException if any supplied value breaks a business rule
	 */
	public PrintOrder(String orderId, Customer customer, PrintType printType, PaperSize paperSize,
			PrintingSide printingSide, int numberOfPages, int numberOfCopies,
			BindingOption bindingOption, boolean laminationRequired, boolean expressPrintingRequired) {

		setOrderId(orderId);
		setCustomer(customer);
		setPrintType(printType);
		setPaperSize(paperSize);
		setPrintingSide(printingSide);
		setNumberOfPages(numberOfPages);
		setNumberOfCopies(numberOfCopies);
		setBindingOption(bindingOption);
		this.laminationRequired = laminationRequired;
		this.expressPrintingRequired = expressPrintingRequired;

		this.baseCharge = 0.00;
		this.optionalServiceCharge = 0.00;
		this.discountAmount = 0.00;
		this.totalPrintingCharge = 0.00;
		this.chargeCalculated = false;
		this.orderStatus = OrderStatus.NEW;
		this.paymentStatus = PaymentStatus.UNPAID;
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {

		if (orderId == null || orderId.trim().isEmpty())
			throw new IllegalArgumentException("Order ID must not be empty");

		this.orderId = orderId.trim();
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {

		if (customer == null)
			throw new IllegalArgumentException("Customer must not be null");

		this.customer = customer;
	}

	public PrintType getPrintType() {
		return printType;
	}

	public void setPrintType(PrintType printType) {

		if (printType == null)
			throw new IllegalArgumentException("Print type must not be null");

		this.printType = printType;
	}

	public PaperSize getPaperSize() {
		return paperSize;
	}

	public void setPaperSize(PaperSize paperSize) {

		if (paperSize == null)
			throw new IllegalArgumentException("Paper size must not be null");

		this.paperSize = paperSize;
	}

	public PrintingSide getPrintingSide() {
		return printingSide;
	}

	public void setPrintingSide(PrintingSide printingSide) {

		if (printingSide == null)
			throw new IllegalArgumentException("Printing side must not be null");

		this.printingSide = printingSide;
	}

	public int getNumberOfPages() {
		return numberOfPages;
	}

	/**
	 * @throws IllegalArgumentException if the number of pages is below 1 or above 500
	 */
	public void setNumberOfPages(int numberOfPages) {

		if (numberOfPages < MIN_PAGES || numberOfPages > MAX_PAGES)
			throw new IllegalArgumentException("Number of pages must be between " + MIN_PAGES
					+ " and " + MAX_PAGES + " : " + numberOfPages);

		this.numberOfPages = numberOfPages;
	}

	public int getNumberOfCopies() {
		return numberOfCopies;
	}

	/**
	 * @throws IllegalArgumentException if the number of copies is below 1 or above 1000
	 */
	public void setNumberOfCopies(int numberOfCopies) {

		if (numberOfCopies < MIN_COPIES || numberOfCopies > MAX_COPIES)
			throw new IllegalArgumentException("Number of copies must be between " + MIN_COPIES
					+ " and " + MAX_COPIES + " : " + numberOfCopies);

		this.numberOfCopies = numberOfCopies;
	}

	public BindingOption getBindingOption() {
		return bindingOption;
	}

	public void setBindingOption(BindingOption bindingOption) {

		if (bindingOption == null)
			throw new IllegalArgumentException("Binding option must not be null");

		this.bindingOption = bindingOption;
	}

	public boolean isLaminationRequired() {
		return laminationRequired;
	}

	public void setLaminationRequired(boolean laminationRequired) {
		this.laminationRequired = laminationRequired;
	}

	public boolean isExpressPrintingRequired() {
		return expressPrintingRequired;
	}

	public void setExpressPrintingRequired(boolean expressPrintingRequired) {
		this.expressPrintingRequired = expressPrintingRequired;
	}

	/**
	 * @return the total number of printed pages, that is pages multiplied by copies.
	 *         Lamination is charged on this value.
	 */
	public int getTotalPrintedPages() {
		return numberOfPages * numberOfCopies;
	}

	public double getBaseCharge() {
		return baseCharge;
	}

	public void setBaseCharge(double baseCharge) {

		if (baseCharge < 0)
			throw new IllegalArgumentException("Base charge must not be negative : " + baseCharge);

		this.baseCharge = baseCharge;
	}

	public double getOptionalServiceCharge() {
		return optionalServiceCharge;
	}

	public void setOptionalServiceCharge(double optionalServiceCharge) {

		if (optionalServiceCharge < 0)
			throw new IllegalArgumentException("Optional service charge must not be negative : "
					+ optionalServiceCharge);

		this.optionalServiceCharge = optionalServiceCharge;
	}

	/**
	 * @return the subtotal, that is the base charge plus the optional service charges,
	 *         before any discount is applied
	 */
	public double getSubtotal() {
		return baseCharge + optionalServiceCharge;
	}

	public double getDiscountAmount() {
		return discountAmount;
	}

	public void setDiscountAmount(double discountAmount) {

		if (discountAmount < 0)
			throw new IllegalArgumentException("Discount amount must not be negative : " + discountAmount);

		this.discountAmount = discountAmount;
	}

	public double getTotalPrintingCharge() {
		return totalPrintingCharge;
	}

	public void setTotalPrintingCharge(double totalPrintingCharge) {

		if (totalPrintingCharge < 0)
			throw new IllegalArgumentException("Total printing charge must not be negative : "
					+ totalPrintingCharge);

		this.totalPrintingCharge = totalPrintingCharge;
		this.chargeCalculated = true;
	}

	/**
	 * @return true once the total printing charge has been calculated for this order
	 */
	public boolean isChargeCalculated() {
		return chargeCalculated;
	}

	public OrderStatus getOrderStatus() {
		return orderStatus;
	}

	public void setOrderStatus(OrderStatus orderStatus) {

		if (orderStatus == null)
			throw new IllegalArgumentException("Order status must not be null");

		this.orderStatus = orderStatus;
	}

	public PaymentStatus getPaymentStatus() {
		return paymentStatus;
	}

	public void setPaymentStatus(PaymentStatus paymentStatus) {

		if (paymentStatus == null)
			throw new IllegalArgumentException("Payment status must not be null");

		this.paymentStatus = paymentStatus;
	}

	@Override
	public String toString() {
		return "PrintOrder [" + orderId + ", " + customer.getCustomerId() + ", " + printType
				+ ", " + paperSize + ", " + printingSide + ", " + numberOfPages + " pages, "
				+ numberOfCopies + " copies, " + bindingOption
				+ ", lamination = " + laminationRequired
				+ ", express = " + expressPrintingRequired
				+ ", total = RM" + String.format("%.2f", totalPrintingCharge)
				+ ", " + orderStatus + ", " + paymentStatus + "]";
	}
}
