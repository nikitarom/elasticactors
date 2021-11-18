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

package org.elasticsoftware.elasticactors.runtime;

import com.google.common.collect.ImmutableMap;
import org.elasticsoftware.elasticactors.serialization.Message;
import org.elasticsoftware.elasticactors.serialization.SerializationFramework;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

/**
 * Find all classes annotated with {@link org.elasticsoftware.elasticactors.serialization.Message} and
 * register them with the {@link org.elasticsoftware.elasticactors.serialization.SerializationFramework#register(Class)}
 *
 * @author Joost van de Wijgerd
 */
public final class MessagesScanner {

    private final static Logger logger = LoggerFactory.getLogger(MessagesScanner.class);

    private final ApplicationContext applicationContext;
    private final ImmutableMap<Class<?>, SerializationFramework> serializationFrameworks;

    public MessagesScanner(
        ApplicationContext applicationContext,
        List<SerializationFramework> serializationFrameworks)
    {
        this.applicationContext = applicationContext;
        this.serializationFrameworks = serializationFrameworks.stream()
            .collect(ImmutableMap.toImmutableMap(SerializationFramework::getClass, identity()));
    }

    @PostConstruct
    public void init() {
        logger.info("Scanning @Message-annotated classes");
        String[] basePackages = ScannerHelper.findBasePackagesOnClasspath(applicationContext.getClassLoader());
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();

        logger.debug("Scanning the following base packages: {}", (Object) basePackages);

        for (String basePackage : basePackages) {
            configurationBuilder.addUrls(ClasspathHelper.forPackage(basePackage));
        }

        Reflections reflections = new Reflections(configurationBuilder);

        Set<Class<?>> messageClasses = reflections.getTypesAnnotatedWith(Message.class);

        logger.info("Found {} classes annotated with @Message", messageClasses.size());
        if (logger.isDebugEnabled()) {
            logger.debug(
                "Found the following classes annotated with @Message: {}",
                messageClasses.stream().map(Class::getName).collect(Collectors.toList())
            );
        }

        for (Class<?> messageClass : messageClasses) {
            Message messageAnnotation = messageClass.getAnnotation(Message.class);
            Class<?> frameworkClass = messageAnnotation.serializationFramework();
            SerializationFramework serializationFramework =
                serializationFrameworks.get(frameworkClass);
            if (serializationFramework == null) {
                throw new IllegalStateException(String.format(
                    "Serialization framework instance not found for class '%s'",
                    frameworkClass.getTypeName()
                ));
            }
            logger.debug(
                "Registering message of type [{}] on [{}]",
                messageClass.getTypeName(),
                frameworkClass.getTypeName()
            );
            serializationFramework.register(messageClass);
        }
    }
}
