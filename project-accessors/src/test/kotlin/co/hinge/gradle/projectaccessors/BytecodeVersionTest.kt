package co.hinge.gradle.projectaccessors

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isNotNull
import assertk.assertions.prop
import org.junit.jupiter.api.Test
import kotlin.reflect.full.findAnnotation

class BytecodeVersionTest {
    /**
     * This test is only relevant as long as we use Language Version 1.9 in the plugin.
     */
    @Test
    fun `ensures that the bytecode version is correct`() {
        assertThat(ProjectAccessorsPlugin::class.java.getAnnotation(Metadata::class.java))
            .isNotNull()
            .prop(Metadata::metadataVersion)
            .containsExactly(1, 9, 0)
    }
}