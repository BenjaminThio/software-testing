package my.edu.utar;

/**
 * Paper sizes supported by the PrintMaster Printing Service Management System.
 * Refer to Table 2 (Base Printing Charges) of the assignment specification.
 */
public enum PaperSize {

	A3("A3"),
	A4("A4"),
	A5("A5");

	private final String code;

	private PaperSize(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}

	/**
	 * Converts a text value (e.g. read from customer.txt, a CSV test data file or
	 * the user interface) into a PaperSize.
	 *
	 * @param value the paper size as text, case insensitive, surrounding blanks ignored
	 * @return the matching PaperSize
	 * @throws IllegalArgumentException if the value is null, blank or not a supported paper size
	 */
	public static PaperSize fromString(String value) {

		if (value == null || value.trim().isEmpty())
			throw new IllegalArgumentException("Paper size must not be empty");

		String cleanedValue = value.trim();
		for (PaperSize paperSize : values()) {
			if (paperSize.code.equalsIgnoreCase(cleanedValue))
				return paperSize;
		}
		throw new IllegalArgumentException("Invalid paper size : " + value);
	}

	@Override
	public String toString() {
		return code + " Paper";
	}
}
