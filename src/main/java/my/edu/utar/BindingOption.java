package my.edu.utar;

/**
 * Binding options and their charges.
 * Refer to Table 3 (Optional Service Charges) of the assignment specification.
 *
 * Business rule: only one binding option may be selected for each print order,
 * which is why the binding options are modelled as a single enumerated value
 * rather than as three independent boolean flags.
 */
public enum BindingOption {

	NONE("None", 0.00),
	STAPLE_BINDING("Staple Binding", 2.00),
	COMB_BINDING("Comb Binding", 5.00),
	SPIRAL_BINDING("Spiral Binding", 8.00);

	private final String description;
	private final double charge;

	private BindingOption(String description, double charge) {
		this.description = description;
		this.charge = charge;
	}

	public String getDescription() {
		return description;
	}

	/**
	 * @return the flat charge in Ringgit Malaysia for this binding option
	 */
	public double getCharge() {
		return charge;
	}

	/**
	 * Converts a text value into a BindingOption. Both the descriptive form
	 * ("Spiral Binding") and the enum name ("SPIRAL_BINDING") are accepted.
	 *
	 * @param value the binding option as text, case insensitive
	 * @return the matching BindingOption
	 * @throws IllegalArgumentException if the value is null, blank or not a supported binding option
	 */
	public static BindingOption fromString(String value) {

		if (value == null || value.trim().isEmpty())
			throw new IllegalArgumentException("Binding option must not be empty");

		String cleanedValue = value.trim();
		for (BindingOption bindingOption : values()) {
			if (bindingOption.description.equalsIgnoreCase(cleanedValue)
					|| bindingOption.name().equalsIgnoreCase(cleanedValue))
				return bindingOption;
		}
		throw new IllegalArgumentException("Invalid binding option : " + value);
	}

	@Override
	public String toString() {
		return description;
	}
}
