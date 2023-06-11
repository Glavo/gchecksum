/*
 * Copyright 2021 Glavo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.glavo.checksum.util;

import java.util.Objects;

public final class Pair<T1, T2> {
    public final T1 component1;
    public final T2 component2;

    public Pair(T1 component1, T2 component2) {
        this.component1 = component1;
        this.component2 = component2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Pair)) {
            return false;
        }
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(component1, pair.component1) && Objects.equals(component2, pair.component2);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(component1) * 31 + Objects.hashCode(component2);
    }

    @Override
    public String toString() {
        return "Pair[" + component1 + ", " + component2 + ']';
    }
}
