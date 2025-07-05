# Despliegue en Render - DrawTogether Backend

Este proyecto está configurado para desplegarse en Render usando Docker.

## Archivos de Configuración

- `Dockerfile`: Configura el contenedor Docker con OpenJDK 21 y Maven
- `render.yaml`: Configuración específica para Render
- `.dockerignore`: Optimiza el build excluyendo archivos innecesarios

## Pasos para Desplegar en Render

### 1. Preparar el Repositorio
1. Sube tu código a GitHub (si no lo has hecho ya)
2. Asegúrate de que todos los archivos estén committeados

### 2. Conectar con Render
1. Ve a [render.com](https://render.com) y crea una cuenta
2. Conecta tu cuenta de GitHub
3. Haz clic en "New +" → "Web Service"
4. Selecciona tu repositorio del backend

### 3. Configurar el Servicio
Render debería detectar automáticamente el archivo `render.yaml`, pero si necesitas configurar manualmente:

- **Runtime**: Docker
- **Dockerfile Path**: `./Dockerfile`
- **Branch**: `main` (o la rama que uses)
- **Plan**: Free (para empezar)

### 4. Variables de Entorno (Opcional)
Si necesitas configurar variables adicionales:
- `JAVA_OPTS`: `-Xmx512m -Xms256m` (ya configurado en render.yaml)

### 5. Desplegar
1. Haz clic en "Create Web Service"
2. Render construirá y desplegará automáticamente tu aplicación
3. Una vez completado, obtendrás una URL pública para tu WebSocket server

## URL del WebSocket
Tu servidor WebSocket estará disponible en:
```
wss://tu-app-name.onrender.com
```

## Notas Importantes

- **Plan Gratuito**: Render's free tier tiene limitaciones:
  - El servicio se "duerme" después de 15 minutos de inactividad
  - 750 horas de tiempo de ejecución por mes
  - Recursos limitados de CPU y memoria

- **Puerto**: La aplicación está configurada para usar la variable de entorno `PORT` que Render proporciona automáticamente

- **Región**: Configurado para Oregon por defecto, puedes cambiar la región en `render.yaml`

## Troubleshooting

Si tienes problemas con el despliegue:

1. Verifica los logs en el dashboard de Render
2. Asegúrate de que el Dockerfile esté en la raíz del proyecto
3. Confirma que todas las dependencias estén en el `pom.xml`
4. Para el plan gratuito, la aplicación puede tardar en "despertar" después de inactividad

## Actualizar el Despliegue
Para actualizar tu aplicación:
1. Haz push de los cambios a tu repositorio
2. Render automáticamente detectará los cambios y redesplegarán
3. O puedes hacer un redeploy manual desde el dashboard
