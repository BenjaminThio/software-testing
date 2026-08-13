package my.edu.utar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.FileParameters;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.mappers.CsvWithHeaderMapper;

/**
 * Unit tests for PrintOrder (FR1 - Create Print Order).
 *
 * Test design techniques applied in this class:
 *   - BOUNDARY VALUE ANALYSIS on the number of pages, whose valid partition is 1 to
 *     500: the values 0, 1, 2, 499, 500 and 501 are all exercised;
 *   - BOUNDARY VALUE ANALYSIS on the number of copies, whose valid partition is 1 to
 *     1000: the values 0, 1, 2, 999, 1000 and 1001 are all exercised;
 *   - Equivalence Partitioning on each enumerated printing option;
 *   - the invalid page and copy combinations are READ FROM A TEXT FILE
 *     (invalid-order-test-data.csv).
 */
@RunWith(JUnitParamsRunner.class)
public class PrintOrderUnitTests {

	private static final double DELTA = 0.001;

	private Customer customer;

	@Before
	public void setupForAllTests() {
		customer = new Customer("C0001", "Ali Bin Ahmad", "ali.ahmad@example.com", "0123456789",
				CustomerType.STUDENT, 3);
	}

	private PrintOrder buildOrder(int numberOfPages, int numberOfCopies) {
		return new PrintOrder("ORD001", customer, PrintType.BLACK_AND_WHITE, PaperSize.A4,
				PrintingSide.SINGLE_SIDED, numberOfPages, numberOfCopies);
	}

	// ----------------------------------------------------------------- VALID CASES

	/**
	 * VALID CASE - a newly created order keeps every detail it was given, and starts
	 * life with no charges, the status New and the payment status Unpaid.
	 */
	@Test
	public void testNewOrderHoldsItsDetailsAndStartsUnpriced() {

		PrintOrder printOrder = new PrintOrder("ORD001", customer, PrintType.COLOUR, PaperSize.A3,
				PrintingSide.DOUBLE_SIDED, 50, 2, BindingOption.SPIRAL_BINDING, true, false);

		assertEquals("ORD001", printOrder.getOrderId());
		assertEquals(customer, printOrder.getCustomer());
		assertEquals(PrintType.COLOUR, printOrder.getPrintType());
		assertEquals(PaperSize.A3, printOrder.getPaperSize());
		assertEquals(PrintingSide.DOUBLE_SIDED, printOrder.getPrintingSide());
		assertEquals(50, printOrder.getNumberOfPages());
		assertEquals(2, printOrder.getNumberOfCopies());
		assertEquals(BindingOption.SPIRAL_BINDING, printOrder.getBindingOption());
		assertTrue(printOrder.isLaminationRequired());
		assertFalse(printOrder.isExpressPrintingRequired());

		assertEquals(0.00, printOrder.getTotalPrintingCharge(), DELTA);
		assertFalse("A new order must not claim its charge has been calculated",
				printOrder.isChargeCalculated());
		assertEquals(OrderStatus.NEW, printOrder.getOrderStatus());
		assertEquals(PaymentStatus.UNPAID, printOrder.getPaymentStatus());
	}

	/**
	 * VALID CASE - the short constructor defaults to no optional services, which is the
	 * most common order.
	 */
	@Test
	public void testShortConstructorSelectsNoOptionalServices() {

		PrintOrder printOrder = buildOrder(20, 2);

		assertEquals(BindingOption.NONE, printOrder.getBindingOption());
		assertFalse(printOrder.isLaminationRequired());
		assertFalse(printOrder.isExpressPrintingRequired());
	}

	/**
	 * VALID CASE - BOUNDARY VALUE ANALYSIS on the number of pages.
	 * 1 and 500 are the boundaries of the valid partition, 2 and 499 sit just inside.
	 */
	@Test
	@Parameters({ "1", "2", "250", "499", "500" })
	public void testNumberOfPagesInsideTheValidPartitionIsAccepted(int numberOfPages) {

		assertEquals(numberOfPages, buildOrder(numberOfPages, 1).getNumberOfPages());
	}

	/**
	 * VALID CASE - BOUNDARY VALUE ANALYSIS on the number of copies.
	 * 1 and 1000 are the boundaries of the valid partition, 2 and 999 sit just inside.
	 */
	@Test
	@Parameters({ "1", "2", "500", "999", "1000" })
	public void testNumberOfCopiesInsideTheValidPartitionIsAccepted(int numberOfCopies) {

		assertEquals(numberOfCopies, buildOrder(10, numberOfCopies).getNumberOfCopies());
	}

	/**
	 * VALID CASE - lamination is charged on the total number of PRINTED pages, so the
	 * order must be able to report pages x copies.
	 */
	@Test
	@Parameters({ "1, 1, 1",
			"20, 2, 40",
			"50, 3, 150",
			"500, 1000, 500000" })
	public void testGetTotalPrintedPages(int numberOfPages, int numberOfCopies,
			int expectedPrintedPages) {

		assertEquals(expectedPrintedPages, buildOrder(numberOfPages, numberOfCopies)
				.getTotalPrintedPages());
	}

	/**
	 * VALID CASE - Equivalence Partitioning across the printing options. Every
	 * supported paper size, print type, printing side and binding option can be used
	 * to build an order.
	 */
	@Test
	@Parameters({ "A3, Black & White, Single-sided, None",
			"A3, Colour, Double-sided, Staple Binding",
			"A4, Black & White, Double-sided, Comb Binding",
			"A4, Colour, Single-sided, Spiral Binding",
			"A5, Black & White, Single-sided, Staple Binding",
			"A5, Colour, Double-sided, None" })
	public void testEveryPrintingOptionCombinationCanBeOrdered(String paperSize, String printType,
			String printingSide, String bindingOption) {

		PrintOrder printOrder = new PrintOrder("ORD001", customer, PrintType.fromString(printType),
				PaperSize.fromString(paperSize), PrintingSide.fromString(printingSide), 10, 1,
				BindingOption.fromString(bindingOption), false, false);

		assertEquals(PaperSize.fromString(paperSize), printOrder.getPaperSize());
		assertEquals(PrintType.fromString(printType), printOrder.getPrintType());
		assertEquals(PrintingSide.fromString(printingSide), printOrder.getPrintingSide());
		assertEquals(BindingOption.fromString(bindingOption), printOrder.getBindingOption());
	}

	/**
	 * VALID CASE - the subtotal is the base charge plus the optional service charges,
	 * before any discount.
	 */
	@Test
	@Parameters({ "140.00, 158.00, 298.00",
			"8.00,     0.00,   8.00",
			"450.00,   0.00, 450.00",
			"0.15,     0.00,   0.15" })
	public void testGetSubtotal(double baseCharge, double optionalCharge, double expectedSubtotal) {

		PrintOrder printOrder = buildOrder(20, 2);
		printOrder.setBaseCharge(baseCharge);
		printOrder.setOptionalServiceCharge(optionalCharge);

		assertEquals(expectedSubtotal, printOrder.getSubtotal(), DELTA);
	}

	/**
	 * VALID CASE - setting the total printing charge marks the order as priced, which
	 * is the flag GenerateInvoice checks before it will produce an invoice.
	 */
	@Test
	public void testSettingTheTotalChargeMarksTheOrderAsPriced() {

		PrintOrder printOrder = buildOrder(20, 2);
		assertFalse(printOrder.isChargeCalculated());

		printOrder.setTotalPrintingCharge(268.20);

		assertTrue(printOrder.isChargeCalculated());
		assertEquals(268.20, printOrder.getTotalPrintingCharge(), DELTA);
	}

	/**
	 * VALID CASE - the order and payment statuses can be moved through their life cycle.
	 */
	@Test
	@Parameters({ "CONFIRMED, UNPAID, Confirmed, Unpaid",
			"COMPLETED, SUCCESSFUL, Completed, Successful",
			"PENDING_PAYMENT, UNSUCCESSFUL, Pending Payment, Unsuccessful",
			"CANCELLED, UNPAID, Cancelled, Unpaid" })
	public void testOrderAndPaymentStatusCanBeUpdated(String orderStatus, String paymentStatus,
			String expectedOrderDescription, String expectedPaymentDescription) {

		PrintOrder printOrder = buildOrder(20, 2);
		printOrder.setOrderStatus(OrderStatus.valueOf(orderStatus));
		printOrder.setPaymentStatus(PaymentStatus.valueOf(paymentStatus));

		assertEquals(OrderStatus.valueOf(orderStatus), printOrder.getOrderStatus());
		assertEquals(PaymentStatus.valueOf(paymentStatus), printOrder.getPaymentStatus());
		assertEquals(expectedOrderDescription, printOrder.getOrderStatus().getDescription());
		assertEquals(expectedPaymentDescription, printOrder.getPaymentStatus().getDescription());
	}

	// --------------------------------------------------------------- INVALID CASES

	/**
	 * INVALID CASE - BOUNDARY VALUE ANALYSIS on the number of pages.
	 * 0 is immediately below the valid partition and 501 is immediately above it, so
	 * both must be rejected. At least one page must be printed.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters({ "0", "-1", "-500", "501", "502", "10000" })
	public void testNumberOfPagesOutsideTheValidPartitionIsRejected(int numberOfPages) {

		buildOrder(numberOfPages, 1);
	}

	/**
	 * INVALID CASE - BOUNDARY VALUE ANALYSIS on the number of copies.
	 * 0 is immediately below the valid partition and 1001 is immediately above it.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters({ "0", "-1", "-100", "1001", "1002", "50000" })
	public void testNumberOfCopiesOutsideTheValidPartitionIsRejected(int numberOfCopies) {

		buildOrder(10, numberOfCopies);
	}

	/**
	 * INVALID CASE - parameterised, test data read from an external text file.
	 * The page and copy rows of invalid-order-test-data.csv are re-used here so the
	 * same invalid data drives both the order class and the charge calculator.
	 * The three rows that carry an unsupported printing option are converted by
	 * fromString, which raises the same IllegalArgumentException.
	 */
	@Test(expected = IllegalArgumentException.class)
	@FileParameters(value = "invalid-order-test-data.csv", mapper = CsvWithHeaderMapper.class)
	public void testInvalidOrderDetailsAreRejected(String paperSize, String printType,
			String printingSide, int numberOfPages, int numberOfCopies, String reasonForRejection) {

		new PrintOrder("ORD001", customer, PrintType.fromString(printType),
				PaperSize.fromString(paperSize), PrintingSide.fromString(printingSide),
				numberOfPages, numberOfCopies);
	}

	/**
	 * INVALID CASE - an order cannot be created without a customer, a printing option
	 * or an order ID.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters(method = "getInvalidConstructorArguments")
	public void testMissingOrderDetailIsRejected(String orderId, Customer orderCustomer,
			PrintType printType, PaperSize paperSize, PrintingSide printingSide,
			BindingOption bindingOption) {

		new PrintOrder(orderId, orderCustomer, printType, paperSize, printingSide, 10, 1,
				bindingOption, false, false);
	}

	private Object[] getInvalidConstructorArguments() {

		Customer validCustomer = new Customer("C0001", "Ali Bin Ahmad", "ali.ahmad@example.com",
				"0123456789", CustomerType.STUDENT, 3);

		return new Object[] {
				new Object[] { null, validCustomer, PrintType.COLOUR, PaperSize.A4,
						PrintingSide.SINGLE_SIDED, BindingOption.NONE },
				new Object[] { "", validCustomer, PrintType.COLOUR, PaperSize.A4,
						PrintingSide.SINGLE_SIDED, BindingOption.NONE },
				new Object[] { "   ", validCustomer, PrintType.COLOUR, PaperSize.A4,
						PrintingSide.SINGLE_SIDED, BindingOption.NONE },
				new Object[] { "ORD001", null, PrintType.COLOUR, PaperSize.A4,
						PrintingSide.SINGLE_SIDED, BindingOption.NONE },
				new Object[] { "ORD001", validCustomer, null, PaperSize.A4,
						PrintingSide.SINGLE_SIDED, BindingOption.NONE },
				new Object[] { "ORD001", validCustomer, PrintType.COLOUR, null,
						PrintingSide.SINGLE_SIDED, BindingOption.NONE },
				new Object[] { "ORD001", validCustomer, PrintType.COLOUR, PaperSize.A4, null,
						BindingOption.NONE },
				new Object[] { "ORD001", validCustomer, PrintType.COLOUR, PaperSize.A4,
						PrintingSide.SINGLE_SIDED, null }
		};
	}

	/**
	 * INVALID CASE - a negative monetary amount can never be a legal charge.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters({ "-0.01", "-1.00", "-100.00" })
	public void testNegativeBaseChargeIsRejected(double baseCharge) {

		buildOrder(10, 1).setBaseCharge(baseCharge);
	}

	/**
	 * INVALID CASE - the same rule applies to every stored monetary field.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters({ "optional", "discount", "total" })
	public void testNegativeAmountIsRejectedByEveryChargeField(String fieldName) {

		PrintOrder printOrder = buildOrder(10, 1);

		if (fieldName.equals("optional"))
			printOrder.setOptionalServiceCharge(-1.00);
		else if (fieldName.equals("discount"))
			printOrder.setDiscountAmount(-1.00);
		else
			printOrder.setTotalPrintingCharge(-1.00);
	}

	/**
	 * INVALID CASE - a null order status is rejected.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testNullOrderStatusIsRejected() {

		buildOrder(10, 1).setOrderStatus(null);
	}

	/**
	 * INVALID CASE - a null payment status is rejected.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testNullPaymentStatusIsRejected() {

		buildOrder(10, 1).setPaymentStatus(null);
	}
}
