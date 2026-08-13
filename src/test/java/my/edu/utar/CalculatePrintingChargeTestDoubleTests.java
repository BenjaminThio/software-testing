package my.edu.utar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import junitparams.FileParameters;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.mappers.CsvWithHeaderMapper;

/**
 * TEST DOUBLE tests for CalculatePrintingCharge (FR5 and FR6, Appendix A).
 *
 * This class satisfies assessment component C.3. It exercises
 * calculateTotalCharge, which cannot be tested without its two collaborators:
 *
 *   printerAvailability - an EXTERNAL module developed by another team. The
 *       assignment states that this module must be mocked, so the real class is never
 *       invoked. It is replaced by a Mockito test double created from the
 *       PrinterAvailabilityService interface.
 *
 *   applyDiscount - the discount module. It has its own unit tests, so here it is
 *       replaced by a test double as well. That isolates the charge calculation: if a
 *       test in this class fails, the fault is in CalculatePrintingCharge and not in
 *       the discount rules.
 *
 * Both roles of a test double are demonstrated:
 *
 *   STUB - the double is programmed with when(...).thenReturn(...) so that it feeds a
 *       chosen answer INTO the class under test. Used to decide whether a printer is
 *       available and what discount applies.
 *
 *   MOCK - the double records the calls made to it, and verify(...) then asserts that
 *       the class under test called the collaborator the right number of times, with
 *       the right arguments and in the right order. Used to prove that the printer is
 *       checked BEFORE any charge is calculated, and that it is checked exactly once.
 */
@RunWith(JUnitParamsRunner.class)
public class CalculatePrintingChargeTestDoubleTests {

	private static final double DELTA = 0.001;

	private PrinterAvailabilityService printerAvailabilityMock;
	private ApplyDiscount applyDiscountMock;
	private CalculatePrintingCharge calculatePrintingCharge;
	private Customer customer;

	@Before
	public void setupForAllTests() {

		// create the test doubles for the two collaborators
		printerAvailabilityMock = mock(PrinterAvailabilityService.class);
		applyDiscountMock = mock(ApplyDiscount.class);

		calculatePrintingCharge = new CalculatePrintingCharge(printerAvailabilityMock,
				applyDiscountMock);

		customer = new Customer("C0001", "Ali Bin Ahmad", "ali.ahmad@example.com", "0123456789",
				CustomerType.STUDENT, 3);
	}

	private PrintOrder buildOrder(PaperSize paperSize, PrintType printType, PrintingSide side,
			int pages, int copies, BindingOption binding, boolean lamination, boolean express) {
		return new PrintOrder("ORD001", customer, printType, paperSize, side, pages, copies,
				binding, lamination, express);
	}

	// -------------------------------------------- VALID CASES WITH A STUBBED PRINTER

	/**
	 * VALID CASE using a STUB - parameterised, test data read from an external text file.
	 *
	 * The printer availability double is stubbed to report that a printer IS available,
	 * and the discount double is stubbed to return the discount the file says applies.
	 * The whole calculation chain is then verified against the expected base charge,
	 * optional service charge, subtotal, discount and total held in the file.
	 */
	@Test
	@FileParameters(value = "total-charge-test-data.csv", mapper = CsvWithHeaderMapper.class)
	public void testCalculateTotalChargeWithAnAvailablePrinter(String customerType,
			int previousOrderCount, String paperSize, String printType, String printingSide,
			int numberOfPages, int numberOfCopies, String bindingOption, boolean lamination,
			boolean express, double expectedBaseCharge, double expectedOptionalCharge,
			double expectedSubtotal, double expectedDiscount, double expectedTotal) {

		Customer orderCustomer = new Customer("C0001", "Test Customer", "test@example.com",
				"0123334444", CustomerType.fromString(customerType), previousOrderCount);

		PrintOrder printOrder = new PrintOrder("ORD001", orderCustomer,
				PrintType.fromString(printType), PaperSize.fromString(paperSize),
				PrintingSide.fromString(printingSide), numberOfPages, numberOfCopies,
				BindingOption.fromString(bindingOption), lamination, express);

		// STUB 1 : the external module reports that a suitable printer is available
		when(printerAvailabilityMock.isPrinterAvailable(anyString(), anyString())).thenReturn(true);

		// STUB 2 : the discount module returns the discount recorded in the data file
		when(applyDiscountMock.calculateDiscount(orderCustomer, expectedSubtotal))
				.thenReturn(expectedDiscount);

		double actualTotal = calculatePrintingCharge.calculateTotalCharge(printOrder);

		assertEquals("total printing charge", expectedTotal, actualTotal, DELTA);
		assertEquals("base charge", expectedBaseCharge, printOrder.getBaseCharge(), DELTA);
		assertEquals("optional service charge", expectedOptionalCharge,
				printOrder.getOptionalServiceCharge(), DELTA);
		assertEquals("subtotal", expectedSubtotal, printOrder.getSubtotal(), DELTA);
		assertEquals("discount", expectedDiscount, printOrder.getDiscountAmount(), DELTA);
		assertEquals("total stored on the order", expectedTotal,
				printOrder.getTotalPrintingCharge(), DELTA);
		assertTrue(printOrder.isChargeCalculated());
		assertEquals(OrderStatus.CONFIRMED, printOrder.getOrderStatus());
	}

	/**
	 * VALID CASE using a MOCK - parameterised.
	 *
	 * Appendix A states that the printer availability module receives the PAPER SIZE
	 * and the PRINT TYPE. This test proves that the class under test passes exactly
	 * those two values, in the textual form the external module expects, and that it
	 * asks exactly once per order.
	 */
	@Test
	@Parameters({ "A3, Black & White",
			"A3, Colour",
			"A4, Black & White",
			"A4, Colour",
			"A5, Black & White",
			"A5, Colour" })
	public void testPrinterAvailabilityIsAskedWithThePaperSizeAndPrintType(String paperSize,
			String printType) {

		PrintOrder printOrder = buildOrder(PaperSize.fromString(paperSize),
				PrintType.fromString(printType), PrintingSide.SINGLE_SIDED, 10, 1,
				BindingOption.NONE, false, false);

		when(printerAvailabilityMock.isPrinterAvailable(anyString(), anyString())).thenReturn(true);
		when(applyDiscountMock.calculateDiscount(eq(customer), anyDouble())).thenReturn(0.00);

		calculatePrintingCharge.calculateTotalCharge(printOrder);

		// MOCK : verify the exact arguments and the exact number of invocations
		verify(printerAvailabilityMock, times(1)).isPrinterAvailable(paperSize, printType);
		verify(printerAvailabilityMock, never()).isPrinterAvailable(eq("A0"), anyString());
	}

	/**
	 * VALID CASE using a MOCK with an ArgumentCaptor.
	 *
	 * The discount module must be handed the SUBTOTAL, that is the base charge plus the
	 * optional service charges, and not the base charge on its own. Capturing the
	 * argument proves which value was passed across the module boundary.
	 *
	 * A3 Colour double-sided, 50 pages, 2 copies  = RM1.40 x 100 = RM140.00 base
	 * Spiral binding RM8.00 + lamination RM1.50 x 100 = RM158.00 optional
	 * subtotal                                        = RM298.00
	 */
	@Test
	public void testApplyDiscountReceivesTheSubtotalNotTheBaseCharge() {

		PrintOrder printOrder = buildOrder(PaperSize.A3, PrintType.COLOUR,
				PrintingSide.DOUBLE_SIDED, 50, 2, BindingOption.SPIRAL_BINDING, true, false);

		when(printerAvailabilityMock.isPrinterAvailable(anyString(), anyString())).thenReturn(true);
		when(applyDiscountMock.calculateDiscount(customer, 298.00)).thenReturn(29.80);

		double total = calculatePrintingCharge.calculateTotalCharge(printOrder);

		ArgumentCaptor<Double> subtotalCaptor = ArgumentCaptor.forClass(Double.class);
		verify(applyDiscountMock, times(1)).calculateDiscount(eq(customer), subtotalCaptor.capture());

		assertEquals("the subtotal handed to applyDiscount", 298.00,
				subtotalCaptor.getValue().doubleValue(), DELTA);
		assertEquals(268.20, total, DELTA);
	}

	/**
	 * VALID CASE using a MOCK for in-order verification.
	 *
	 * Appendix A requires the system to invoke the printer availability module BEFORE
	 * calculating the printing charges. InOrder verification is the only way to assert
	 * that sequencing, because both collaborators are consulted during the same call.
	 */
	@Test
	public void testPrinterIsCheckedBeforeTheDiscountIsRequested() {

		PrintOrder printOrder = buildOrder(PaperSize.A4, PrintType.COLOUR,
				PrintingSide.SINGLE_SIDED, 100, 1, BindingOption.NONE, false, false);

		when(printerAvailabilityMock.isPrinterAvailable(anyString(), anyString())).thenReturn(true);
		when(applyDiscountMock.calculateDiscount(customer, 80.00)).thenReturn(8.00);

		calculatePrintingCharge.calculateTotalCharge(printOrder);

		InOrder inOrder = inOrder(printerAvailabilityMock, applyDiscountMock);
		inOrder.verify(printerAvailabilityMock).isPrinterAvailable("A4", "Colour");
		inOrder.verify(applyDiscountMock).calculateDiscount(customer, 80.00);
	}

	/**
	 * VALID CASE using a STUB - parameterised.
	 *
	 * The same order is priced against several stubbed discount answers. This shows
	 * that CalculatePrintingCharge simply subtracts whatever the discount module
	 * returns, so the two modules can be developed and changed independently.
	 *
	 * A4 Colour single-sided, 100 pages, 1 copy = RM0.80 x 100 = RM80.00 subtotal.
	 */
	@Test
	@Parameters({ " 0.00, 80.00",
			" 8.00, 72.00",
			"12.00, 68.00",
			"20.00, 60.00",
			"80.00,  0.00" })
	public void testTotalIsSubtotalMinusWhateverTheDiscountModuleReturns(double stubbedDiscount,
			double expectedTotal) {

		PrintOrder printOrder = buildOrder(PaperSize.A4, PrintType.COLOUR,
				PrintingSide.SINGLE_SIDED, 100, 1, BindingOption.NONE, false, false);

		when(printerAvailabilityMock.isPrinterAvailable(anyString(), anyString())).thenReturn(true);
		when(applyDiscountMock.calculateDiscount(customer, 80.00)).thenReturn(stubbedDiscount);

		assertEquals(expectedTotal, calculatePrintingCharge.calculateTotalCharge(printOrder), DELTA);
		assertEquals(stubbedDiscount, printOrder.getDiscountAmount(), DELTA);
	}

	// ---------------------------------------- INVALID CASES : PRINTER NOT AVAILABLE

	/**
	 * INVALID CASE using a STUB - parameterised, test data read from an external text file.
	 *
	 * printer-availability-test-data.csv drives both outcomes. When the stubbed module
	 * reports false, Appendix A requires that the system:
	 *   - terminates the print order creation process,
	 *   - does NOT calculate the printing charges,
	 *   - does NOT generate the invoice.
	 *
	 * The expected order status in the file records which branch each row exercises.
	 */
	@Test
	@FileParameters(value = "printer-availability-test-data.csv", mapper = CsvWithHeaderMapper.class)
	public void testOrderIsAbandonedWhenNoPrinterIsAvailable(String paperSize, String printType,
			boolean printerAvailable, String expectedOrderStatus) {

		PrintOrder printOrder = buildOrder(PaperSize.fromString(paperSize),
				PrintType.fromString(printType), PrintingSide.SINGLE_SIDED, 20, 2,
				BindingOption.NONE, false, false);

		// STUB : the external module decides the outcome of this test
		when(printerAvailabilityMock.isPrinterAvailable(paperSize, printType))
				.thenReturn(printerAvailable);

		if (printerAvailable) {
			when(applyDiscountMock.calculateDiscount(eq(customer), anyDouble())).thenReturn(0.00);
			calculatePrintingCharge.calculateTotalCharge(printOrder);
			assertTrue(printOrder.isChargeCalculated());
		} else {
			try {
				calculatePrintingCharge.calculateTotalCharge(printOrder);
				fail("A PrinterUnavailableException was expected for " + paperSize + " " + printType);
			} catch (PrinterUnavailableException e) {
				assertEquals(PrinterAvailability.UNAVAILABLE_MESSAGE, e.getMessage());
			}
			assertFalse("No charge may be calculated when the printer is unavailable",
					printOrder.isChargeCalculated());
			assertEquals(0.00, printOrder.getTotalPrintingCharge(), DELTA);

			// MOCK : the discount module must never be consulted for an abandoned order
			verifyNoInteractions(applyDiscountMock);
		}

		assertEquals(expectedOrderStatus, printOrder.getOrderStatus().getDescription());
	}

	/**
	 * INVALID CASE using a STUB and a MOCK together.
	 *
	 * When the printer is unavailable the exception carries the exact message required
	 * by Appendix A, the order is cancelled, and no work is done downstream.
	 */
	@Test
	public void testUnavailablePrinterStopsTheCalculationImmediately() {

		PrintOrder printOrder = buildOrder(PaperSize.A3, PrintType.COLOUR,
				PrintingSide.DOUBLE_SIDED, 500, 1000, BindingOption.SPIRAL_BINDING, true, true);

		// STUB : no suitable printer
		when(printerAvailabilityMock.isPrinterAvailable("A3", "Colour")).thenReturn(false);

		try {
			calculatePrintingCharge.calculateTotalCharge(printOrder);
			fail("A PrinterUnavailableException was expected");
		} catch (PrinterUnavailableException e) {
			assertEquals("Selected printer is currently unavailable.", e.getMessage());
		}

		assertEquals(OrderStatus.CANCELLED, printOrder.getOrderStatus());
		assertEquals(0.00, printOrder.getBaseCharge(), DELTA);
		assertEquals(0.00, printOrder.getOptionalServiceCharge(), DELTA);
		assertEquals(0.00, printOrder.getDiscountAmount(), DELTA);
		assertFalse(printOrder.isChargeCalculated());

		// MOCK : the printer was asked once, and nothing else was asked at all
		verify(printerAvailabilityMock, times(1)).isPrinterAvailable("A3", "Colour");
		verifyNoInteractions(applyDiscountMock);
	}

	/**
	 * INVALID CASE using a MOCK - no invoice may be produced for an abandoned order.
	 * This links Appendix A to FR7 and shows the two modules behaving consistently.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testNoInvoiceCanBeGeneratedForAnAbandonedOrder() {

		PrintOrder printOrder = buildOrder(PaperSize.A5, PrintType.COLOUR,
				PrintingSide.SINGLE_SIDED, 10, 1, BindingOption.NONE, false, false);

		when(printerAvailabilityMock.isPrinterAvailable(anyString(), anyString())).thenReturn(false);

		try {
			calculatePrintingCharge.calculateTotalCharge(printOrder);
		} catch (PrinterUnavailableException e) {
			// expected : the order has been abandoned
		}

		// the order was never priced, so the invoice module must refuse it
		new GenerateInvoice().generateInvoice(printOrder);
	}

	/**
	 * INVALID CASE using a STUB that throws.
	 *
	 * A test double can also be programmed to fail, which is how an unreliable external
	 * service is simulated. Here the printer availability module itself breaks down;
	 * the failure must surface to the caller rather than being swallowed and treated
	 * as "printer available".
	 */
	@Test(expected = RuntimeException.class)
	public void testFailureInsideTheExternalPrinterModuleIsNotSwallowed() {

		PrintOrder printOrder = buildOrder(PaperSize.A4, PrintType.BLACK_AND_WHITE,
				PrintingSide.SINGLE_SIDED, 20, 2, BindingOption.NONE, false, false);

		// STUB : the external service is down
		when(printerAvailabilityMock.isPrinterAvailable(anyString(), anyString()))
				.thenThrow(new RuntimeException("Printer availability service is unreachable"));

		calculatePrintingCharge.calculateTotalCharge(printOrder);
	}

	/**
	 * INVALID CASE using a STUB that answers differently on consecutive calls.
	 *
	 * The first attempt finds no printer and is abandoned; the customer retries and the
	 * second attempt succeeds. This demonstrates consecutive stubbing and confirms that
	 * a failed attempt leaves no charge behind on the retried order.
	 */
	@Test
	public void testRetryAfterThePrinterBecomesAvailable() {

		when(printerAvailabilityMock.isPrinterAvailable("A4", "Colour"))
				.thenReturn(false)
				.thenReturn(true);
		when(applyDiscountMock.calculateDiscount(eq(customer), anyDouble())).thenReturn(0.00);

		PrintOrder firstAttempt = buildOrder(PaperSize.A4, PrintType.COLOUR,
				PrintingSide.SINGLE_SIDED, 100, 1, BindingOption.NONE, false, false);

		try {
			calculatePrintingCharge.calculateTotalCharge(firstAttempt);
			fail("The first attempt should have been abandoned");
		} catch (PrinterUnavailableException e) {
			assertEquals(OrderStatus.CANCELLED, firstAttempt.getOrderStatus());
		}

		PrintOrder secondAttempt = buildOrder(PaperSize.A4, PrintType.COLOUR,
				PrintingSide.SINGLE_SIDED, 100, 1, BindingOption.NONE, false, false);

		assertEquals(80.00, calculatePrintingCharge.calculateTotalCharge(secondAttempt), DELTA);
		assertEquals(OrderStatus.CONFIRMED, secondAttempt.getOrderStatus());

		// MOCK : the external module was asked exactly twice, once per attempt
		verify(printerAvailabilityMock, times(2)).isPrinterAvailable("A4", "Colour");
	}
}
