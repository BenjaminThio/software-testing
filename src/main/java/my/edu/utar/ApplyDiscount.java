package my.edu.utar;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * FR5 (part) - Discounts, Table 4 of the assignment specification.
 *
 * Receives the customer type and subtotal from {@link CalculatePrintingCharge},
 * calculates the applicable customer discount, applies any additional discount based
 * on the order subtotal and on the customer's order history, and returns the total
 * discount amount.
 *
 * Table 4 - Discounts
 *   Student (valid student ID required) ................ 10%
 *   Corporate customer ................................. 15%
 *   Order subtotal exceeds RM300 ....................... additional 5%
 *   Existing customer with more than 20 previous orders  additional 5%
 *
 * The specification states that multiple discounts are cumulative and applied
 * SEQUENTIALLY to the subtotal after all optional service charges have been added.
 * Sequential application is therefore implemented as successive reductions, not as a
 * single summed percentage:
 *
 *   amount payable = subtotal x (1 - customer type rate)
 *                             x (1 - large order rate)
 *                             x (1 - loyalty rate)
 *
 *   total discount = subtotal - amount payable
 *
 * A worked example: a corporate customer with 25 previous orders and a subtotal of
 * RM450.00 pays 450.00 x 0.85 x 0.95 x 0.95 = RM345.21, so the total discount is
 * RM104.79. Refer to the Assumptions section of the report.
 *
 * Note that the "exceeds RM300" test is applied to the ORIGINAL subtotal, not to the
 * running amount, because Table 4 describes the condition in terms of the order
 * subtotal.
 */
public class ApplyDiscount {

	public static final double STUDENT_DISCOUNT_RATE = 0.10;
	public static final double CORPORATE_DISCOUNT_RATE = 0.15;
	public static final double LARGE_ORDER_DISCOUNT_RATE = 0.05;
	public static final double LOYAL_CUSTOMER_DISCOUNT_RATE = 0.05;

	/** An order subtotal must EXCEED this value to earn the additional 5%. */
	public static final double LARGE_ORDER_THRESHOLD = 300.00;

	/** A customer must have MORE THAN this many previous orders to earn the additional 5%. */
	public static final int LOYAL_CUSTOMER_MIN_ORDERS = 20;

	/**
	 * Calculates the total discount amount for a customer.
	 *
	 * @param customer the customer placing the order
	 * @param subtotal the order subtotal, after optional service charges have been added
	 * @return the total discount amount, rounded to two decimal places
	 * @throws IllegalArgumentException if the customer is null or the subtotal is negative
	 */
	public double calculateDiscount(Customer customer, double subtotal) {

		if (customer == null)
			throw new IllegalArgumentException("Customer must not be null");

		return calculateDiscount(customer.getCustomerType(), subtotal, customer.getPreviousOrderCount());
	}

	/**
	 * Calculates the total discount amount from the raw discount inputs. This form is
	 * used by the parameterised test code, which feeds the customer type, subtotal and
	 * previous order count directly from a data file.
	 *
	 * @param customerType the type of customer (Regular, Student or Corporate)
	 * @param subtotal the order subtotal, after optional service charges have been added
	 * @param previousOrderCount the number of orders the customer has placed previously
	 * @return the total discount amount, rounded to two decimal places
	 * @throws IllegalArgumentException if the customer type is null, the subtotal is
	 *         negative, or the previous order count is negative
	 */
	public double calculateDiscount(CustomerType customerType, double subtotal, int previousOrderCount) {

		if (customerType == null)
			throw new IllegalArgumentException("Customer type must not be null");
		if (subtotal < 0)
			throw new IllegalArgumentException("Subtotal must not be negative : " + subtotal);
		if (previousOrderCount < 0)
			throw new IllegalArgumentException("Previous order count must not be negative : "
					+ previousOrderCount);

		double amountPayable = subtotal;

		// 1. customer type discount : Student 10%, Corporate 15%, Regular none
		amountPayable = amountPayable * (1 - customerType.getDiscountRate());

		// 2. additional 5% when the order subtotal exceeds RM300
		if (isEligibleForLargeOrderDiscount(subtotal))
			amountPayable = amountPayable * (1 - LARGE_ORDER_DISCOUNT_RATE);

		// 3. additional 5% for an existing customer with more than 20 previous orders
		if (isEligibleForLoyalCustomerDiscount(previousOrderCount))
			amountPayable = amountPayable * (1 - LOYAL_CUSTOMER_DISCOUNT_RATE);

		return roundToTwoDecimals(subtotal - amountPayable);
	}

	/**
	 * @return the amount still payable after every applicable discount has been applied
	 * @throws IllegalArgumentException if any input is invalid
	 */
	public double getAmountAfterDiscount(CustomerType customerType, double subtotal,
			int previousOrderCount) {
		return roundToTwoDecimals(subtotal - calculateDiscount(customerType, subtotal, previousOrderCount));
	}

	/**
	 * @return true when the subtotal strictly exceeds RM300.00, so RM300.00 itself is
	 *         NOT eligible while RM300.01 is
	 */
	public boolean isEligibleForLargeOrderDiscount(double subtotal) {
		return subtotal > LARGE_ORDER_THRESHOLD;
	}

	/**
	 * @return true when the customer has strictly more than 20 previous orders, so 20
	 *         previous orders is NOT eligible while 21 is
	 */
	public boolean isEligibleForLoyalCustomerDiscount(int previousOrderCount) {
		return previousOrderCount > LOYAL_CUSTOMER_MIN_ORDERS;
	}

	/**
	 * Rounds a monetary amount to two decimal places using half up rounding, which is
	 * the rounding rule used throughout the system.
	 */
	public static double roundToTwoDecimals(double value) {
		return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}
}
