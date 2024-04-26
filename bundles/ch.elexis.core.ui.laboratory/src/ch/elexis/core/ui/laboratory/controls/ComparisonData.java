package ch.elexis.core.ui.laboratory.controls;

public class ComparisonData {
	private double value1;
	private double value2;
	private double value3;
	private double value4;
	private double value5;

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

	public double getValue2() {
		return value2;
	}

	public double getValue3() {
		return value3;
	}

	public double getValue4() {
		return value4;
	}

	public double getValue5() {
		return value5;
	}
}
