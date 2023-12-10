package org.example;

import io.moquette.broker.Server;
import io.moquette.broker.config.MemoryConfig;
import java.io.IOException;
import java.util.Properties;

public class EmbeddedMqttBroker {

    public static void main(String[] args) {
        Server mqttBroker = new Server();
        Properties configProps = new Properties();

        configProps.setProperty("port", "1883");
        configProps.setProperty("host", "0.0.0.0");
        // Aquí puedes configurar más propiedades según tus necesidades

        MemoryConfig memoryConfig = new MemoryConfig(configProps);

        try {
            mqttBroker.startServer(memoryConfig);
            System.out.println("Broker MQTT iniciado en el puerto 1883");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
