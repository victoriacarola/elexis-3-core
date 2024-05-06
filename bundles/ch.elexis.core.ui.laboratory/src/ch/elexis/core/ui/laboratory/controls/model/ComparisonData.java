/**
 * Diese Klasse repräsentiert die Vergleichsdaten für Laborergebnisse.
 * Sie enthält fünf Werte sowie entsprechende Checkbox-Zustände für den Vergleich.
 * 
 * @author  Victoria Carola Adolph
 * @version 1.0
 * @since   06.05.2024
 */

package ch.elexis.core.ui.laboratory.controls.model;

public class ComparisonData {
	private double value1;
	private double value2;
	private double value3;
	private double value4;
	private double value5;

	private boolean checkbox1;
	private boolean checkbox2;
	private boolean checkbox3;
	private boolean checkbox4;
	private boolean checkbox5;

	public ComparisonData(double value1, double value2, double value3, double value4, double value5) {
		this.value1 = value1;
		this.value2 = value2;
		this.value3 = value3;
		this.value4 = value4;
		this.value5 = value5;
	}

	public double getValue1() {
		return value1;
	}

	public boolean isCheckbox1() {
		return checkbox1;
	}

	public void setCheckbox1(boolean checkbox1) {
		this.checkbox1 = checkbox1;
	}


	public boolean compareValuesAndCheckboxes(ComparisonData other) {
		return Double.compare(this.value1, other.value1) == 0 && Double.compare(this.value2, other.value2) == 0
				&& Double.compare(this.value3, other.value3) == 0 && Double.compare(this.value4, other.value4) == 0
				&& Double.compare(this.value5, other.value5) == 0 && this.checkbox1 == other.checkbox1
				&& this.checkbox2 == other.checkbox2 && this.checkbox3 == other.checkbox3
				&& this.checkbox4 == other.checkbox4 && this.checkbox5 == other.checkbox5;
	}

	public void setCheckbox2(boolean b) {
		// TODO Auto-generated method stub

	}

	public void setCheckbox3(boolean b) {
		// TODO Auto-generated method stub

	}

	public void setCheckbox4(boolean b) {
		// TODO Auto-generated method stub

	}

	public void setCheckbox5(boolean b) {
		// TODO Auto-generated method stub

	}
}