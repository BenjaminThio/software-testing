package my.edu.utar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
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

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

import org.junit.After;
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
 * INTEGRATION TESTS for the PrintMaster Printing Service Management System.
 *
 * This class satisfies assessment component C.5. It is run AFTER the unit tests, once
 * every individual module has been verified on its own by:
 *
 *   CustomerUnitTests, ReadCustomerUnitTests, AddNewCustomerUnitTests,
 *   PrintOrderUnitTests, CalculatePrintingChargeUnitTests, ApplyDiscountUnitTests,
 *   GenerateInvoiceUnitTests, CalculatePrintingChargeTestDoubleTests and
 *   PaymentAndEmailInvoiceTestDoubleTests.
 *
 * Where the unit tests replaced collaborators with test doubles in order to isolate a
 * single class, these integration tests do the opposite: the modules developed in
 * this assignment are assembled as REAL objects and are exercised together, so that
 * the data really does flow from one module into the next.
 *
 *   REAL      : ReadCustomer, AddNewCustomer, Customer, PrintOrder,
 *               CalculatePrintingCharge, ApplyDiscount, GenerateInvoice,
 *               PrintOrderService
 *   TEST DOUBLE : printerAvailability, payment and emailInvoice, which are external
 *               modules that are either mocked by instruction (Appendix A) or not to
 *               be developed (FR8 and FR9). They cannot be made real, so they remain
 *               test doubles even here.
 *
 * The integration paths covered are:
 *   IT1  readCustomer  -> printOrder -> calculatePrintingCharge -> applyDiscount
 *   IT2  calculatePrintingCharge -> generateInvoice
 *   IT3  addNewCustomer -> readCustomer (write a record and read it back)
 *   IT4  addNewCustomer -> printOrder -> calculatePrintingCharge -> generateInvoice
 *   IT5  the full order flow through to payment and emailInvoice
 *   IT6  the printer unavailable path, where the chain must stop early
 */
@RunWith(JUnitParamsRunner.class)
public class PrintingServiceIntegrationTests {

	private static final double DELTA = 0.001;

	/** Copy of the customer fixture, rebuilt before every test so writes are safe. */
	private static final String WORKING_CUSTOMER_FILE = "integration-customer.txt";

	private ReadCustomer readCustomer;
	private AddNewCustomer addNewCustomer;
	private ApplyDiscount applyDiscount;
	private CalculatePrintingCharge calculatePrintingCharge;
	private GenerateInvoice generateInvoice;
	private PrintOrderService printOrderService;

	private PrinterAvailabilityService printerAvailabilityMock;
	private Payment paymentMock;
	private EmailInvoice emailInvoiceMock;

	@Before
	public void assembleTheSystem() throws Exception {

		buildWorkingCustomerFile();

		// the external modules stay as test doubles
		printerAvailabilityMock = mock(PrinterAvailabilityService.class);
		paymentMock = mock(Payment.class);
		emailInvoiceMock = mock(EmailInvoice.class);

		// every module developed in this assignment is REAL and wired to the next
		readCustomer = new ReadCustomer(WORKING_CUSTOMER_FILE);
		addNewCustomer = new AddNewCustomer(WORKING_CUSTOMER_FILE, readCustomer);
		applyDiscount = new ApplyDiscount();
		calculatePrintingCharge = new CalculatePrintingCharge(printerAvailabilityMock, applyDiscount);
		generateInvoice = new GenerateInvoice();

		printOrderService = new PrintOrderService(readCustomer, addNewCustomer,
				calculatePrintingCharge, generateInvoice, paymentMock, emailInvoiceMock);
	}

	@After
	public void removeWorkingCustomerFile() {
		File workingFile = new File(WORKING_CUSTOMER_FILE);
		if (workingFile.exists())
			workingFile.delete();
	}

	/**
	 * Rebuilds a known customer file before each test, so a test that registers a new
	 * customer cannot affect the next test.
	 */
	private void buildWorkingCustomerFile() throws Exception {

		PrintWriter output = new PrintWriter(new File(WORKING_CUSTOMER_FILE));
		output.println("# integration test working copy of customer.txt");
		output.println("C0001,Ali Bin Ahmad,ali.ahmad@example.com,0123456789,Student,3");
		output.println("C0002,Siti Nurhaliza,siti.n@example.com,0139876543,Regular,12");
		output.println("C0003,Tan Wei Ming,weiming.tan@printhub.com.my,01123456789,Corporate,25");
		output.println("C0004,Kavitha Rajan,kavitha.rajan@example.com,0175551234,Student,21");
		output.println("C0005,Lim Mei Ling,meiling.lim@example.com,0198887777,Regular,20");
		output.close();
	}

	private void stubPrinterAsAvailable() {
		when(printerAvailabilityMock.isPrinterAvailable(anyString(), anyString())).thenReturn(true);
	}

	// ------------------------------------------------------------------------- IT1

	/**
	 * IT1 - readCustomer -> printOrder -> calculatePrintingCharge -> applyDiscount.
	 *
	 * The customer is retrieved from the real text file, an order is built for that
	 * customer, and the real discount module prices it. The discount that comes out
	 * therefore depends on data that travelled all the way from the file, which is
	 * exactly what a unit test with a stubbed collaborator cannot prove.
	 *
	 * C0001 Ali Bin Ahmad is a Student with 3 previous orders.
	 * A3 Colour double-sided, 50 pages, 2 copies, spiral binding and lamination:
	 *   base RM1.40 x 100 = RM140.00, optional RM8.00 + RM150.00 = RM158.00,
	 *   subtotal RM298.00, student discount 10% = RM29.80, total RM268.20.
	 */
	@Test
	public void testIT1RetrievedCustomerDrivesTheDiscount() {

		stubPrinterAsAvailable();

		Customer customer = readCustomer.getCustomerById("C0001");
		assertNotNull("the customer must be read from " + WORKING_CUSTOMER_FILE, customer);
		assertEquals(CustomerType.STUDENT, customer.getCustomerType());

		PrintOrder printOrder = new PrintOrder("ORD001", customer, PrintType.COLOUR, PaperSize.A3,
				PrintingSide.DOUBLE_SIDED, 50, 2, BindingOption.SPIRAL_BINDING, true, false);

		double total = calculatePrintingCharge.calculateTotalCharge(printOrder);

		assertEquals(140.00, printOrder.getBaseCharge(), DELTA);
		assertEquals(158.00, printOrder.getOptionalServiceCharge(), DELTA);
		assertEquals(298.00, printOrder.getSubtotal(), DELTA);
		assertEquals(29.80, printOrder.getDiscountAmount(), DELTA);
		assertEquals(268.20, total, DELTA);
		assertEquals(OrderStatus.CONFIRMED, printOrder.getOrderStatus());
	}

	/**
	 * IT1 - the SAME order priced for four different customers read from the file
	 * produces four different totals, because the customer type and the previous order
	 * count both feed the discount module.
	 *
	 * A4 Colour single-sided, 500 pages, 1 copy = RM0.80 x 500 = RM400.00 subtotal,
	 * which is above the RM300 threshold, so the additional 5% applies to everyone.
	 *
	 *   C0001 Student,   3 previous orders : 400 x 0.90 x 0.95            = RM342.00
	 *   C0002 Regular,  12 previous orders : 400 x 1.00 x 0.95            = RM380.00
	 *   C0003 Corporate,25 previous orders : 400 x 0.85 x 0.95 x 0.95     = RM306.85
	 *   C0004 Student,  21 previous orders : 400 x 0.90 x 0.95 x 0.95     = RM324.90
	 *   C0005 Regular,  20 previous orders : 400 x 1.00 x 0.95            = RM380.00
	 */
	@Test
	@Parameters({ "C0001, Student,   3, 342.00, 58.00",
			"C0002, Regular,  12, 380.00, 20.00",
			"C0003, Corporate,25, 306.85, 93.15",
			"C0004, Student,  21, 324.90, 75.10",
			"C0005, Regular,  20, 380.00, 20.00" })
	public void testIT1DiscountVariesWithTheCustomerReadFromTheFile(String customerId,
			String expectedType, int expectedPreviousOrders, double expectedTotal,
			double expectedDiscount) {

		stubPrinterAsAvailable();

		Customer customer = readCustomer.getCustomerById(customerId);

		assertEquals(CustomerType.fromString(expectedType), customer.getCustomerType());
		assertEquals(expectedPreviousOrders, customer.getPreviousOrderCount());

		PrintOrder printOrder = new PrintOrder("ORD-" + customerId, customer, PrintType.COLOUR,
				PaperSize.A4, PrintingSide.SINGLE_SIDED, 500, 1);

		double total = calculatePrintingCharge.calculateTotalCharge(printOrder);

		assertEquals(400.00, printOrder.getSubtotal(), DELTA);
		assertEquals(expectedDiscount, printOrder.getDiscountAmount(), DELTA);
		assertEquals(expectedTotal, total, DELTA);
	}

	// ------------------------------------------------------------------------- IT2

	/**
	 * IT2 - calculatePrintingCharge -> generateInvoice, driven by an external data file.
	 *
	 * Every row of total-charge-test-data.csv is priced by the REAL calculation and
	 * discount modules, and the invoice produced by the REAL invoice module is checked
	 * against the same row. Because no charge is stubbed here, this test proves the
	 * two modules agree end to end.
	 */
	@Test
	@FileParameters(value = "total-charge-test-data.csv", mapper = CsvWithHeaderMapper.class)
	public void testIT2CalculationFlowsIntoTheInvoice(String customerType, int previousOrderCount,
			String paperSize, String printType, String printingSide, int numberOfPages,
			int numberOfCopies, String bindingOption, boolean lamination, boolean express,
			double expectedBaseCharge, double expectedOptionalCharge, double expectedSubtotal,
			double expectedDiscount, double expectedTotal) {

		stubPrinterAsAvailable();

		Customer customer = new Customer("C0009", "Integration Customer", "it@example.com",
				"0123334444", CustomerType.fromString(customerType), previousOrderCount);

		PrintOrder printOrder = printOrderService.createOrder("ORD001", customer,
				PrintType.fromString(printType), PaperSize.fromString(paperSize),
				PrintingSide.fromString(printingSide), numberOfPages, numberOfCopies,
				BindingOption.fromString(bindingOption), lamination, express);

		assertEquals("base charge", expectedBaseCharge, printOrder.getBaseCharge(), DELTA);
		assertEquals("optional charge", expectedOptionalCharge,
				printOrder.getOptionalServiceCharge(), DELTA);
		assertEquals("subtotal", expectedSubtotal, printOrder.getSubtotal(), DELTA);
		assertEquals("discount", expectedDiscount, printOrder.getDiscountAmount(), DELTA);
		assertEquals("total", expectedTotal, printOrder.getTotalPrintingCharge(), DELTA);

		String invoice = printOrderService.issueInvoice(printOrder);

		assertTrue(invoice.contains(String.format("%-24s : RM%.2f", "Subtotal", expectedSubtotal)));
		assertTrue(invoice.contains(String.format("%-24s : RM%.2f", "TOTAL AMOUNT PAYABLE",
				expectedTotal)));
	}

	// ------------------------------------------------------------------------- IT3

	/**
	 * IT3 - addNewCustomer -> readCustomer.
	 *
	 * A record written by the registration module must be readable by the retrieval
	 * module. The two classes were unit tested separately, one of them against a
	 * mocked collaborator, so only an integration test can prove that the file format
	 * they agreed on really matches.
	 */
	@Test
	@Parameters({ "Nur Aisyah, nur.aisyah@example.com, 0123334444, Student",
			"Wong Kar Wai, karwai.wong@example.com, 013-2223333, Regular",
			"Muthu Samy, muthu.samy@corp.com.my, 01198887777, Corporate" })
	public void testIT3RegisteredCustomerCanBeReadBack(String name, String emailAddress,
			String phoneNumber, String customerType) {

		int countBefore = readCustomer.getCustomerCount();

		Customer registered = addNewCustomer.addCustomer(name, emailAddress, phoneNumber,
				CustomerType.fromString(customerType));

		// the ID follows on from C0005, the highest ID in the working file
		assertEquals("C0006", registered.getCustomerId());
		assertEquals(countBefore + 1, readCustomer.getCustomerCount());

		Customer readBack = readCustomer.getCustomerById("C0006");

		assertNotNull("the newly registered customer must be retrievable", readBack);
		assertEquals("the record read back must equal the record written", registered, readBack);
		assertEquals(name, readBack.getName());
		assertEquals(emailAddress, readBack.getEmailAddress());
		assertEquals(CustomerType.fromString(customerType), readBack.getCustomerType());
		assertEquals(0, readBack.getPreviousOrderCount());
	}

	/**
	 * IT3 - registering several customers in succession keeps the ID sequence correct,
	 * because each registration re-reads the file the previous one wrote.
	 */
	@Test
	public void testIT3ConsecutiveRegistrationsContinueTheIdSequence() {

		assertEquals("C0006", addNewCustomer.addCustomer("First New", "first@example.com",
				"0121110001", CustomerType.REGULAR).getCustomerId());
		assertEquals("C0007", addNewCustomer.addCustomer("Second New", "second@example.com",
				"0121110002", CustomerType.STUDENT).getCustomerId());
		assertEquals("C0008", addNewCustomer.addCustomer("Third New", "third@example.com",
				"0121110003", CustomerType.CORPORATE).getCustomerId());

		List<Customer> allCustomers = readCustomer.readAllCustomers();
		assertEquals(8, allCustomers.size());
		assertEquals("C0008", allCustomers.get(7).getCustomerId());
		assertEquals(CustomerType.CORPORATE, allCustomers.get(7).getCustomerType());
	}

	/**
	 * IT3 - INVALID PATH. A rejected registration must leave the data file untouched,
	 * so the retrieval module must still see exactly the original records.
	 */
	@Test
	public void testIT3RejectedRegistrationLeavesTheFileUnchanged() {

		int countBefore = readCustomer.getCustomerCount();

		try {
			addNewCustomer.addCustomer("Bad Person", "not-an-email-address", "0123334444",
					CustomerType.REGULAR);
			fail("An IllegalArgumentException was expected for a malformed email address");
		} catch (IllegalArgumentException e) {
			// expected
		}

		assertEquals(countBefore, readCustomer.getCustomerCount());
		assertNull(readCustomer.getCustomerById("C0006"));
	}

	// ------------------------------------------------------------------------- IT4

	/**
	 * IT4 - addNewCustomer -> printOrder -> calculatePrintingCharge -> generateInvoice.
	 *
	 * The complete "Register New Customer" branch of FR1: a walk-in customer is
	 * registered, an order is placed for them straight away, and the invoice is
	 * produced. The customer details printed on the invoice must be the details that
	 * were just written to the file.
	 */
	@Test
	public void testIT4NewCustomerCanOrderAndBeInvoicedImmediately() {

		stubPrinterAsAvailable();

		PrintOrder printOrder = printOrderService.createOrderForNewCustomer("ORD010",
				"Nurul Izzah", "nurul.izzah@utar.edu.my", "0162223333", CustomerType.STUDENT,
				PrintType.BLACK_AND_WHITE, PaperSize.A4, PrintingSide.DOUBLE_SIDED, 200, 5,
				BindingOption.COMB_BINDING, false, true);

		// base RM0.18 x 200 x 5 = RM180.00, optional RM5.00 + RM20.00 = RM25.00
		// subtotal RM205.00, student discount 10% = RM20.50, total RM184.50
		assertEquals(180.00, printOrder.getBaseCharge(), DELTA);
		assertEquals(25.00, printOrder.getOptionalServiceCharge(), DELTA);
		assertEquals(205.00, printOrder.getSubtotal(), DELTA);
		assertEquals(20.50, printOrder.getDiscountAmount(), DELTA);
		assertEquals(184.50, printOrder.getTotalPrintingCharge(), DELTA);

		// the customer really was written to the data file
		Customer storedCustomer = readCustomer.getCustomerById("C0006");
		assertNotNull(storedCustomer);
		assertEquals("Nurul Izzah", storedCustomer.getName());
		assertEquals(storedCustomer, printOrder.getCustomer());

		String invoice = printOrderService.issueInvoice(printOrder);
		assertTrue(invoice.contains("C0006"));
		assertTrue(invoice.contains("Nurul Izzah"));
		assertTrue(invoice.contains("nurul.izzah@utar.edu.my"));
		assertTrue(invoice.contains("Comb Binding"));
		assertTrue(invoice.contains("Express       : Yes"));
		assertTrue(invoice.contains(String.format("%-24s : RM%.2f", "TOTAL AMOUNT PAYABLE", 184.50)));
	}

	/**
	 * IT4 - INVALID PATH. When the customer ID is unknown, the chain must stop at the
	 * retrieval module: no order is built, the printer is never consulted and no
	 * invoice is produced.
	 */
	@Test
	public void testIT4UnknownCustomerStopsTheChainAtRetrieval() {

		// the printer double is deliberately left unprogrammed: if the chain were to
		// reach it at all, verifyNoInteractions below would fail

		try {
			printOrderService.createOrderForExistingCustomer("ORD011", "C0099",
					PrintType.COLOUR, PaperSize.A4, PrintingSide.SINGLE_SIDED, 10, 1,
					BindingOption.NONE, false, false);
			fail("An IllegalArgumentException was expected for an unknown customer ID");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("C0099"));
		}

		verifyNoInteractions(printerAvailabilityMock);
		verifyNoInteractions(paymentMock);
		verifyNoInteractions(emailInvoiceMock);
	}

	// ------------------------------------------------------------------------- IT5

	/**
	 * IT5 - the full order flow.
	 *
	 * readCustomer -> printOrder -> calculatePrintingCharge -> applyDiscount ->
	 * generateInvoice -> payment -> emailInvoice.
	 *
	 * This is the realistic end to end scenario for an existing customer. Only the
	 * three external modules are test doubles; every other value is produced by a real
	 * module and handed to the next one.
	 *
	 * C0003 Tan Wei Ming is a Corporate customer with 25 previous orders.
	 * A3 Colour single-sided, 100 pages, 3 copies = RM1.50 x 300 = RM450.00 subtotal.
	 * Corporate 15%, then 5% for exceeding RM300, then 5% for loyalty:
	 * 450.00 x 0.85 x 0.95 x 0.95 = RM345.21, so the discount is RM104.79.
	 */
	@Test
	public void testIT5FullOrderFlowForAnExistingCustomer() {

		stubPrinterAsAvailable();
		when(paymentMock.makePayment(anyDouble(), any(PaymentMethod.class))).thenReturn(true);
		when(emailInvoiceMock.sendInvoice(anyString(), anyString())).thenReturn(true);

		PrintOrder printOrder = printOrderService.createOrderForExistingCustomer("ORD020", "C0003",
				PrintType.COLOUR, PaperSize.A3, PrintingSide.SINGLE_SIDED, 100, 3,
				BindingOption.NONE, false, false);

		assertEquals("Tan Wei Ming", printOrder.getCustomer().getName());
		assertEquals(450.00, printOrder.getSubtotal(), DELTA);
		assertEquals(104.79, printOrder.getDiscountAmount(), DELTA);
		assertEquals(345.21, printOrder.getTotalPrintingCharge(), DELTA);
		assertEquals(OrderStatus.CONFIRMED, printOrder.getOrderStatus());

		assertTrue(printOrderService.completeOrder(printOrder, PaymentMethod.ONLINE_BANKING));

		assertEquals(PaymentStatus.SUCCESSFUL, printOrder.getPaymentStatus());
		assertEquals(OrderStatus.COMPLETED, printOrder.getOrderStatus());

		// the modules must have been driven in the specified sequence
		InOrder inOrder = inOrder(printerAvailabilityMock, paymentMock, emailInvoiceMock);
		inOrder.verify(printerAvailabilityMock).isPrinterAvailable("A3", "Colour");
		inOrder.verify(paymentMock).makePayment(345.21, PaymentMethod.ONLINE_BANKING);
		inOrder.verify(emailInvoiceMock).sendInvoice(eq("weiming.tan@printhub.com.my"), anyString());

		// the invoice that was emailed must describe the order that was actually priced
		ArgumentCaptor<String> invoiceCaptor = ArgumentCaptor.forClass(String.class);
		verify(emailInvoiceMock).sendInvoice(anyString(), invoiceCaptor.capture());

		String emailedInvoice = invoiceCaptor.getValue();
		assertTrue(emailedInvoice.contains("C0003"));
		assertTrue(emailedInvoice.contains("Tan Wei Ming"));
		assertTrue(emailedInvoice.contains("Corporate"));
		assertTrue(emailedInvoice.contains(String.format("%-24s : RM%.2f", "Discount", -104.79)));
		assertTrue(emailedInvoice.contains(String.format("%-24s : RM%.2f", "TOTAL AMOUNT PAYABLE",
				345.21)));
		assertTrue(emailedInvoice.contains("Completed"));
	}

	/**
	 * IT5 - INVALID PATH. When payment fails the order stops short of completion and
	 * no invoice is emailed, but the priced order itself is preserved so the customer
	 * can try again.
	 */
	@Test
	@Parameters({ "E_WALLET", "CREDIT_CARD", "ONLINE_BANKING" })
	public void testIT5FailedPaymentLeavesTheOrderPending(String paymentMethod) {

		stubPrinterAsAvailable();
		when(paymentMock.makePayment(anyDouble(), any(PaymentMethod.class))).thenReturn(false);

		PrintOrder printOrder = printOrderService.createOrderForExistingCustomer("ORD021", "C0002",
				PrintType.BLACK_AND_WHITE, PaperSize.A4, PrintingSide.SINGLE_SIDED, 20, 2,
				BindingOption.NONE, false, false);

		assertEquals(8.00, printOrder.getTotalPrintingCharge(), DELTA);

		assertFalse(printOrderService.completeOrder(printOrder, PaymentMethod.valueOf(paymentMethod)));

		assertEquals(PaymentStatus.UNSUCCESSFUL, printOrder.getPaymentStatus());
		assertEquals(OrderStatus.PENDING_PAYMENT, printOrder.getOrderStatus());
		assertTrue("the priced order must be preserved", printOrder.isChargeCalculated());

		verify(paymentMock, times(1)).makePayment(8.00, PaymentMethod.valueOf(paymentMethod));
		verify(emailInvoiceMock, never()).sendInvoice(anyString(), anyString());
	}

	// ------------------------------------------------------------------------- IT6

	/**
	 * IT6 - INVALID PATH. Appendix A: when no suitable printer is available the print
	 * order creation process terminates, no printing charge is calculated and no
	 * invoice is generated.
	 *
	 * This integration test proves that the early exit really does propagate through
	 * every downstream module, not merely through the calculation module alone.
	 */
	@Test
	@Parameters({ "C0001, A3, Colour",
			"C0002, A4, Black & White",
			"C0003, A5, Colour" })
	public void testIT6NoPrinterStopsTheWholeChain(String customerId, String paperSize,
			String printType) {

		// STUB : the external module reports that no suitable printer is available
		when(printerAvailabilityMock.isPrinterAvailable(paperSize, printType)).thenReturn(false);

		try {
			printOrderService.createOrderForExistingCustomer("ORD030", customerId,
					PrintType.fromString(printType), PaperSize.fromString(paperSize),
					PrintingSide.SINGLE_SIDED, 50, 2, BindingOption.SPIRAL_BINDING, true, true);
			fail("A PrinterUnavailableException was expected");
		} catch (PrinterUnavailableException e) {
			assertEquals("Selected printer is currently unavailable.", e.getMessage());
		}

		// the external module was consulted exactly once, with the right arguments
		verify(printerAvailabilityMock, times(1)).isPrinterAvailable(paperSize, printType);

		// nothing downstream may have happened
		verifyNoInteractions(paymentMock);
		verifyNoInteractions(emailInvoiceMock);
	}

	/**
	 * IT6 - INVALID PATH. An order abandoned because of an unavailable printer can
	 * never be invoiced, no matter which module is asked.
	 */
	@Test
	public void testIT6AbandonedOrderCannotBeInvoicedOrPaid() {

		when(printerAvailabilityMock.isPrinterAvailable(anyString(), anyString())).thenReturn(false);

		Customer customer = readCustomer.getCustomerById("C0001");
		PrintOrder printOrder = new PrintOrder("ORD031", customer, PrintType.COLOUR, PaperSize.A4,
				PrintingSide.SINGLE_SIDED, 10, 1);

		try {
			calculatePrintingCharge.calculateTotalCharge(printOrder);
			fail("A PrinterUnavailableException was expected");
		} catch (PrinterUnavailableException e) {
			assertEquals(OrderStatus.CANCELLED, printOrder.getOrderStatus());
		}

		try {
			printOrderService.issueInvoice(printOrder);
			fail("An IllegalArgumentException was expected for an unpriced order");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("printing charge has been calculated"));
		}

		try {
			printOrderService.completeOrder(printOrder, PaymentMethod.E_WALLET);
			fail("An IllegalArgumentException was expected for an unpriced order");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("printing charge has been calculated"));
		}

		verifyNoInteractions(paymentMock);
		verifyNoInteractions(emailInvoiceMock);
	}

	/**
	 * IT6 - INVALID PATH. An order that breaks a business rule is rejected while the
	 * order is being built, so the printer is never even consulted.
	 * BOUNDARY VALUE ANALYSIS is applied at the system level here.
	 */
	@Test
	@Parameters({ "0, 1", "501, 1", "10, 0", "10, 1001", "-1, -1" })
	public void testIT6InvalidOrderNeverReachesTheExternalModules(int numberOfPages,
			int numberOfCopies) {

		// the printer double is deliberately left unprogrammed: the order must be
		// rejected before any external module is contacted

		try {
			printOrderService.createOrderForExistingCustomer("ORD032", "C0001", PrintType.COLOUR,
					PaperSize.A4, PrintingSide.SINGLE_SIDED, numberOfPages, numberOfCopies,
					BindingOption.NONE, false, false);
			fail("An IllegalArgumentException was expected for " + numberOfPages + " pages and "
					+ numberOfCopies + " copies");
		} catch (IllegalArgumentException e) {
			// expected : the order breaks a business rule
		}

		verifyNoInteractions(printerAvailabilityMock);
		verifyNoInteractions(paymentMock);
		verifyNoInteractions(emailInvoiceMock);
	}
}
