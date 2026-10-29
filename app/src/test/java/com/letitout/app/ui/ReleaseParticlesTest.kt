package com.letitout.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseParticlesTest {
    @Test fun particlesAreRepeatableForTheSameSeed() {
        assertEquals(releaseParticles(seed = 3), releaseParticles(seed = 3))
    }

    @Test fun particlesAreHiddenAtTheStartAndEnd() {
        releaseParticles().forEach { particle ->
            assertNull(particle.frameAt(0f))
            assertNull(particle.frameAt(1f))
        }
    }

    @Test fun visibleParticlesStayOnTheCanvas() {
        val frames = releaseParticles().flatMap { particle -> (1..19).mapNotNull { particle.frameAt(it / 20f) } }
        assertTrue("Some particles should be visible mid-animation", frames.isNotEmpty())
        frames.forEach { frame ->
            assertTrue(frame.x in 0f..1f && frame.y in 0f..1f)
            assertTrue(frame.alpha in 0f..1f && frame.radiusDp > 0f)
        }
    }
}
