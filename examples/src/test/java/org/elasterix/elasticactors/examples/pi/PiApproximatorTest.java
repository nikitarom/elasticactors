/*
 * Copyright 2013 Joost van de Wijgerd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasterix.elasticactors.examples.pi;

import org.apache.log4j.BasicConfigurator;
import org.elasterix.elasticactors.ActorRef;
import org.elasterix.elasticactors.ActorState;
import org.elasterix.elasticactors.ActorSystem;
import org.elasterix.elasticactors.ActorSystems;
import org.elasterix.elasticactors.cluster.ActorRefFactory;
import org.elasterix.elasticactors.examples.pi.actors.Listener;
import org.elasterix.elasticactors.examples.pi.actors.Master;
import org.elasterix.elasticactors.examples.pi.messages.Calculate;
import org.elasterix.elasticactors.examples.pi.messages.PiApproximation;
import org.elasterix.elasticactors.examples.pi.messages.Result;
import org.elasterix.elasticactors.examples.pi.messages.Work;
import org.elasterix.elasticactors.serialization.Deserializer;
import org.elasterix.elasticactors.serialization.MessageDeserializer;
import org.elasterix.elasticactors.serialization.MessageSerializer;
import org.elasterix.elasticactors.serialization.Serializer;
import org.elasterix.elasticactors.test.TestActorSystem;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Joost van de Wijgerd
 */
public class PiApproximatorTest {

    @BeforeMethod
    public void setUp() {
        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure();
    }

    @Test
    public void testMessageSerializers() throws IOException {
        PiApproximator piApproximator = new PiApproximator("PiTest",1);

        // Calculate

        MessageSerializer<Calculate> calculateMessageSerializer = piApproximator.getSerializer(Calculate.class);
        assertNotNull(calculateMessageSerializer);
        ByteBuffer serializedForm = calculateMessageSerializer.serialize(new Calculate());
        assertNotNull(serializedForm);
        MessageDeserializer<Calculate> calculateMessageDeserializer = piApproximator.getDeserializer(Calculate.class);
        assertNotNull(calculateMessageDeserializer);
        Calculate calculate = calculateMessageDeserializer.deserialize(serializedForm);
        assertNotNull(calculate);

        // Work
        MessageSerializer<Work> workMessageSerializer = piApproximator.getSerializer(Work.class);
        assertNotNull(workMessageSerializer);
        serializedForm = workMessageSerializer.serialize(new Work(1,100));
        assertNotNull(serializedForm);
        MessageDeserializer<Work> workMessageDeserializer = piApproximator.getDeserializer(Work.class);
        assertNotNull(workMessageDeserializer);
        Work work = workMessageDeserializer.deserialize(serializedForm);
        assertNotNull(work);
        assertEquals(work.getStart(),1);
        assertEquals(work.getNrOfElements(),100);

        // REsult
        MessageSerializer<Result> resultMessageSerializer = piApproximator.getSerializer(Result.class);
        assertNotNull(workMessageSerializer);
        serializedForm = resultMessageSerializer.serialize(new Result(0.8376d));
        assertNotNull(serializedForm);
        MessageDeserializer<Result> resultMessageDeserializer = piApproximator.getDeserializer(Result.class);
        assertNotNull(workMessageDeserializer);
        Result result = resultMessageDeserializer.deserialize(serializedForm);
        assertNotNull(result);
        assertEquals(result.getValue(),0.8376d);

        // PiApproximation
        MessageSerializer<PiApproximation> piApproximationMessageSerializer = piApproximator.getSerializer(PiApproximation.class);
        assertNotNull(piApproximationMessageSerializer);
        serializedForm = piApproximationMessageSerializer.serialize(new PiApproximation(3.14827683d,19283827262l));
        MessageDeserializer<PiApproximation> piApproximationMessageDeserializer = piApproximator.getDeserializer(PiApproximation.class);
        assertNotNull(piApproximationMessageDeserializer);
        PiApproximation piApproximation = piApproximationMessageDeserializer.deserialize(serializedForm);
        assertNotNull(piApproximation);
        assertEquals(piApproximation.getPi(),3.14827683d);
        assertEquals(piApproximation.getDuration(),19283827262l);
    }

    @Test
    public void testStateSerialization() throws Exception {
        ActorRef listenerRef = mock(ActorRef.class);
        ActorRef masterRef = mock(ActorRef.class);
        ActorSystem actorSystem = mock(ActorSystem.class);
        ActorSystems parent = mock(ActorSystems.class);
        ActorRefFactory actorRefFactory = mock(ActorRefFactory.class);
        PiApproximator piApproximator = new PiApproximator("PiTest",1);

        ArgumentCaptor<ActorState> stateArgumentCaptor = ArgumentCaptor.forClass(ActorState.class);

        when(actorSystem.getParent()).thenReturn(parent);
        when(parent.getActorRefFactory()).thenReturn(actorRefFactory);
        when(actorSystem.actorOf("listener", Listener.class)).thenReturn(listenerRef);
        when(actorSystem.actorOf(eq("master"),eq(Master.class),stateArgumentCaptor.capture())).thenReturn(masterRef);

        when(listenerRef.toString()).thenReturn("listenerRef");
        when(masterRef.toString()).thenReturn("masterRef");
        when(actorRefFactory.create("listenerRef")).thenReturn(listenerRef);
        when(actorRefFactory.create("masterRef")).thenReturn(masterRef);

        piApproximator.initialize(actorSystem);
        piApproximator.create(actorSystem);

        Serializer<ActorState,byte[]> actorStateSerializer = piApproximator.getActorStateSerializer();
        assertNotNull(actorStateSerializer);

        byte[] serializedBytes = actorStateSerializer.serialize(stateArgumentCaptor.getValue());
        //System.out.println(new String(serializedBytes, Charsets.UTF_8));
        assertNotNull(serializedBytes);

        Deserializer<byte[],ActorState> actorStateDeserializer = piApproximator.getActorStateDeserializer();
        assertNotNull(actorStateDeserializer);

        ActorState actorState = actorStateDeserializer.deserialize(serializedBytes);
        assertNotNull(actorState);
        Master.MasterState masterState = actorState.getAsObject(Master.MasterState.class);
        assertNotNull(masterState);
        assertEquals(masterState.getListener(),listenerRef);
        assertEquals(masterState.getNrOfWorkers(),4);
        assertEquals(masterState.getNrOfMessages(),10000);
        assertEquals(masterState.getNrOfElements(),10000);

    }

    @Test
    public void testInContainer() throws Exception {
        ActorSystem piSystem = TestActorSystem.create(new PiApproximator("Pi",8));
        ActorRef actorRef = piSystem.actorFor("master");
        //actorRef.tell(new Calculate(),null);
        // wait
        Thread.sleep(30000);
    }
}