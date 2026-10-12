package darkspawn.black.equipment;

public final class TemperatureState {
	public static final int WARNING = 35, STRAIN = 60, DANGER = 85;
	public float exposure;
	public float target;
	public int wetSeconds;
	public int dangerSeconds;

	public void restore(float exposure, int wetSeconds, int dangerSeconds) {
		this.exposure = Float.isFinite(exposure) ? Math.clamp(exposure, -100, 100) : 0;
		this.wetSeconds = Math.clamp(wetSeconds, 0, 30);
		this.dangerSeconds = Math.clamp(dangerSeconds, 0, 4);
	}

	public boolean step(float target) {
		this.target = Math.clamp(target, -100, 100);
		// Thermal Inertia: Shelter and armor change the destination, never reset accumulated exposure.
		float rate = exposure * this.target < 0 || Math.abs(this.target) < Math.abs(exposure) ? 2 : 1;
		exposure += Math.clamp(this.target - exposure, -rate, rate);
		if (Math.abs(exposure) < DANGER) dangerSeconds = 0;
		else if (++dangerSeconds >= 5) { dangerSeconds = 0; return true; }
		return false;
	}
}
