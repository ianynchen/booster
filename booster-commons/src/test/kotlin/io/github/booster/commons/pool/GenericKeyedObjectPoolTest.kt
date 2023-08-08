package io.github.booster.commons.pool

import arrow.core.Option
import arrow.core.getOrElse
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.CoreMatchers.sameInstance
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Test

internal class GenericKeyedObjectPoolTest {

    @Suppress("SwallowedException")
    private val pool = GenericKeyedObjectPool(
        object: KeyedPoolObjectFactory<String, Int> {
            override fun create(key: String) = try {
                key.toInt()
            } catch (e: Exception) {
                null
            }
        }
    )

    @Test
    fun shouldCreate() {
        MatcherAssert.assertThat(pool.get("2"), notNullValue())
        val value: Option<Int> = pool.tryGet("2")
        MatcherAssert.assertThat(value, notNullValue())
        MatcherAssert.assertThat(value.isDefined(), equalTo(true))
    }

    @Test
    fun shouldNotCreate() {
        MatcherAssert.assertThat(pool.get("abc"), nullValue())
        val value: Option<Int> = pool.tryGet("abc")
        MatcherAssert.assertThat(value, notNullValue())
        MatcherAssert.assertThat(value.isDefined(), equalTo(false))
    }

    @Test
    fun shouldReturnSameInstance() {
        val original: Int? = pool["2"]
        val value: Option<Int> = pool.tryGet("2")
        MatcherAssert.assertThat(value, notNullValue())
        MatcherAssert.assertThat(value.isDefined(), equalTo(true))
        MatcherAssert.assertThat(pool.get("2"), sameInstance<Any>(original))
        MatcherAssert.assertThat(value.getOrElse { null }, sameInstance(original))
    }
}
