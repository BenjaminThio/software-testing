package my.edu.utar;

/**
 * Appendix A - Printer Availability Module (Mocked).
 *
 * This class represents the external printer availability module. The real module
 * has been developed by another team and is NOT part of this assignment, therefore
 * the method body is not implemented here.
 *
 * The class exists so that the collaboration is visible in the class diagram and so
 * that the test code can create a Mockito test double from it. Every test that needs
 * a printer availability answer stubs this collaborator; the real implementation is
 * never invoked by the test suite.
 */
public class PrinterAvailability implements PrinterAvailabilityService {

	/** Message displayed by the system when no suitable printer is available. */
	public static final String UNAVAILABLE_MESSAGE = "Selected printer is currently unavailable.";

	/**
	 * Checks whether a suitable printer is available for the selected paper size and
	 * print type.
	 *
	 * Not to be developed - this module is external to the assignment and is always
	 * replaced by a test double during testing.
	 *
	 * @param paperSize selected paper size (A3, A4 or A5)
	 * @param printType selected printing type (Black &amp; White or Colour)
	 * @return true when a suitable printer is available
	 */
	@Override
	public boolean isPrinterAvailable(String paperSize, String printType) {
		throw new UnsupportedOperationException(
				"printerAvailability is an external module and is not implemented in this assignment");
	}
}
