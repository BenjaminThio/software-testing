package my.edu.utar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.FileParameters;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.mappers.CsvWithHeaderMapper;

/**
 * Unit tests for GenerateInvoice (FR7 - Generate Invoice).
 *
 * FR7 requires the invoice to contain the customer details, the print order details,
 * a breakdown of the printing charges, the optional service charges, the discounts
 * applied and the final total amount payable. Each of those six requirements is
 * asserted separately, so a failure points straight at the missing section.
 *
 * Test design techniques applied in this class:
 *   - Equivalence Partitioning on the state of the order: priced (valid) and unpriced
 *     or null (invalid);
 *   - the charge figures printed on the invoice are READ FROM A TEXT FILE
 *     (total-charge-test-data.csv), the same file that drives the charge calculation
 *     tests, so the invoice and the calculator can never drift apart;
 *   - a STUBBED printer availability module is used to price the orders, because
 *     pricing is a precondition of invoicing rather than the subject of these tests.
 */
@RunWith(JUnitParamsRunner.class)
public class GenerateInvoiceUnitTests {

	private GenerateInvoice generateInvoice;
	private CalculatePrintingCharge calculatePrintingCharge;
	private PrinterAvailabilityService printerAvailabilityStub;

	@Before
	public void setupForAllTests() {

		generateInvoice = new GenerateInvoice();

		// STUB : a printer is always available, so every order in this class can be priced
		printerAvailabilityStub = mock(PrinterAvailabilityService.class);
		when(printerAvailabilityStub.isPrinterAvailable(anyString(), anyString())).thenReturn(true);

		calculatePrintingCharge = new CalculatePrintingCharge(printerAvailabilityStub,
				new ApplyDiscount());
	}

	/**
	 * Builds and prices the worked example from the assignment guideline:
	 * a Student customer, Colour A3 double-sided, 50 pages, 2 copies, spiral binding
	 * and lamination. Base RM140.00 + optional RM158.00 = RM298.00 subtotal,
	 * student discount RM29.80, total RM268.20.
	 */
	private PrintOrder buildAndPriceTheWorkedExample() {

		Customer customer = new Customer("C0001", "Ali Bin Ahmad", "ali.ahmad@example.com",
				"0123456789", CustomerType.STUDENT, 3);

		PrintOrder printOrder = new PrintOrder("ORD001", customer, PrintType.COLOUR, PaperSize.A3,
				PrintingSide.DOUBLE_SIDED, 50, 2, BindingOption.SPIRAL_BINDING, true, false);

		calculatePrintingCharge.calculateTotalCharge(printOrder);
		return printOrder;
	}

	// ----------------------------------------------------------------- VALID CASES

	/**
	 * VALID CASE - the invoice carries the CUSTOMER DETAILS required by FR7.
	 */
	@Test
	public void testInvoiceContainsTheCustomerDetails() {

		String invoice = generateInvoice.generateInvoice(buildAndPriceTheWorkedExample());

		assertNotNull(invoice);
		assertTrue("customer id", invoice.contains("C0001"));
		assertTrue("name", invoice.contains("Ali Bin Ahmad"));
		assertTrue("email address", invoice.contains("ali.ahmad@example.com"));
		assertTrue("phone number", invoice.contains("0123456789"));
		assertTrue("customer type", invoice.contains("Student"));
	}

	/**
	 * VALID CASE - the invoice carries the PRINT ORDER DETAILS required by FR7.
	 */
	@Test
	public void testInvoiceContainsThePrintOrderDetails() {

		String invoice = generateInvoice.generateInvoice(buildAndPriceTheWorkedExample());

		assertTrue("order id", invoice.contains("ORD001"));
		assertTrue("print type", invoice.contains("Colour"));
		assertTrue("paper size", invoice.contains("A3"));
		assertTrue("printing side", invoice.contains("Double-sided"));
		assertTrue("number of pages", invoice.contains("Pages         : 50"));
		assertTrue("number of copies", invoice.contains("Copies        : 2"));
		assertTrue("binding option", invoice.contains("Spiral Binding"));
		assertTrue("lamination", invoice.contains("Lamination    : Yes"));
		assertTrue("express printing", invoice.contains("Express       : No"));
	}

	/**
	 * VALID CASE - the invoice carries the CHARGE BREAKDOWN, the OPTIONAL SERVICE
	 * CHARGES, the DISCOUNT APPLIED and the FINAL TOTAL required by FR7.
	 */
	@Test
	public void testInvoiceContainsTheFullChargeBreakdown() {

		String invoice = generateInvoice.generateInvoice(buildAndPriceTheWorkedExample());

		assertTrue("base charge line", invoice.contains(formatAmount("Base Printing Charge", 140.00)));
		assertTrue("optional service line",
				invoice.contains(formatAmount("Optional Services", 158.00)));
		assertTrue("subtotal line", invoice.contains(formatAmount("Subtotal", 298.00)));
		assertTrue("discount line", invoice.contains(formatAmount("Discount", -29.80)));
		assertTrue("total line", invoice.contains(formatAmount("TOTAL AMOUNT PAYABLE", 268.20)));
	}

	/**
	 * VALID CASE - parameterised, test data read from an external text file.
	 * Every row of total-charge-test-data.csv is priced and invoiced, and the invoice
	 * is checked against the base charge, optional service charge, subtotal, discount
	 * and total held in the file.
	 */
	@Test
	@FileParameters(value = "total-charge-test-data.csv", mapper = CsvWithHeaderMapper.class)
	public void testInvoiceReportsTheCalculatedCharges(String customerType, int previousOrderCount,
			String paperSize, String printType, String printingSide, int numberOfPages,
			int numberOfCopies, String bindingOption, boolean lamination, boolean express,
			double expectedBaseCharge, double expectedOptionalCharge, double expectedSubtotal,
			double expectedDiscount, double expectedTotal) {

		Customer customer = new Customer("C0001", "Test Customer", "test@example.com",
				"0123334444", CustomerType.fromString(customerType), previousOrderCount);

		PrintOrder printOrder = new PrintOrder("ORD001", customer, PrintType.fromString(printType),
				PaperSize.fromString(paperSize), PrintingSide.fromString(printingSide),
				numberOfPages, numberOfCopies, BindingOption.fromString(bindingOption), lamination,
				express);

		calculatePrintingCharge.calculateTotalCharge(printOrder);
		String invoice = generateInvoice.generateInvoice(printOrder);

		assertTrue("base charge", invoice.contains(formatAmount("Base Printing Charge",
				expectedBaseCharge)));
		assertTrue("optional services", invoice.contains(formatAmount("Optional Services",
				expectedOptionalCharge)));
		assertTrue("subtotal", invoice.contains(formatAmount("Subtotal", expectedSubtotal)));
		assertTrue("discount", invoice.contains(formatAmount("Discount", -expectedDiscount)));
		assertTrue("total", invoice.contains(formatAmount("TOTAL AMOUNT PAYABLE", expectedTotal)));
	}

	/**
	 * VALID CASE - the invoice number is derived from the order ID.
	 */
	@Test
	@Parameters({ "ORD001, INV-ORD001",
			"ORD002, INV-ORD002",
			"PM-2026-0001, INV-PM-2026-0001" })
	public void testGetInvoiceNumber(String orderId, String expectedInvoiceNumber) {

		Customer customer = new Customer("C0001", "Ali Bin Ahmad", "ali.ahmad@example.com",
				"0123456789", CustomerType.STUDENT, 3);
		PrintOrder printOrder = new PrintOrder(orderId, customer, PrintType.COLOUR, PaperSize.A4,
				PrintingSide.SINGLE_SIDED, 10, 1);

		assertEquals(expectedInvoiceNumber, generateInvoice.getInvoiceNumber(printOrder));
	}

	/**
	 * VALID CASE - the invoice reports the order and payment statuses, which tells the
	 * customer whether the amount shown is still outstanding.
	 */
	@Test
	@Parameters({ "CONFIRMED, UNPAID, Confirmed, Unpaid",
			"COMPLETED, SUCCESSFUL, Completed, Successful",
			"PENDING_PAYMENT, UNSUCCESSFUL, Pending Payment, Unsuccessful" })
	public void testInvoiceReportsTheOrderAndPaymentStatus(String orderStatus,
			String paymentStatus, String expectedOrderText, String expectedPaymentText) {

		PrintOrder printOrder = buildAndPriceTheWorkedExample();
		printOrder.setOrderStatus(OrderStatus.valueOf(orderStatus));
		printOrder.setPaymentStatus(PaymentStatus.valueOf(paymentStatus));

		String invoice = generateInvoice.generateInvoice(printOrder);

		assertTrue(invoice.contains("Order Status   : " + expectedOrderText));
		assertTrue(invoice.contains("Payment Status : " + expectedPaymentText));
	}

	/**
	 * VALID CASE - an order with no optional services and no discount still produces a
	 * complete invoice, with zero shown rather than the lines being omitted.
	 */
	@Test
	public void testInvoiceForAnOrderWithNoServicesAndNoDiscount() {

		Customer customer = new Customer("C0002", "Siti Nurhaliza", "siti.n@example.com",
				"0139876543", CustomerType.REGULAR, 12);
		PrintOrder printOrder = new PrintOrder("ORD002", customer, PrintType.BLACK_AND_WHITE,
				PaperSize.A4, PrintingSide.SINGLE_SIDED, 20, 2);

		calculatePrintingCharge.calculateTotalCharge(printOrder);
		String invoice = generateInvoice.generateInvoice(printOrder);

		assertTrue(invoice.contains(formatAmount("Base Printing Charge", 8.00)));
		assertTrue(invoice.contains(formatAmount("Optional Services", 0.00)));
		assertTrue(invoice.contains(formatAmount("Subtotal", 8.00)));
		assertTrue(invoice.contains(formatAmount("TOTAL AMOUNT PAYABLE", 8.00)));
		assertTrue(invoice.contains("Binding       : None"));
		assertTrue(invoice.contains("Lamination    : No"));
	}

	// --------------------------------------------------------------- INVALID CASES

	/**
	 * INVALID CASE - Appendix A states that no invoice is generated when the printing
	 * charge was not calculated, for example because no suitable printer was available.
	 * A brand new order has not been priced, so it must be refused.
	 */
	@Test
	public void testInvoiceIsRefusedForAnUnpricedOrder() {

		Customer customer = new Customer("C0001", "Ali Bin Ahmad", "ali.ahmad@example.com",
				"0123456789", CustomerType.STUDENT, 3);
		PrintOrder unpricedOrder = new PrintOrder("ORD001", customer, PrintType.COLOUR,
				PaperSize.A4, PrintingSide.SINGLE_SIDED, 10, 1);

		assertFalse(unpricedOrder.isChargeCalculated());

		try {
			generateInvoice.generateInvoice(unpricedOrder);
			org.junit.Assert.fail("An IllegalArgumentException was expected for an unpriced order");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("printing charge has been calculated"));
		}
	}

	/**
	 * INVALID CASE - an order that was cancelled because the printer was unavailable is
	 * never priced, so it can never be invoiced either.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testInvoiceIsRefusedForAnOrderCancelledByAnUnavailablePrinter() {

		PrinterAvailabilityService unavailablePrinterStub = mock(PrinterAvailabilityService.class);
		when(unavailablePrinterStub.isPrinterAvailable(anyString(), anyString())).thenReturn(false);

		Customer customer = new Customer("C0001", "Ali Bin Ahmad", "ali.ahmad@example.com",
				"0123456789", CustomerType.STUDENT, 3);
		PrintOrder printOrder = new PrintOrder("ORD001", customer, PrintType.COLOUR, PaperSize.A4,
				PrintingSide.SINGLE_SIDED, 10, 1);

		try {
			new CalculatePrintingCharge(unavailablePrinterStub, new ApplyDiscount())
					.calculateTotalCharge(printOrder);
		} catch (PrinterUnavailableException e) {
			// expected : the order has been abandoned
		}

		generateInvoice.generateInvoice(printOrder);
	}

	/**
	 * INVALID CASE - a null order cannot be invoiced, and neither can its invoice
	 * number be derived.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters({ "generateInvoice", "getInvoiceNumber" })
	public void testNullOrderIsRejected(String methodUnderTest) {

		if (methodUnderTest.equals("generateInvoice"))
			generateInvoice.generateInvoice(null);
		else
			generateInvoice.getInvoiceNumber(null);
	}

	// ---------------------------------------------------------------------- HELPER

	/**
	 * Rebuilds one charge line exactly as GenerateInvoice formats it, so the test
	 * asserts on the real output rather than on a loose substring.
	 */
	private String formatAmount(String label, double amount) {
		return String.format("%-24s : RM%.2f", label, amount);
	}
}
