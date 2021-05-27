package tz.co.matrixhub.mediator;

import akka.actor.ActorSelection;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.openhim.mediator.engine.MediatorConfig;
import org.openhim.mediator.engine.messages.FinishRequest;
import org.openhim.mediator.engine.messages.MediatorHTTPRequest;
import tz.co.matrixhub.mediator.classes.SourceMessage;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;


public class DefaultOrchestrator extends UntypedActor {
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private final MediatorConfig config;

    public DefaultOrchestrator(MediatorConfig config) {
        this.config = config;
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        if (msg instanceof MediatorHTTPRequest) {

            String body = ((MediatorHTTPRequest) msg).getBody();

            SourceMessage sourceMessage = new Gson().fromJson(
                    body,
                    SourceMessage.class
            );

            String convertedMessage = new Gson().toJson(sourceMessage);

            JSONObject connectionProperties = new JSONObject(config.getDynamicConfig()).getJSONObject("hprs");
            String uri = connectionProperties.getString("scheme")+
                    connectionProperties.getString("host")+":"+
                    connectionProperties.getString("port")+
                    connectionProperties.getString("path");

            HashMap<String, String> header = new HashMap<>();
            header.put("Content-Type", "application/json");

            String username = connectionProperties.getString("username");
            String password = connectionProperties.getString("password");

            String credentials = username + ":" + password;
            byte[] encodedAuth = Base64.encodeBase64(credentials.getBytes(StandardCharsets.ISO_8859_1));
            String authHeader = "Basic " + new String(encodedAuth);
            header.put("Authorization", authHeader);

            MediatorHTTPRequest mediatorHTTPRequest = new MediatorHTTPRequest(
                    ((MediatorHTTPRequest) msg).getRequestHandler(),
                    getSender(),
                    "Sending data to from Mediator to HPRS",
                    "POST",
                    uri,
                    convertedMessage,
                    header,
                    null
            );

            ActorSelection actorSelection = getContext().actorSelection(config.userPathFor("http-connector"));
            actorSelection.tell(mediatorHTTPRequest, getSelf());

            FinishRequest finishRequest = new FinishRequest(
                    convertedMessage,
                    "application/json",
                    HttpStatus.SC_OK
            );
            ((MediatorHTTPRequest) msg).getRequestHandler().tell(finishRequest, getSender());
        } else {
            unhandled(msg);
        }
    }
}
