/*
 *    Copyright (c) 2014-2017 Neil Ellis
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package dollar.uri.mapdb;

import dollar.api.DollarStatic;
import dollar.api.Pipeable;
import dollar.api.Value;
import dollar.api.types.DollarFactory;
import dollar.api.types.ErrorType;
import dollar.api.uri.URI;
import dollar.internal.mapdb.BTreeMap;
import dollar.internal.mapdb.DB;
import dollar.internal.mapdb.MapModificationListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import static dollar.api.DollarStatic.$;

public class MapDBMapURI extends AbstractMapDBURI implements MapModificationListener<Value, Value> {

    @NotNull
    private static final ConcurrentHashMap<String, MapListener<Value, Value, Value>>
            subscribers =
            new ConcurrentHashMap<>();
    @NotNull
    private final BTreeMap<Value, Value> bTreeMap;

    public MapDBMapURI(@NotNull String scheme, @NotNull URI uri) {
        super(uri, scheme);
        bTreeMap = tx.treeMap(getHost(), new VarSerializer(), new VarSerializer()).modificationListener(this).createOrOpen();
    }

    @NotNull
    @Override
    public Value all() {
        HashMap<Value, Value> result = new HashMap<>(bTreeMap);
        return DollarFactory.fromValue(result);
    }

    @NotNull
    @Override
    public Value drain() {
        HashMap<Value, Value> result = new HashMap<>(bTreeMap);
        bTreeMap.clear();
        tx.commit();
        return DollarFactory.fromValue(result);

    }

    @Override
    public Value get(@NotNull Value key) {
        return bTreeMap.get(key.$fixDeep());
    }

    @NotNull
    @Override
    public Value read(boolean blocking, boolean mutating) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value remove(@NotNull Value v) {
        return bTreeMap.remove(v.$fixDeep());

    }

    @NotNull
    @Override
    public Value removeValue(@NotNull Value v) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value set(@NotNull Value key, @NotNull Value value) {
        if (!value.isVoid()) {
            return bTreeMap.put(key, value.$fixDeep());
        } else {
            return DollarStatic.$void();
        }
    }

    @Override
    public int size() {
        return bTreeMap.size();
    }

    @Override
    public void subscribe(@NotNull Pipeable consumer, @NotNull String id) throws IOException {

        subscribers.put(id, (var, oldValue, newValue) -> {
            try {
                consumer.pipe(DollarStatic.$(var, newValue));
            } catch (Exception e) {
                DollarFactory.failure(ErrorType.EXCEPTION, e, false);
            }
        });


    }

    @Override
    public void unsubscribe(@NotNull String subId) {
        subscribers.remove(subId);
    }

    @Override
    public Value write(@NotNull Value value, boolean blocking, boolean mutating) {
        if (value.pair()) {
            return set($(value.$pairKey()), value.$pairValue());
        } else {
            throw new UnsupportedOperationException("Can only write pairs to a map");
        }
    }

    @NotNull
    private BTreeMap<Value, Value> getTreeMap(@NotNull DB d) {
        return bTreeMap;
    }

    @Override
    public void modify(@NotNull Value key, @Nullable Value oldValue, @Nullable Value newValue, boolean triggered) {
        for (MapListener<Value, Value, Value> listener : subscribers.values()) {
            listener.apply(key, oldValue, newValue);
        }
    }
}
