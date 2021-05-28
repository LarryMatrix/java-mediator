package tz.co.matrixhub.mediator;

import akka.actor.ActorSelection;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import org.openhim.mediator.engine.MediatorConfig;
import org.openhim.mediator.engine.messages.MediatorHTTPRequest;
import org.openhim.mediator.engine.messages.MediatorHTTPResponse;

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

            HashMap<String, String> header = new HashMap<>();
            header.put("Content-Type", "application/json");

            String  uri = "";
            String username = "";
            String password = "";

            String convertedMessage = new Gson().toJson(sourceMessage);
            if (!config.getDynamicConfig().isEmpty()) {
                JSONObject connectionProperties = new JSONObject(config.getDynamicConfig()).getJSONObject("hprs");
                uri = connectionProperties.getString("scheme")+
                        connectionProperties.getString("host")+":"+
                        connectionProperties.getString("port")+
                        connectionProperties.getString("path");

                username = connectionProperties.getString("username");
                password = connectionProperties.getString("password");

            } else {
                uri = config.getProperty("hprs.scheme")+
                        config.getProperty("hprs.host")+":"+
                        config.getProperty("hprs.port")+
                        config.getProperty("hprs.path");

                username = config.getProperty("hprs.username");
                password = config.getProperty("hprs.password");
            }


            String credentials = username + ":" + password;
            byte[] encodedAuth = Base64.encodeBase64(credentials.getBytes(StandardCharsets.ISO_8859_1));
            String authHeader = "Basic " + new String(encodedAuth);
            header.put("Authorization", authHeader);

            MediatorHTTPRequest mediatorHTTPRequest = new MediatorHTTPRequest(
                    ((MediatorHTTPRequest) msg).getRequestHandler(),
                    getSelf(),
                    "Sending data to from Mediator to HPRS",
                    "POST",
                    uri,
                    convertedMessage,
                    header,
                    null
            );

            ActorSelection actorSelection = getContext().actorSelection(config.userPathFor("http-connector"));
            actorSelection.tell(mediatorHTTPRequest, getSelf());
        } else if (msg instanceof MediatorHTTPResponse) {
            ((MediatorHTTPResponse) msg)
                    .getOriginalRequest()
                    .getRequestHandler()
                    .tell(((MediatorHTTPResponse) msg).toFinishRequest(), getSelf());
        } else {
            unhandled(msg);
        }
    }
}
