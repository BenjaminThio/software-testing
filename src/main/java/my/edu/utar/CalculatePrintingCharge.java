package my.edu.utar;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * FR5 - Calculate Printing Charges.
 *
 * Calculates the total printing charge for a print order based on the print type,
 * paper size, printing side, number of pages, number of copies and the selected
 * optional services.
 *
 * The class collaborates with two other modules:
 *   - {@link PrinterAvailabilityService} (external, mocked) is asked whether a
 *     suitable printer is available before any charge is calculated;
 *   - {@link ApplyDiscount} supplies the applicable customer discount.
 *
 * Both collaborators are injected through the constructor so that the test code can
 * replace them with Mockito test doubles.
 *
 * Order of calculation, following the business rules:
 *   1. check printer availability, abandon the order if no printer is available
 *   2. base charge     = base rate x number of pages x number of copies
 *   3. optional charge = binding + lamination + express printing
 *   4. subtotal        = base charge + optional charge
 *   5. discount        = supplied by applyDiscount, applied to the subtotal
 *   6. total           = subtotal - discount, rounded to two decimal places
 */
public class CalculatePrintingCharge {

	/** Table 3 - lamination is charged per printed page. */
	public static final double LAMINATION_CHARGE_PER_PAGE = 1.50;

	/** Table 3 - express printing is a flat charge per order. */
	public static final double EXPRESS_PRINTING_CHARGE = 20.00;

	/**
	 * Table 2 - Base Printing Charges (per page).
	 * First index  : paper size   (A3, A4, A5    - PaperSize.ordinal())
	 * Second index : print type   (B&W, Colour   - PrintType.ordinal())
	 * Third index  : printing side (Single, Double - PrintingSide.ordinal())
	 */
	private static final double[][][] BASE_RATES = {
			// A3 : B&W single 0.40, B&W double 0.35, Colour single 1.50, Colour double 1.40
			{ { 0.40, 0.35 }, { 1.50, 1.40 } },
			// A4 : B&W single 0.20, B&W double 0.18, Colour single 0.80, Colour double 0.75
			{ { 0.20, 0.18 }, { 0.80, 0.75 } },
			// A5 : B&W single 0.15, B&W double 0.13, Colour single 0.60, Colour double 0.55
			{ { 0.15, 0.13 }, { 0.60, 0.55 } }
	};

	private PrinterAvailabilityService printerAvailabilityService;
	private ApplyDiscount applyDiscount;

	/**
	 * @param printerAvailabilityService the external printer availability module
	 * @param applyDiscount the discount calculation module
	 * @throws IllegalArgumentException if either collaborator is null
	 */
	public CalculatePrintingCharge(PrinterAvailabilityService printerAvailabilityService,
			ApplyDiscount applyDiscount) {

		if (printerAvailabilityService == null)
			throw new IllegalArgumentException("Printer availability service must not be null");
		if (applyDiscount == null)
			throw new IllegalArgumentException("ApplyDiscount must not be null");

		this.printerAvailabilityService = printerAvailabilityService;
		this.applyDiscount = applyDiscount;
	}

	public PrinterAvailabilityService getPrinterAvailabilityService() {
		return printerAvailabilityService;
	}

	public ApplyDiscount getApplyDiscount() {
		return applyDiscount;
	}

	/**
	 * Looks up the per page base rate from Table 2.
	 *
	 * @return the base rate in Ringgit Malaysia for one page
	 * @throws IllegalArgumentException if any argument is null
	 */
	public double getBaseRate(PaperSize paperSize, PrintType printType, PrintingSide printingSide) {

		if (paperSize == null)
			throw new IllegalArgumentException("Paper size must not be null");
		if (printType == null)
			throw new IllegalArgumentException("Print type must not be null");
		if (printingSide == null)
			throw new IllegalArgumentException("Printing side must not be null");

		return BASE_RATES[paperSize.ordinal()][printType.ordinal()][printingSide.ordinal()];
	}

	/**
	 * Text based form of {@link #getBaseRate(PaperSize, PrintType, PrintingSide)}.
	 * Used by the file driven parameterised tests, which read the printing options as
	 * text from a CSV data file.
	 *
	 * @throws IllegalArgumentException if any value is empty or not a supported option
	 */
	public double getBaseRate(String paperSize, String printType, String printingSide) {
		return getBaseRate(PaperSize.fromString(paperSize), PrintType.fromString(printType),
				PrintingSide.fromString(printingSide));
	}

	/**
	 * Calculates the base printing charge.
	 *
	 *   Base Charge = Base Rate x Number of Pages x Number of Copies
	 *
	 * @throws IllegalArgumentException if any option is null, or if the number of pages
	 *         or copies falls outside the permitted range
	 */
	public double calculateBaseCharge(PaperSize paperSize, PrintType printType,
			PrintingSide printingSide, int numberOfPages, int numberOfCopies) {

		validatePagesAndCopies(numberOfPages, numberOfCopies);

		double baseRate = getBaseRate(paperSize, printType, printingSide);
		return roundToTwoDecimals(baseRate * numberOfPages * numberOfCopies);
	}

	/**
	 * Text based form of
	 * {@link #calculateBaseCharge(PaperSize, PrintType, PrintingSide, int, int)},
	 * used by the file driven parameterised tests.
	 */
	public double calculateBaseCharge(String paperSize, String printType, String printingSide,
			int numberOfPages, int numberOfCopies) {
		return calculateBaseCharge(PaperSize.fromString(paperSize), PrintType.fromString(printType),
				PrintingSide.fromString(printingSide), numberOfPages, numberOfCopies);
	}

	/**
	 * Calculates the total additional charge for the selected optional services,
	 * following Table 3.
	 *
	 *   binding    - one flat charge, only one binding option may be selected
	 *   lamination - RM1.50 for every printed page, that is pages x copies
	 *   express    - RM20.00 per order
	 *
	 * @throws IllegalArgumentException if the binding option is null, or if the number
	 *         of pages or copies falls outside the permitted range
	 */
	public double calculateOptionalServiceCharge(BindingOption bindingOption,
			boolean laminationRequired, boolean expressPrintingRequired, int numberOfPages,
			int numberOfCopies) {

		if (bindingOption == null)
			throw new IllegalArgumentException("Binding option must not be null");

		validatePagesAndCopies(numberOfPages, numberOfCopies);

		double charge = bindingOption.getCharge();

		if (laminationRequired)
			charge = charge + (LAMINATION_CHARGE_PER_PAGE * numberOfPages * numberOfCopies);

		if (expressPrintingRequired)
			charge = charge + EXPRESS_PRINTING_CHARGE;

		return roundToTwoDecimals(charge);
	}

	/**
	 * Text based form of
	 * {@link #calculateOptionalServiceCharge(BindingOption, boolean, boolean, int, int)},
	 * used by the file driven parameterised tests.
	 */
	public double calculateOptionalServiceCharge(String bindingOption, boolean laminationRequired,
			boolean expressPrintingRequired, int numberOfPages, int numberOfCopies) {
		return calculateOptionalServiceCharge(BindingOption.fromString(bindingOption),
				laminationRequired, expressPrintingRequired, numberOfPages, numberOfCopies);
	}

	/**
	 * Calculates the total printing charge for a complete print order.
	 *
	 * The external printer availability module is consulted first. When it reports
	 * that no suitable printer is available the order is cancelled, no charge is
	 * calculated and a {@link PrinterUnavailableException} is thrown, as required by
	 * Appendix A of the specification.
	 *
	 * When the calculation succeeds the base charge, optional service charge, discount
	 * amount, total printing charge and order status of the supplied order are updated.
	 *
	 * @param printOrder the order to price
	 * @return the total printing charge, rounded to two decimal places
	 * @throws IllegalArgumentException if the order is null
	 * @throws PrinterUnavailableException if no suitable printer is available
	 */
	public double calculateTotalCharge(PrintOrder printOrder) {

		if (printOrder == null)
			throw new IllegalArgumentException("Print order must not be null");

		// 1. the external module decides whether the order may proceed at all
		boolean printerAvailable = printerAvailabilityService.isPrinterAvailable(
				printOrder.getPaperSize().getCode(), printOrder.getPrintType().getDescription());

		if (!printerAvailable) {
			printOrder.setOrderStatus(OrderStatus.CANCELLED);
			throw new PrinterUnavailableException();
		}

		// 2. base printing charge
		double baseCharge = calculateBaseCharge(printOrder.getPaperSize(), printOrder.getPrintType(),
				printOrder.getPrintingSide(), printOrder.getNumberOfPages(),
				printOrder.getNumberOfCopies());

		// 3. optional service charges
		double optionalCharge = calculateOptionalServiceCharge(printOrder.getBindingOption(),
				printOrder.isLaminationRequired(), printOrder.isExpressPrintingRequired(),
				printOrder.getNumberOfPages(), printOrder.getNumberOfCopies());

		// 4. subtotal
		double subtotal = roundToTwoDecimals(baseCharge + optionalCharge);

		// 5. discount, obtained from the applyDiscount module
		double discount = applyDiscount.calculateDiscount(printOrder.getCustomer(), subtotal);

		// 6. total printing charge
		double total = roundToTwoDecimals(subtotal - discount);

		printOrder.setBaseCharge(baseCharge);
		printOrder.setOptionalServiceCharge(optionalCharge);
		printOrder.setDiscountAmount(discount);
		printOrder.setTotalPrintingCharge(total);
		printOrder.setOrderStatus(OrderStatus.CONFIRMED);

		return total;
	}

	/**
	 * Enforces the business rules on the number of pages and copies.
	 *
	 * @throws IllegalArgumentException if pages are outside 1 to 500 or copies are
	 *         outside 1 to 1000
	 */
	private void validatePagesAndCopies(int numberOfPages, int numberOfCopies) {

		if (numberOfPages < PrintOrder.MIN_PAGES || numberOfPages > PrintOrder.MAX_PAGES)
			throw new IllegalArgumentException("Number of pages must be between "
					+ PrintOrder.MIN_PAGES + " and " + PrintOrder.MAX_PAGES + " : " + numberOfPages);

		if (numberOfCopies < PrintOrder.MIN_COPIES || numberOfCopies > PrintOrder.MAX_COPIES)
			throw new IllegalArgumentException("Number of copies must be between "
					+ PrintOrder.MIN_COPIES + " and " + PrintOrder.MAX_COPIES + " : " + numberOfCopies);
	}

	/**
	 * Rounds a monetary amount to two decimal places using half up rounding.
	 */
	public static double roundToTwoDecimals(double value) {
		return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}
}
