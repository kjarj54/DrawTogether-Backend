package com.drawtogether;

import com.drawtogether.websocket.DrawWebSocketServer;

public class Main {
    public static void main(String[] args) {
        int port = 8080; // Default port

        // Primero intentar obtener el puerto de la variable de entorno PORT (para Render)
        String portEnv = System.getenv("PORT");
        if (portEnv != null) {
            try {
                port = Integer.parseInt(portEnv);
            } catch (NumberFormatException exception) {
                System.err.println("Variable de entorno PORT invalida, usando el puerto por defecto: " + port);
            }
        }
        // Si no hay variable de entorno, intentar usar argumentos de línea de comandos
        else if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException exception) {
                System.err.println("Puerto invalido, usando el puerto por defecto: " + port);
            }
        }

        DrawWebSocketServer server = new DrawWebSocketServer(port);
        server.start();

        System.out.println("Servidor DrawTogether iniciado en el puerto: " + port);
        System.out.println("Presiona Ctrl+C para detener el servidor.");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Cerrando servidor DrawTogether...");

            try {
                server.stop();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
    }
}