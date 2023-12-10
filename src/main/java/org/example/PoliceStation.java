package org.example;

import org.eclipse.paho.client.mqttv3.*;
import redis.clients.jedis.Jedis;

import java.util.Set;

public class PoliceStation {
    // Configuración del cliente MQTT
    private final String mqttBrokerUrl = "tcp://localhost:1883";
    private final String mqttClientId = "PoliceStation";
    private final String mqttTopic = "trafficFines"; // Tema MQTT para enviar las multas

    // Cliente MQTT y Redis
    private MqttClient mqttClient;
    private Jedis redisClient;

    // Límite de velocidad para cálculo de multas
    private final int speedLimit = 80;

    public PoliceStation() throws MqttException {
        // Conexión con el broker MQTT
        mqttClient = new MqttClient(mqttBrokerUrl, mqttClientId);
        mqttClient.connect();

        // Conexión con Redis
        redisClient = new Jedis("localhost");

        // Procesar infracciones en un hilo separado
        new Thread(this::procesarInfracciones).start();

        // Mostrar estadísticas en un hilo separado
        new Thread(this::mostrarEstadisticas).start();
    }

    // Método para procesar infracciones de velocidad
    private void procesarInfracciones() {
        while (true) {
            // Obtener todas las claves de infracciones de Redis
            Set<String> infracciones = redisClient.keys("EXCESO:*");
            for (String clave : infracciones) {
                // Extraer la matrícula y la velocidad de la clave
                String matricula = clave.split(":")[2];
                int velocidad = Integer.parseInt(redisClient.get(clave));

                // Calcular el importe de la multa en función de la velocidad
                int importe = calcularImporteMulta(velocidad);

                // Enviar la multa vía MQTT
                enviarMulta(matricula, importe);

                // Eliminar la clave de Redis y añadir a los vehículos denunciados
                redisClient.del(clave);
                redisClient.sadd("VEHICULOSDENUNCIADOS", matricula);
            }
            try {
                Thread.sleep(1000); // Pausa entre cada ciclo de procesamiento
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // Método para calcular el importe de la multa
    private int calcularImporteMulta(int velocidad) {
        int exceso = velocidad - speedLimit;
        double porcentajeExceso = (double) exceso / speedLimit;

        // Determinar el importe de la multa según el porcentaje de exceso
        if (porcentajeExceso > 0.30) {
            return 500;
        } else if (porcentajeExceso > 0.20) {
            return 200;
        } else if (porcentajeExceso > 0.10) {
            return 100;
        }
        return 0; // No hay multa si no hay exceso
    }

    // Método para enviar la multa a través de MQTT
    private void enviarMulta(String matricula, int importe) {
        String mensaje = "Multa para " + matricula + ": " + importe + " €";
        try {
            mqttClient.publish(mqttTopic, new MqttMessage(mensaje.getBytes()));
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // Método para mostrar estadísticas de vehículos y multas
    private void mostrarEstadisticas() {
        while (true) {
            long totalVehiculos = redisClient.scard("VEHICULOS");
            long totalMultados = redisClient.scard("VEHICULOSDENUNCIADOS");

            double porcentajeMultados = (double) totalMultados / totalVehiculos * 100;
            System.out.println("Total vehículos: " + totalVehiculos + ", Porcentaje multados: " + porcentajeMultados + "%");

            try {
                Thread.sleep(1000); // Pausa entre cada actualización de estadísticas
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


}

