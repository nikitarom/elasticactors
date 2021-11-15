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
import org.elasticsoftware.elasticactors.ActorState;
import org.elasticsoftware.elasticactors.ElasticActor;
import org.elasticsoftware.elasticactors.serialization.Deserializer;
import org.elasticsoftware.elasticactors.serialization.Message;
import org.elasticsoftware.elasticactors.serialization.MessageDeserializer;
import org.elasticsoftware.elasticactors.serialization.MessageSerializer;
import org.elasticsoftware.elasticactors.serialization.MessageToStringConverter;
import org.elasticsoftware.elasticactors.serialization.SerializationFramework;
import org.elasticsoftware.elasticactors.serialization.Serializer;
import org.springframework.core.env.Environment;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.elasticsoftware.elasticactors.serialization.MessageToStringConverter.DEFAULT_MAX_LENGTH;
import static org.elasticsoftware.elasticactors.serialization.MessageToStringConverter.LOGGING_MAXIMUM_LENGTH_PROPERTY;
import static org.elasticsoftware.elasticactors.serialization.MessageToStringConverter.LOGGING_USE_TO_STRING_PROPERTY;

/**
 * @author Joost van de Wijgerd
 */
@Named
public final class JacksonSerializationFramework implements SerializationFramework {

    private final ConcurrentMap<Class, JacksonMessageDeserializer> deserializers =
            new ConcurrentHashMap<>();
    private final JacksonMessageSerializer serializer;
    private final MessageToStringConverter toStringConverter;
    private final ObjectMapper objectMapper;
    private final JacksonActorStateSerializer actorStateSerializer;
    private final JacksonActorStateDeserializer actorStateDeserializer;

    @Inject
    public JacksonSerializationFramework(
            ObjectMapper objectMapper,
            Environment environment) {
        this.objectMapper = objectMapper;
        this.serializer = new JacksonMessageSerializer(objectMapper);
        this.toStringConverter = new JacksonMessageToStringConverter(
            objectMapper,
            environment.getProperty(
                LOGGING_MAXIMUM_LENGTH_PROPERTY,
                Integer.class,
                DEFAULT_MAX_LENGTH
            ),
            environment.getProperty(LOGGING_USE_TO_STRING_PROPERTY, Boolean.class, false)
        );
        this.actorStateSerializer = new JacksonActorStateSerializer(objectMapper);
        this.actorStateDeserializer = new JacksonActorStateDeserializer(objectMapper);
    }

    @Override
    public void register(Class<?> messageClass) {
        Message messageAnnotation;
        if ((messageAnnotation = messageClass.getAnnotation(Message.class)) != null
            && this.getClass().equals(messageAnnotation.serializationFramework()))
        {
            deserializers.computeIfAbsent(
                messageClass,
                c -> new JacksonMessageDeserializer(c, objectMapper)
            );
        }
    }

    @Override
    public <T> MessageSerializer<T> getSerializer(Class<T> messageClass) {
        return serializer;
    }

    @Override
    public MessageToStringConverter getToStringConverter() {
        return toStringConverter;
    }

    @Override
    public <T> MessageDeserializer<T> getDeserializer(Class<T> messageClass) {
        return deserializers.get(messageClass);
    }

    @Override
    public Serializer<ActorState, byte[]> getActorStateSerializer(Class<? extends ElasticActor> actorClass) {
        return actorStateSerializer;
    }

    @Override
    public Serializer<ActorState, byte[]> getActorStateSerializer(ActorState actorState) {
        return actorStateSerializer;
    }

    @Override
    public Deserializer<ByteBuffer, ActorState> getActorStateDeserializer(Class<? extends ElasticActor> actorClass) {
        return actorStateDeserializer;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
