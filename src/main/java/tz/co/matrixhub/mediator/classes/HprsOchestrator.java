package tz.co.matrixhub.mediator.classes;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.openhim.mediator.engine.MediatorConfig;
import org.openhim.mediator.engine.messages.MediatorHTTPRequest;

import java.util.HashMap;

public class HprsOchestrator extends UntypedActor {

    LoggingAdapter log = Logging.getLogger(getContext().system(), this);


    private final MediatorConfig config;

    public HprsOchestrator(MediatorConfig config) {
        this.config = config;
    }

    @Override
    public void onReceive(Object o) throws Exception {
        if (o instanceof MediatorHTTPRequest) {
            HashMap<String, String> hashMap = new HashMap<>();

            hashMap.put("key", "value");
            log.info(hashMap.toString());
        }
    }


}
