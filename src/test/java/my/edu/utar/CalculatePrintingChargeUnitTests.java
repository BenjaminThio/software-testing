package my.edu.utar;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.FileParameters;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.mappers.CsvWithHeaderMapper;

/**
 * Unit tests for CalculatePrintingCharge (FR5 - Calculate Printing Charges).
 *
 * This class tests the CALCULATION methods in isolation. The end to end
 * calculateTotalCharge method, which collaborates with the external printer
 * availability module and with applyDiscount, is tested separately in
 * CalculatePrintingChargeTestDoubleTests.
 *
 * Test design techniques applied in this class:
 *   - Equivalence Partitioning across every row of Table 2, so each of the twelve
 *     paper size / print type / printing side combinations is exercised once;
 *   - Equivalence Partitioning across every row of Table 3 for the optional services,
 *     including the combinations of binding, lamination and express printing;
 *   - Boundary Value Analysis on the number of pages (0 / 1 / 500 / 501) and the
 *     number of copies (0 / 1 / 1000 / 1001);
 *   - The expected rates and charges are READ FROM TEXT FILES
 *     (base-rate-test-data.csv, base-charge-test-data.csv,
 *     optional-service-test-data.csv and invalid-order-test-data.csv) instead of
 *     being hardcoded, so the pricing tables can be re-priced without editing code.
 */
@RunWith(JUnitParamsRunner.class)
public class CalculatePrintingChargeUnitTests {

	/** Tolerance used when comparing monetary amounts held in a double. */
	private static final double DELTA = 0.001;

	private CalculatePrintingCharge calculatePrintingCharge;

	@Before
	public void setupForAllTests() {
		// The printer availability collaborator is a DUMMY here: none of the methods
		// tested in this class calls it, it only has to exist so the object can be
		// constructed. ApplyDiscount is the real class because these tests do not
		// exercise the discount path either.
		calculatePrintingCharge = new CalculatePrintingCharge(mock(PrinterAvailabilityService.class),
				new ApplyDiscount());
	}

	// ----------------------------------------------------------------- VALID CASES

	/**
	 * VALID CASE - parameterised, test data read from an external text file.
	 * Equivalence Partitioning : one test per row of Table 2, covering all three paper
	 * sizes, both print types and both printing sides.
	 */
	@Test
	@FileParameters(value = "base-rate-test-data.csv", mapper = CsvWithHeaderMapper.class)
	public void testGetBaseRateMatchesTable2(String paperSize, String printType,
			String printingSide, double expectedRate) {

		assertEquals(expectedRate,
				calculatePrintingCharge.getBaseRate(paperSize, printType, printingSide), DELTA);
	}

	/**
	 * VALID CASE - parameterised, test data read from an external text file.
	 * Verifies Base Charge = Base Rate x Number of Pages x Number of Copies for a
	 * representative order in every partition of Table 2, and includes the boundary
	 * combinations 1 page x 1 copy and 500 pages x 1000 copies.
	 */
	@Test
	@FileParameters(value = "base-charge-test-data.csv", mapper = CsvWithHeaderMapper.class)
	public void testCalculateBaseCharge(String paperSize, String printType, String printingSide,
			int numberOfPages, int numberOfCopies, double expectedBaseCharge) {

		assertEquals(expectedBaseCharge, calculatePrintingCharge.calculateBaseCharge(paperSize,
				printType, printingSide, numberOfPages, numberOfCopies), DELTA);
	}

	/**
	 * VALID CASE - Boundary Value Analysis on the number of pages and the number of
	 * copies. The valid partitions are 1 to 500 pages and 1 to 1000 copies, so the
	 * values tested here are the two boundaries of each range and the value just
	 * inside each boundary.
	 */
	@Test
	@Parameters({ "1, 1, 0.20",
			"2, 1, 0.40",
			"500, 1, 100.00",
			"499, 1, 99.80",
			"1, 1000, 200.00",
			"1, 999, 199.80",
			"500, 1000, 100000.00" })
	public void testCalculateBaseChargeAtTheBoundaries(int numberOfPages, int numberOfCopies,
			double expectedBaseCharge) {

		// A4 Black & White single-sided is RM0.20 per page
		assertEquals(expectedBaseCharge, calculatePrintingCharge.calculateBaseCharge(PaperSize.A4,
				PrintType.BLACK_AND_WHITE, PrintingSide.SINGLE_SIDED, numberOfPages, numberOfCopies),
				DELTA);
	}

	/**
	 * VALID CASE - parameterised, test data read from an external text file.
	 * Equivalence Partitioning across Table 3: no service, each binding option on its
	 * own, lamination on its own, express printing on its own, and combinations.
	 * Lamination is charged on pages x copies, express printing is a flat charge.
	 */
	@Test
	@FileParameters(value = "optional-service-test-data.csv", mapper = CsvWithHeaderMapper.class)
	public void testCalculateOptionalServiceCharge(String bindingOption, boolean laminationRequired,
			boolean expressPrintingRequired, int numberOfPages, int numberOfCopies,
			double expectedCharge) {

		assertEquals(expectedCharge, calculatePrintingCharge.calculateOptionalServiceCharge(
				bindingOption, laminationRequired, expressPrintingRequired, numberOfPages,
				numberOfCopies), DELTA);
	}

	/**
	 * VALID CASE - the flat binding charges of Table 3, verified one option at a time.
	 * Only one binding option may be selected per order, which is why the options are
	 * mutually exclusive values rather than independent flags.
	 */
	@Test
	@Parameters({ "None, 0.00",
			"Staple Binding, 2.00",
			"Comb Binding, 5.00",
			"Spiral Binding, 8.00" })
	public void testBindingChargesMatchTable3(String bindingOption, double expectedCharge) {

		assertEquals(expectedCharge, BindingOption.fromString(bindingOption).getCharge(), DELTA);
		assertEquals(expectedCharge, calculatePrintingCharge.calculateOptionalServiceCharge(
				bindingOption, false, false, 10, 1), DELTA);
	}

	/**
	 * VALID CASE - lamination is charged per PRINTED page, that is pages x copies, not
	 * per page of the document. This rule is easy to get wrong, so it is asserted on
	 * its own.
	 */
	@Test
	@Parameters({ "1, 1, 1.50",
			"10, 1, 15.00",
			"10, 3, 45.00",
			"100, 2, 300.00",
			"500, 1000, 750000.00" })
	public void testLaminationIsChargedPerPrintedPage(int numberOfPages, int numberOfCopies,
			double expectedCharge) {

		assertEquals(expectedCharge, calculatePrintingCharge.calculateOptionalServiceCharge(
				BindingOption.NONE, true, false, numberOfPages, numberOfCopies), DELTA);
	}

	/**
	 * VALID CASE - express printing is a flat RM20.00 per order, so it does not vary
	 * with the number of pages or copies.
	 */
	@Test
	@Parameters({ "1, 1", "50, 2", "500, 1000" })
	public void testExpressPrintingIsAFlatChargePerOrder(int numberOfPages, int numberOfCopies) {

		assertEquals(20.00, calculatePrintingCharge.calculateOptionalServiceCharge(
				BindingOption.NONE, false, true, numberOfPages, numberOfCopies), DELTA);
	}

	/**
	 * VALID CASE - the rounding rule. Every monetary amount is rounded to two decimal
	 * places using half up rounding.
	 */
	@Test
	@Parameters({ "1.004, 1.00",
			"1.005, 1.01",
			"1.006, 1.01",
			"104.79375, 104.79",
			"345.20625, 345.21",
			"0.0, 0.0" })
	public void testRoundToTwoDecimals(double value, double expectedResult) {

		assertEquals(expectedResult, CalculatePrintingCharge.roundToTwoDecimals(value), DELTA);
	}

	// --------------------------------------------------------------- INVALID CASES

	/**
	 * INVALID CASE - parameterised, test data read from an external text file.
	 * Each row of invalid-order-test-data.csv holds one invalid printing option or one
	 * page/copy value outside its permitted range, together with the reason it must be
	 * rejected. The expected exception is declared on the @Test annotation.
	 */
	@Test(expected = IllegalArgumentException.class)
	@FileParameters(value = "invalid-order-test-data.csv", mapper = CsvWithHeaderMapper.class)
	public void testCalculateBaseChargeRejectsInvalidInput(String paperSize, String printType,
			String printingSide, int numberOfPages, int numberOfCopies, String reasonForRejection) {

		calculatePrintingCharge.calculateBaseCharge(paperSize, printType, printingSide,
				numberOfPages, numberOfCopies);
	}

	/**
	 * INVALID CASE - Boundary Value Analysis on the number of pages.
	 * The valid partition is 1 to 500, so 0 and -1 sit in the invalid low partition
	 * and 501 sits in the invalid high partition.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters({ "0", "-1", "-500", "501", "1000" })
	public void testNumberOfPagesOutsideTheValidPartitionIsRejected(int numberOfPages) {

		calculatePrintingCharge.calculateBaseCharge(PaperSize.A4, PrintType.BLACK_AND_WHITE,
				PrintingSide.SINGLE_SIDED, numberOfPages, 1);
	}

	/**
	 * INVALID CASE - Boundary Value Analysis on the number of copies.
	 * The valid partition is 1 to 1000, so 0 and -1 sit in the invalid low partition
	 * and 1001 sits in the invalid high partition.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters({ "0", "-1", "-100", "1001", "5000" })
	public void testNumberOfCopiesOutsideTheValidPartitionIsRejected(int numberOfCopies) {

		calculatePrintingCharge.calculateBaseCharge(PaperSize.A4, PrintType.BLACK_AND_WHITE,
				PrintingSide.SINGLE_SIDED, 10, numberOfCopies);
	}

	/**
	 * INVALID CASE - the same page and copy limits are enforced when only optional
	 * services are being priced, because lamination depends on pages x copies.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters({ "0, 1", "501, 1", "10, 0", "10, 1001" })
	public void testOptionalServiceChargeRejectsInvalidPagesOrCopies(int numberOfPages,
			int numberOfCopies) {

		calculatePrintingCharge.calculateOptionalServiceCharge(BindingOption.SPIRAL_BINDING, true,
				true, numberOfPages, numberOfCopies);
	}

	/**
	 * INVALID CASE - unsupported printing options are rejected, one invalid partition
	 * per row.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters({ "A2, Black & White, Single-sided",
			"A0, Colour, Double-sided",
			"Letter, Colour, Single-sided",
			"A4, Greyscale, Single-sided",
			"A4, Sepia, Double-sided",
			"A4, Colour, Triple-sided",
			"A4, Colour, Both-sides" })
	public void testUnsupportedPrintingOptionIsRejected(String paperSize, String printType,
			String printingSide) {

		calculatePrintingCharge.getBaseRate(paperSize, printType, printingSide);
	}

	/**
	 * INVALID CASE - a blank printing option is rejected before any table lookup.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters(method = "getNullAndBlankPrintingOptions")
	public void testNullOrBlankPrintingOptionIsRejected(String paperSize, String printType,
			String printingSide) {

		calculatePrintingCharge.getBaseRate(paperSize, printType, printingSide);
	}

	private Object[] getNullAndBlankPrintingOptions() {
		return new Object[] {
				new Object[] { null, "Colour", "Single-sided" },
				new Object[] { "A4", null, "Single-sided" },
				new Object[] { "A4", "Colour", null },
				new Object[] { "", "Colour", "Single-sided" },
				new Object[] { "A4", "   ", "Single-sided" },
				new Object[] { "A4", "Colour", "" }
		};
	}

	/**
	 * INVALID CASE - the enum based overload rejects nulls too.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters(method = "getNullEnumPrintingOptions")
	public void testNullEnumPrintingOptionIsRejected(PaperSize paperSize, PrintType printType,
			PrintingSide printingSide) {

		calculatePrintingCharge.getBaseRate(paperSize, printType, printingSide);
	}

	private Object[] getNullEnumPrintingOptions() {
		return new Object[] {
				new Object[] { null, PrintType.COLOUR, PrintingSide.SINGLE_SIDED },
				new Object[] { PaperSize.A4, null, PrintingSide.SINGLE_SIDED },
				new Object[] { PaperSize.A4, PrintType.COLOUR, null }
		};
	}

	/**
	 * INVALID CASE - a null binding option is rejected.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters(method = "getNullBindingOption")
	public void testNullBindingOptionIsRejected(BindingOption bindingOption) {

		calculatePrintingCharge.calculateOptionalServiceCharge(bindingOption, false, false, 10, 1);
	}

	private Object[] getNullBindingOption() {
		return new Object[] { new Object[] { (BindingOption) null } };
	}

	/**
	 * INVALID CASE - unsupported binding options are rejected.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters({ "Glue Binding", "Hardcover", "Ring", "" })
	public void testUnsupportedBindingOptionIsRejected(String bindingOption) {

		calculatePrintingCharge.calculateOptionalServiceCharge(bindingOption, false, false, 10, 1);
	}

	/**
	 * INVALID CASE - the class cannot be constructed without its collaborators,
	 * because it would then be unable to check printer availability or obtain
	 * discounts.
	 */
	@Test(expected = IllegalArgumentException.class)
	@Parameters(method = "getInvalidConstructorArguments")
	public void testConstructorRejectsMissingCollaborators(
			PrinterAvailabilityService printerAvailabilityService, ApplyDiscount applyDiscount) {

		new CalculatePrintingCharge(printerAvailabilityService, applyDiscount);
	}

	private Object[] getInvalidConstructorArguments() {
		return new Object[] {
				new Object[] { null, new ApplyDiscount() },
				new Object[] { mock(PrinterAvailabilityService.class), null },
				new Object[] { null, null }
		};
	}

	/**
	 * INVALID CASE - a null print order cannot be priced.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCalculateTotalChargeRejectsANullOrder() {

		calculatePrintingCharge.calculateTotalCharge(null);
	}
}
