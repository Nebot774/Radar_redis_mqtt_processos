package org.example;

import org.eclipse.paho.client.mqttv3.*;
import redis.clients.jedis.Jedis;

import java.util.Arrays;

public class Radar {
    private final String mqttBrokerUrl = "tcp://localhost:1883"; // URL del broker MQTT
    private final String mqttClientId = "RadarDetector";// ID del cliente MQTT para este radar
    private final String mqttTopic = "vehicleData";// Tema MQTT donde se publican los datos de los vehículos

    private final int limiteVelocidad = 80;// Límite de velocidad establecido
    private MqttClient mqttClient;// Cliente MQTT
    private Jedis redisClient;// Cliente Redis

    public Radar() throws MqttException {
        // Inicializamos y configuramos el cliente MQTT
        mqttClient = new MqttClient(mqttBrokerUrl, mqttClientId);
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                procesarDatosVehiculo(new String(message.getPayload()));
            }

            @Override
            public void connectionLost(Throwable cause) {
                // Informar sobre la pérdida de conexión
                System.out.println("Conexión perdida con el broker MQTT: " + cause.getMessage());

                // Intentar reconectar
                while (!mqttClient.isConnected()) {
                    try {
                        System.out.println("Intentando reconectar al broker MQTT...");
                        mqttClient.reconnect();
                        Thread.sleep(5000); // Esperar un tiempo antes de intentar de nuevo
                    } catch (MqttException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }


                System.out.println("Reconexión exitosa al broker MQTT.");
                try {
                    mqttClient.subscribe(mqttTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                try {
                    // Confirmar que el mensaje ha sido entregado
                    System.out.println("Entrega de mensaje confirmada al tema: " + Arrays.toString(token.getTopics()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });

        // Conectar al broker MQTT y suscribirse al tema de los datos de vehículos
        mqttClient.connect();
        mqttClient.subscribe(mqttTopic);

        // Conectar al servidor Redis
        redisClient = new Jedis("localhost", 6379); // 6379 es el puerto por defecto de Redis

    }

    private void procesarDatosVehiculo(String datos) {
        // Separar la matrícula y la velocidad del mensaje recibido
        String[] partes = datos.split(" - ");
        String matricula = partes[0];
        int velocidad = Integer.parseInt(partes[1].split(" ")[0]);

        if (velocidad > limiteVelocidad) {
            // Si la velocidad supera el límite, registra una infracción en Redis
            String clave = "EXCESO:80:" + matricula;
            redisClient.set(clave, String.valueOf(velocidad));
        } else {
            // Si no supera el límite, añade al conjunto para estadísticas
            redisClient.sadd("VEHICULOS", matricula);
        }
    }


}
