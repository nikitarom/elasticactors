/*
 *   Copyright 2013 - 2019 The Original Authors
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.elasticsoftware.elasticactors.base.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsoftware.elasticactors.serialization.MessageToStringSerializer;

public final class JacksonMessageToStringSerializer<T> implements MessageToStringSerializer<T> {

    private final ObjectMapper objectMapper;

    public JacksonMessageToStringSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String serialize(T message) throws Exception {
        return objectMapper.writeValueAsString(message);
    }
}