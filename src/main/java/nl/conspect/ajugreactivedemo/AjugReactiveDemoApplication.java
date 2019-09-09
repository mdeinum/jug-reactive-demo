package nl.conspect.ajugreactivedemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import reactor.core.publisher.Mono;

@SpringBootApplication
public class AjugReactiveDemoApplication {

    private static final Logger log = LoggerFactory.getLogger(AjugReactiveDemoApplication.class);

    public static void main(String[] args) throws Exception {

        Mono.just("Hello World!").subscribe(AjugReactiveDemoApplication::log);

    }

    private static void log(Object o) {
        log.info(String.valueOf(o));
    }

}
