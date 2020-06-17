package org.elasticsoftware.elasticactors.tracing;

import org.elasticsoftware.elasticactors.ActorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Optional;
import java.util.ServiceLoader;

public abstract class MessagingContextManager {

    protected final static Logger logger = LoggerFactory.getLogger(MessagingContextManager.class);

    /*
     * Headers for external usage and MDC keys
     */

    // Tracing headers
    public static final String SPAN_ID_HEADER = "X-B3-SpanId";
    public static final String TRACE_ID_HEADER = "X-B3-TraceId";
    public static final String PARENT_SPAN_ID_HEADER = "X-B3-ParentSpanId";

    // Receiving-side headers
    public static final String MESSAGE_TYPE_KEY = "messageType";
    public static final String SENDER_KEY = "sender";
    public static final String RECEIVER_KEY = "receiver";
    public static final String RECEIVER_TYPE_KEY = "receiverType";
    public static final String RECEIVER_METHOD_KEY = "receiverMethod";

    // Originating-side headers
    public static final String CREATOR_KEY = "creator";
    public static final String CREATOR_TYPE_KEY = "creatorType";
    public static final String CREATOR_METHOD_KEY = "creatorMethod";
    public static final String SCHEDULED_KEY = "scheduled";

    @Nonnull
    public static MessagingContextManager getManager() {
        return MessagingContextManagerHolder.INSTANCE;
    }

    public interface MessagingScope extends AutoCloseable {

        @Nullable
        TraceContext getTraceContext();

        boolean isClosed();

        @Override
        void close();
    }

    @Nullable
    public abstract TraceContext currentTraceContext();

    @Nullable
    public abstract MessageHandlingContext currentMessageHandlingContext();

    @Nullable
    public abstract CreationContext currentCreationContext();

    @Nullable
    public abstract CreationContext creationContextFromScope();

    @Nullable
    public abstract Method currentMethodContext();

    @Nonnull
    public abstract MessagingScope enter(
            @Nullable ActorContext context,
            @Nullable TracedMessage message);

    @Nonnull
    public abstract MessagingScope enter(
            @Nullable TraceContext traceContext,
            @Nullable CreationContext creationContext);

    @Nonnull
    public abstract MessagingScope enter(@Nonnull Method context);

    /*
     * Initialization-on-deman holder pattern (lazy-loaded singleton)
     * See: https://en.wikipedia.org/wiki/Initialization-on-demand_holder_idiom
     */
    private static final class MessagingContextManagerHolder {

        private static final MessagingContextManager INSTANCE = loadService();

        private static MessagingContextManager loadService() {
            try {
                return Optional.of(ServiceLoader.load(MessagingContextManager.class))
                        .map(ServiceLoader::iterator)
                        .filter(Iterator::hasNext)
                        .map(Iterator::next)
                        .orElseGet(() -> {
                            logger.warn(
                                    "No implementations of MessagingContextManager were found. "
                                            + "Falling back to no-op.");
                            return new NoopMessagingContextManager();
                        });
            } catch (Exception e) {
                logger.error(
                        "Exception thrown while loading MessagingContextManager implementation. "
                                + "Falling back to no-op.", e);
                return new NoopMessagingContextManager();
            }
        }
    }

    /**
     * NO-OP implementation for when an implementation of the managet cannot be found
     */
    private final static class NoopMessagingContextManager extends MessagingContextManager {

        private final static class NoopMessagingScope implements MessagingScope {

            @Nullable
            @Override
            public TraceContext getTraceContext() {
                return null;
            }

            @Override
            public boolean isClosed() {
                return false;
            }

            @Override
            public void close() {

            }
        }

        private final static MessagingScope noopMessagingScope = new NoopMessagingScope();

        @Nullable
        @Override
        public TraceContext currentTraceContext() {
            return null;
        }

        @Nullable
        @Override
        public MessageHandlingContext currentMessageHandlingContext() {
            return null;
        }

        @Nullable
        @Override
        public CreationContext currentCreationContext() {
            return null;
        }

        @Nullable
        @Override
        public CreationContext creationContextFromScope() {
            return null;
        }

        @Nullable
        @Override
        public Method currentMethodContext() {
            return null;
        }

        @Nonnull
        @Override
        public MessagingScope enter(
                @Nullable ActorContext context, @Nullable TracedMessage message) {
            return noopMessagingScope;
        }

        @Nonnull
        @Override
        public MessagingScope enter(
                @Nullable TraceContext traceContext, @Nullable CreationContext creationContext) {
            return noopMessagingScope;
        }

        @Nonnull
        @Override
        public MessagingScope enter(@Nonnull Method context) {
            return noopMessagingScope;
        }
    }

}