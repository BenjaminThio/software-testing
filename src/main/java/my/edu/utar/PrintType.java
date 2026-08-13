package my.edu.utar;

/**
 * Print types supported by the system.
 * Refer to Table 2 (Base Printing Charges) of the assignment specification.
 */
public enum PrintType {

	BLACK_AND_WHITE("Black & White"),
	COLOUR("Colour");

	private final String description;

	private PrintType(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	/**
	 * Converts a text value into a PrintType. Both the descriptive form
	 * ("Black &amp; White", "Colour") and the enum name ("BLACK_AND_WHITE") are accepted
	 * so that the same method can be used by the application and by file driven tests.
	 *
	 * @param value the print type as text, case insensitive
	 * @return the matching PrintType
	 * @throws IllegalArgumentException if the value is null, blank or not a supported print type
	 */
	public static PrintType fromString(String value) {

		if (value == null || value.trim().isEmpty())
			throw new IllegalArgumentException("Print type must not be empty");

		String cleanedValue = value.trim();
		for (PrintType printType : values()) {
			if (printType.description.equalsIgnoreCase(cleanedValue)
					|| printType.name().equalsIgnoreCase(cleanedValue))
				return printType;
		}
		throw new IllegalArgumentException("Invalid print type : " + value);
	}

	@Override
	public String toString() {
		return description;
	}
}
