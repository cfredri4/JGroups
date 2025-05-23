package org.jgroups.protocols;

import org.jgroups.Message;
import org.jgroups.annotations.Experimental;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Bela Ban
 * @since  4.0
 */
@Experimental
public class SenderSendsBundler extends BaseBundler {
    protected final AtomicInteger num_senders=new AtomicInteger(0); // current senders adding msgs to the bundler

    public void send(Message msg) throws Exception {
        num_senders.incrementAndGet();
        int size=msg.size();

        lock.lock();
        try {
            if(count + size >= max_size)
                sendBundledMessages();

            addMessage(msg, size);

            // at this point, we haven't sent our message yet !
            if(num_senders.decrementAndGet() == 0) // no other sender threads present at this time
                sendBundledMessages();
            // else there are other sender threads waiting, so our message will be sent by a different thread
        }
        finally {
            lock.unlock();
        }
    }
}
