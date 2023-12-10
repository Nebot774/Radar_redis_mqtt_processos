package org.example;


import org.eclipse.paho.client.mqttv3.*;

import java.util.Arrays;
import java.util.Random;

public class CarSimulator {
    // Configuración del cliente MQTT
    private final String brokerUrl = "tcp://localhost:1883"; // URL del broker MQTT
    private final String clientId = "CarSimulatorUniqueID";  // ID único para este cliente MQTT
    private final String publishTopic = "vehicleData";       // Tema para publicar datos de los vehículos
    private final String subscribeTopic = "trafficFines";    // Tema para suscribirse a las multas

    private MqttClient mqttClient;

    public CarSimulator() throws MqttException {
        mqttClient = new MqttClient(brokerUrl, clientId);

        // Establecemos un callback para manejar los mensajes entrantes y las desconexiones
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void messageArrived(String topic, MqttMessage message) {
                // Acción a realizar cuando se recibe una multa
                System.out.println("Multa recibida: " + new String(message.getPayload()));
            }

            @Override
            public void connectionLost(Throwable cause) {
                // Imprimimos la razón de la pérdida de conexión
                System.out.println("Conexión perdida con el broker MQTT: " + cause.getMessage());

                // Intentamos reconectarnos
                while (!mqttClient.isConnected()) {
                    try {
                        System.out.println("Intentando reconectar...");
                        mqttClient.reconnect();
                        Thread.sleep(5000); // Esperamos un tiempo antes de volver a intentar
                    } catch (MqttException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }


                System.out.println("Reconectado al broker MQTT.");
            }


            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // Como getTopics() no lanza MqttException, no necesitamos un bloque try-catch para eso
                String[] topics = token.getTopics();
                if (topics != null) {
                    System.out.println("Mensaje entregado con éxito a los temas: " + Arrays.toString(topics));
                } else {
                    System.out.println("Mensaje entregado, pero los temas no están disponibles");
                }
            }




        });

        // Conectamos al broker MQTT y nos suscribimos al tema de multas
        mqttClient.connect();
        mqttClient.subscribe(subscribeTopic);
    }

    private void velocidadVehiculo() throws MqttException {
        Random random = new Random();

        while (true) {
            int speed = 60 + random.nextInt(81);  // Velocidad entre 60 y 140
            String plate = generarMatricula(); // Generar una matrícula aleatoria
            String data = plate + " - " + speed + " km/h";

            // Publicar los datos del vehículo en el tema MQTT
            mqttClient.publish(publishTopic, new MqttMessage(data.getBytes()));

            try {
                Thread.sleep(1000); // Esperar 1 segundo antes de publicar el siguiente mensaje
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private String generarMatricula() {
        Random random = new Random();
        // Generar una combinación de 4 dígitos y 3 letras para la matrícula
        String numbers = String.format("%04d", random.nextInt(10000));
        String letters = "" + (char) (random.nextInt(26) + 'A') +
                (char) (random.nextInt(26) + 'A') +
                (char) (random.nextInt(26) + 'A');
        return numbers + letters;
    }

    public static void main(String[] args) throws MqttException {
        CarSimulator simulator = new CarSimulator();
        simulator.velocidadVehiculo(); // Comenzamos la publicación de datos de vehículos
    }
}

