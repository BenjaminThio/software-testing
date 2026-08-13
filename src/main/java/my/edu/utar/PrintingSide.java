package my.edu.utar;

/**
 * Printing sides supported by the system.
 * Refer to Table 2 (Base Printing Charges) of the assignment specification.
 */
public enum PrintingSide {

	SINGLE_SIDED("Single-sided"),
	DOUBLE_SIDED("Double-sided");

	private final String description;

	private PrintingSide(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	/**
	 * Converts a text value into a PrintingSide. Both the descriptive form
	 * ("Single-sided") and the enum name ("SINGLE_SIDED") are accepted.
	 *
	 * @param value the printing side as text, case insensitive
	 * @return the matching PrintingSide
	 * @throws IllegalArgumentException if the value is null, blank or not a supported printing side
	 */
	public static PrintingSide fromString(String value) {

		if (value == null || value.trim().isEmpty())
			throw new IllegalArgumentException("Printing side must not be empty");

		String cleanedValue = value.trim();
		for (PrintingSide printingSide : values()) {
			if (printingSide.description.equalsIgnoreCase(cleanedValue)
					|| printingSide.name().equalsIgnoreCase(cleanedValue))
				return printingSide;
		}
		throw new IllegalArgumentException("Invalid printing side : " + value);
	}

	@Override
	public String toString() {
		return description;
	}
}
