package io.github.booster.commons.pool;

import arrow.core.Option;
import arrow.core.OptionKt;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;

class NamedObjectPoolTest {

    private NamedObjectPool<Integer> pool = new NamedObjectPool<>() {
        @Override
        protected Integer createObject(String name) {
            try {
                return Integer.parseInt(name);
            } catch (Exception e) {
                return null;
            }
        }
    };

    @Test
    void shouldCreate() {
        assertThat(pool.get("2"), notNullValue());

        Option<Integer> value = pool.getOption("2");
        assertThat(value, notNullValue());
        assertThat(value.isDefined(), equalTo(true));
    }

    @Test
    void shouldNotCreate() {
        assertThat(pool.get("abc"), nullValue());

        Option<Integer> value = pool.getOption("abc");
        assertThat(value, notNullValue());
        assertThat(value.isDefined(), equalTo(false));
    }

    @Test
    void shouldReturnSameInstance() {
        Integer original = pool.get("2");

        Option<Integer> value = pool.getOption("2");
        assertThat(value, notNullValue());
        assertThat(value.isDefined(), equalTo(true));
        assertThat(pool.get("2"), sameInstance(original));
        assertThat(OptionKt.getOrElse(value, () -> null), sameInstance(original));
    }
}
