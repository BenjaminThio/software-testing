package my.edu.utar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

/**
 * TEST DOUBLE tests for the two modules that are NOT to be developed:
 * payment (FR8) and emailInvoice (FR9).
 *
 * Both classes exist only as method signatures, so the only way to test the code that
 * depends on them is to replace them with test doubles. This class therefore
 * satisfies assessment component C.3 for the remaining external dependencies.
 *
 * What is verified here:
 *   - a SUCCESSFUL payment moves the order status to Completed and the payment status
 *     to Successful, and the invoice is then emailed (FR8 and FR9);
 *   - an UNSUCCESSFUL payment moves the order status to Pending Payment and the
 *     invoice is NOT emailed - proved with verify(..., never());
 *   - the payment module receives the TOTAL PRINTING CHARGE and the chosen payment
 *     method, captured with an ArgumentCaptor;
 *   - the email module receives the customer's email address and an invoice that
 *     really does contain the order details;
 *   - payment is taken BEFORE the invoice is emailed, proved with InOrder.
 */
@RunWith(JUnitParamsRunner.class)
public class PaymentAndEmailInvoiceTestDoubleTests {

	private static final double DELTA = 0.001;

	private PrinterAvailabilityService printerAvailabilityMock;
	private Payment paymentMock;
	private EmailInvoice emailInvoiceMock;
	private PrintOrderService printOrderService;

	private Customer customer;

	@Before
	public void setupForAllTests() {

		// the three external modules are replaced by test doubles
		printerAvailabilityMock = mock(PrinterAvailabilityService.class);
		paymentMock = mock(Payment.class);
		emailInvoiceMock = mock(EmailInvoice.class);

		// the modules developed in this assignment are the real ones
		ReadCustomer readCustomer = new ReadCustomer("customer-test-records.txt");
		// this class never registers a customer, so the registration module is pointed at
		// a scratch file to guarantee the fixture file cannot be modified by these tests
		AddNewCustomer addNewCustomer = new AddNewCustomer("payment-tests-scratch.txt", readCustomer);
		CalculatePrintingCharge calculatePrintingCharge = new CalculatePrintingCharge(
				printerAvailabilityMock, new ApplyDiscount());

		printOrderService = new PrintOrderService(readCustomer, addNewCustomer,
				calculatePrintingCharge, new GenerateInvoice(), paymentMock, emailInvoiceMock);

		customer = new Customer("C0001", "Ali Bin Ahmad", "ali.ahmad@example.com", "0123456789",
				CustomerType.STUDENT, 3);
	}

	/**
	 * Builds a priced order. A4 Colour single-sided, 100 pages, 1 copy = RM80.00 base,
	 * no optional services, student discount 10% = RM8.00, total RM72.00.
	 */
	private PrintOrder buildPricedOrder() {

		when(printerAvailabilityMock.isPrinterAvailable(anyString(), anyString())).thenReturn(true);

		return printOrderService.createOrder("ORD001", customer, PrintType.COLOUR, PaperSize.A4,
				PrintingSide.SINGLE_SIDED, 100, 1, BindingOption.NONE, false, false);
	}

	// ----------------------------------------------------------------- VALID CASES

	/**
	 * VALID CASE using STUBS for payment and emailInvoice - parameterised over every
	 * supported payment method.
	 *
	 * FR8 : once payment is successful the order status is updated to Completed.
	 * FR9 : once payment is successful an invoice is emailed to the customer.
	 */
	@Test
	@Parameters({ "E_WALLET", "CREDIT_CARD", "ONLINE_BANKING" })
	public void testSuccessfulPaymentCompletesTheOrderAndEmailsTheInvoice(String paymentMethod) {

		PrintOrder printOrder = buildPricedOrder();
		PaymentMethod method = PaymentMethod.valueOf(paymentMethod);

		// STUB : the external payment module reports success
		when(paymentMock.makePayment(anyDouble(), any(PaymentMethod.class))).thenReturn(true);
		// STUB : the external email module reports that the invoice was sent
		when(emailInvoiceMock.sendInvoice(anyString(), anyString())).thenReturn(true);

		assertTrue(printOrderService.completeOrder(printOrder, method));

		assertEquals(PaymentStatus.SUCCESSFUL, printOrder.getPaymentStatus());
		assertEquals(OrderStatus.COMPLETED, printOrder.getOrderStatus());

		// MOCK : payment was taken once, for the total charge, using the chosen method
		verify(paymentMock, times(1)).makePayment(72.00, method);
		// MOCK : the invoice went to the customer's registered email address exactly once
		verify(emailInvoiceMock, times(1)).sendInvoice(eq("ali.ahmad@example.com"), anyString());
	}

	/**
	 * VALID CASE using a MOCK with ArgumentCaptors.
	 *
	 * The payment module must receive the TOTAL printing charge, that is the amount
	 * after the discount, not the subtotal. Capturing both arguments proves exactly
	 * what crossed the module boundary.
	 */
	@Test
	public void testPaymentModuleReceivesTheDiscountedTotal() {

		PrintOrder printOrder = buildPricedOrder();

		when(paymentMock.makePayment(anyDouble(), any(PaymentMethod.class))).thenReturn(true);
		when(emailInvoiceMock.sendInvoice(anyString(), anyString())).thenReturn(true);

		printOrderService.completeOrder(printOrder, PaymentMethod.CREDIT_CARD);

		ArgumentCaptor<Double> amountCaptor = ArgumentCaptor.forClass(Double.class);
		ArgumentCaptor<PaymentMethod> methodCaptor = ArgumentCaptor.forClass(PaymentMethod.class);
		verify(paymentMock).makePayment(amountCaptor.capture(), methodCaptor.capture());

		assertEquals("the subtotal before the discount", 80.00, printOrder.getSubtotal(), DELTA);
		assertEquals("the amount sent to the payment module", 72.00,
				amountCaptor.getValue().doubleValue(), DELTA);
		assertEquals(PaymentMethod.CREDIT_CARD, methodCaptor.getValue());
	}

	/**
	 * VALID CASE using a MOCK with an ArgumentCaptor.
	 *
	 * FR9 states what the emailed invoice must contain. Capturing the invoice text
	 * handed to the external module lets the test assert on its contents.
	 */
	@Test
	public void testEmailedInvoiceCarriesTheCustomerAndOrderDetails() {

		PrintOrder printOrder = buildPricedOrder();

		when(paymentMock.makePayment(anyDouble(), any(PaymentMethod.class))).thenReturn(true);
		when(emailInvoiceMock.sendInvoice(anyString(), anyString())).thenReturn(true);

		printOrderService.completeOrder(printOrder, PaymentMethod.E_WALLET);

		ArgumentCaptor<String> addressCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> invoiceCaptor = ArgumentCaptor.forClass(String.class);
		verify(emailInvoiceMock).sendInvoice(addressCaptor.capture(), invoiceCaptor.capture());

		assertEquals("ali.ahmad@example.com", addressCaptor.getValue());

		String emailedInvoice = invoiceCaptor.getValue();
		assertTrue("customer details", emailedInvoice.contains("Ali Bin Ahmad"));
		assertTrue("customer id", emailedInvoice.contains("C0001"));
		assertTrue("order details", emailedInvoice.contains("ORD001"));
		assertTrue("charge breakdown", emailedInvoice.contains("Base Printing Charge"));
		assertTrue("discount applied", emailedInvoice.contains("Discount"));
		assertTrue("final amount paid", emailedInvoice.contains("RM72.00"));
		assertTrue("payment status", emailedInvoice.contains("Successful"));
	}

	/**
	 * VALID CASE using a MOCK for in-order verification.
	 *
	 * The invoice may only be emailed AFTER payment has succeeded, so the order of the
	 * two calls is part of the specification and is asserted directly.
	 */
	@Test
	public void testPaymentIsTakenBeforeTheInvoiceIsEmailed() {

		PrintOrder printOrder = buildPricedOrder();

		when(paymentMock.makePayment(anyDouble(), any(PaymentMethod.class))).thenReturn(true);
		when(emailInvoiceMock.sendInvoice(anyString(), anyString())).thenReturn(true);

		printOrderService.completeOrder(printOrder, PaymentMethod.ONLINE_BANKING);

		InOrder inOrder = inOrder(paymentMock, emailInvoiceMock);
		inOrder.verify(paymentMock).makePayment(72.00, PaymentMethod.ONLINE_BANKING);
		inOrder.verify(emailInvoiceMock).sendInvoice(eq("ali.ahmad@example.com"), anyString());
	}

	// --------------------------------------------------------------- INVALID CASES

	/**
	 * INVALID CASE using STUBS - parameterised over every payment method.
	 *
	 * FR8 : if payment is unsuccessful the order status is updated to Pending Payment.
	 * FR9 : the invoice is emailed only once payment is successful, so an unsuccessful
	 * payment must NOT trigger an email. verify(..., never()) proves the negative.
	 */
	@Test
	@Parameters({ "E_WALLET", "CREDIT_CARD", "ONLINE_BANKING" })
	public void testUnsuccessfulPaymentLeavesTheOrderPendingAndSendsNoEmail(String paymentMethod) {

		PrintOrder printOrder = buildPricedOrder();

		// STUB : the external payment module reports failure
		when(paymentMock.makePayment(anyDouble(), any(PaymentMethod.class))).thenReturn(false);

		assertFalse(printOrderService.completeOrder(printOrder, PaymentMethod.valueOf(paymentMethod)));

		assertEquals(PaymentStatus.UNSUCCESSFUL, printOrder.getPaymentStatus());
		assertEquals(OrderStatus.PENDING_PAYMENT, printOrder.getOrderStatus());

		// MOCK : no invoice may be emailed when payment failed
		verify(emailInvoiceMock, never()).sendInvoice(anyString(), anyString());
		verifyNoInteractions(emailInvoiceMock);
	}

	/**
	 * INVALID CASE using a STUB that throws.
	 *
	 * A payment gateway can fail outright rather than simply declining. The failure
	 * must reach the caller, and no invoice may be emailed.
	 */
	@Test
	public void testPaymentGatewayFailureIsNotSwallowed() {

		PrintOrder printOrder = buildPricedOrder();

		when(paymentMock.makePayment(anyDouble(), any(PaymentMethod.class)))
				.thenThrow(new RuntimeException("Payment gateway is unreachable"));

		try {
			printOrderService.completeOrder(printOrder, PaymentMethod.E_WALLET);
			org.junit.Assert.fail("The payment gateway failure should have reached the caller");
		} catch (RuntimeException e) {
			assertEquals("Payment gateway is unreachable", e.getMessage());
		}

		verifyNoInteractions(emailInvoiceMock);
	}

	/**
	 * INVALID CASE using a STUB.
	 *
	 * Payment succeeded but the email could not be delivered. completeOrder reports
	 * false, yet the order itself stays Completed because the customer HAS paid.
	 */
	@Test
	public void testFailedEmailDeliveryDoesNotUndoASuccessfulPayment() {

		PrintOrder printOrder = buildPricedOrder();

		when(paymentMock.makePayment(anyDouble(), any(PaymentMethod.class))).thenReturn(true);
		// STUB : the email module could not deliver the invoice
		when(emailInvoiceMock.sendInvoice(anyString(), anyString())).thenReturn(false);

		assertFalse(printOrderService.completeOrder(printOrder, PaymentMethod.CREDIT_CARD));

		assertEquals(PaymentStatus.SUCCESSFUL, printOrder.getPaymentStatus());
		assertEquals(OrderStatus.COMPLETED, printOrder.getOrderStatus());
		verify(emailInvoiceMock, times(1)).sendInvoice(anyString(), anyString());
	}

	/**
	 * INVALID CASE using a MOCK.
	 *
	 * Payment may not be taken for an order whose charge has never been calculated,
	 * for example because no suitable printer was available. Neither external module
	 * may be touched.
	 */
	@Test
	public void testPaymentIsRefusedForAnUnpricedOrder() {

		PrintOrder unpricedOrder = new PrintOrder("ORD002", customer, PrintType.COLOUR,
				PaperSize.A4, PrintingSide.SINGLE_SIDED, 100, 1);

		try {
			printOrderService.completeOrder(unpricedOrder, PaymentMethod.E_WALLET);
			org.junit.Assert.fail("An IllegalArgumentException was expected for an unpriced order");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("printing charge has been calculated"));
		}

		verifyNoInteractions(paymentMock);
		verifyNoInteractions(emailInvoiceMock);
	}

	/**
	 * INVALID CASE - a null order or a null payment method is rejected before any
	 * external module is contacted.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters(method = "getInvalidCompleteOrderArguments")
	public void testCompleteOrderRejectsInvalidArguments(PrintOrder printOrder,
			PaymentMethod paymentMethod) {

		printOrderService.completeOrder(printOrder, paymentMethod);
	}

	private Object[] getInvalidCompleteOrderArguments() {

		Customer aCustomer = new Customer("C0001", "Ali Bin Ahmad", "ali.ahmad@example.com",
				"0123456789", CustomerType.STUDENT, 3);
		PrintOrder anUnpricedOrder = new PrintOrder("ORD003", aCustomer, PrintType.COLOUR,
				PaperSize.A4, PrintingSide.SINGLE_SIDED, 10, 1);

		return new Object[] {
				new Object[] { null, PaymentMethod.E_WALLET },
				new Object[] { anUnpricedOrder, PaymentMethod.E_WALLET },
				new Object[] { anUnpricedOrder, null }
		};
	}

	/**
	 * INVALID CASE - the real payment and emailInvoice classes are deliberately not
	 * implemented, so calling them directly must fail loudly. This test documents WHY
	 * a test double is compulsory for these two modules rather than optional.
	 */
	@Test(expected = UnsupportedOperationException.class)
	public void testTheRealPaymentModuleIsNotImplemented() {

		new Payment().makePayment(100.00, PaymentMethod.E_WALLET);
	}

	/**
	 * INVALID CASE - as above, for the email module.
	 */
	@Test(expected = UnsupportedOperationException.class)
	public void testTheRealEmailInvoiceModuleIsNotImplemented() {

		new EmailInvoice().sendInvoice("ali.ahmad@example.com", "invoice text");
	}

	/**
	 * INVALID CASE - the real printer availability module is external too, and is
	 * likewise never executed by the test suite.
	 */
	@Test(expected = UnsupportedOperationException.class)
	public void testTheRealPrinterAvailabilityModuleIsNotImplemented() {

		new PrinterAvailability().isPrinterAvailable("A4", "Colour");
	}
}
