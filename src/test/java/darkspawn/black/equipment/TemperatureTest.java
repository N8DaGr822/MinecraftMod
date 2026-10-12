package darkspawn.black.equipment;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TemperatureTest {
	@Test void exposureWarnsBeforePenaltiesAndDamageInBothDirections() {
		for (int sign : new int[] { -1, 1 }) {
			var state = new TemperatureState();
			for (int second = 1; second <= 88; second++) assertFalse(state.step(sign * 170));
			assertEquals(sign * 88, state.exposure);
			assertTrue(state.step(sign * 170));
			for (int second = 0; second < 4; second++) assertFalse(state.step(sign * 170));
			assertTrue(state.step(sign * 170));
		}
	}

	@Test void shelterAndGearRecoverGraduallyAndOppositeClimatesDoNotResetExposure() {
		var state = new TemperatureState(); state.restore(-94, 30, 0);
		state.step(0); assertEquals(-92, state.exposure);
		state.step(100); assertEquals(-90, state.exposure);
		for (int second = 0; second < 45; second++) state.step(100);
		assertEquals(0, state.exposure);
		state.step(100); assertEquals(1, state.exposure);
		state.restore(Float.NaN, 500, 100); assertEquals(0, state.exposure); assertEquals(30, state.wetSeconds); assertEquals(4, state.dangerSeconds);
	}

	@Test void cavesModerateClimateButNetherShelterDoesNotEraseHeat() {
		float surface = Temperature.baseClimate(0, false, false, 200, true, 0, true, true);
		float shallow = Temperature.baseClimate(0, false, false, 200, false, 4, true, true);
		float deep = Temperature.baseClimate(0, false, false, 200, false, 12, true, true);
		assertTrue(surface < shallow && shallow < deep);
		assertTrue(Math.abs(deep) < TemperatureState.WARNING);
		assertEquals(140, Temperature.baseClimate(2, true, false, 40, false, 90, false, false));
		assertEquals(110, Temperature.baseClimate(2, false, false, 64, true, 0, false, false));
		assertEquals(45, Temperature.baseClimate(2, false, false, 64, true, 0, true, false));
		assertEquals(0, Temperature.baseClimate(.8F, false, false, 64, true, 0, false, false));
	}
}
