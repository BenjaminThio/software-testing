package my.edu.utar;

/**
 * Appendix A - Printer Availability Module (Mocked).
 *
 * The Printer Availability Module is an external service that checks whether a
 * printer is available to process a print job. It has already been developed by
 * another team and is not part of this assignment, so only the interface it exposes
 * is declared here.
 *
 * Declaring the collaboration as an interface allows {@link CalculatePrintingCharge}
 * to depend on the contract rather than on a concrete class, so the test code can
 * substitute a Mockito test double in its place.
 */
public interface PrinterAvailabilityService {

	/**
	 * Checks whether a suitable printer is available for the selected paper size and
	 * print type.
	 *
	 * @param paperSize selected paper size (A3, A4 or A5)
	 * @param printType selected printing type (Black &amp; White or Colour)
	 * @return true when a suitable printer is available and the print order may
	 *         proceed, false when no suitable printer is available
	 */
	public boolean isPrinterAvailable(String paperSize, String printType);
}
