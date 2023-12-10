package org.example;


public class Main {
    public static void main(String[] args) {
        try {
            // Iniciar CarSimulator
            CarSimulator carSimulator = new CarSimulator();
            System.out.println("CarSimulator iniciado.");

            // Iniciar Radar
            Radar radar = new Radar();
            System.out.println("Radar iniciado.");

            // Iniciar PoliceStation
            PoliceStation policeStation = new PoliceStation();
            System.out.println("PoliceStation iniciada.");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error al iniciar las aplicaciones.");
        }



    }
}