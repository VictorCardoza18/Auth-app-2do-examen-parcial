# Firebase Authentication App

## Descripción General

Esta aplicación móvil desarrollada en Android Studio con Kotlin y Jetpack Compose implementa un sistema de autenticación completo utilizando Firebase. La aplicación permite el registro e inicio de sesión de usuarios, con dos tipos de roles diferenciados: usuarios normales y administradores. Además, cuenta con un sistema de notificaciones que permite a los administradores enviar mensajes personalizados a usuarios específicos o de manera global.

## Características Principales

- **Autenticación con Firebase**
  - Registro de usuarios
  - Inicio de sesión
  - Manejo de sesiones
  - Roles de usuario (administrador y usuario normal)
  - Contraseña maestra para creación de administradores

- **Sistema de Notificaciones**
  - Notificaciones personalizadas a usuarios específicos
  - Notificaciones globales para todos los usuarios
  - Categorización de notificaciones (INFO, WARNING, ERROR, SUCCESS)
  - Notificaciones push en tiempo real
  - Interfaz para gestionar notificaciones (marcar como leídas, eliminar)

- **Panel de Administración**
  - Vista de todos los usuarios registrados
  - Capacidad para enviar notificaciones desde la interfaz
  - Estadísticas básicas de usuarios

## Tecnologías Utilizadas

- **Lenguajes y Frameworks**
  - Kotlin
  - Jetpack Compose para la interfaz de usuario
  - Firebase Authentication
  - Firebase Realtime Database
  - Firebase Cloud Messaging (FCM)

- **Arquitectura**
  - Modelo-Vista-ViewModel (MVVM)
  - Clean Architecture
  - Repository Pattern
  - Dependency Injection

## Estructura del Proyecto

### Modelos de Datos

- **User**: Representa la información del usuario (uid, email, nombre, rol de administrador)
- **Notification**: Almacena datos de las notificaciones (id, userId, título, mensaje, timestamp, estado de lectura, tipo)

### Repositorios

- **AuthRepository**: Maneja operaciones de autenticación y acceso a datos de usuarios
- **NotificationRepository**: Gestiona operaciones relacionadas con notificaciones

### ViewModels

- **AuthViewModel**: Lógica de autenticación y gestión de usuarios
- **NotificationViewModel**: Lógica para manejar notificaciones

### Pantallas UI

- **LoginScreen**: Pantalla de inicio de sesión
- **RegisterScreen**: Pantalla de registro con opción para roles
- **UserHomeScreen**: Pantalla principal para usuarios normales
- **AdminHomeScreen**: Panel de control para administradores
- **NotificationsScreen**: Lista de notificaciones para ambos tipos de usuarios

## Requisitos del Sistema

- Android Studio Hedgehog o superior
- Kotlin 1.9.0 o superior
- Android SDK 30+ (Android 11.0+)
- Firebase project configurado con Authentication y Realtime Database
- Gradle 8.0+

## Configuración del Proyecto

1. Clone el repositorio
2. Abra el proyecto en Android Studio
3. Conecte su proyecto con Firebase:
   - Vaya a Tools > Firebase
   - Siga los pasos para conectar Authentication y Realtime Database
   - Descargue el archivo `google-services.json` y colóquelo en la carpeta `/app`
4. Sincronice el proyecto con los archivos Gradle
5. Configure las reglas de seguridad de Firebase Realtime Database

## Reglas de Seguridad para Firebase

```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read": "$uid === auth.uid || root.child('users').child(auth.uid).child('admin').val() === true",
        ".write": "$uid === auth.uid || root.child('users').child(auth.uid).child('admin').val() === true"
      }
    },
    "notifications": {
      ".read": "auth != null",
      ".write": "auth != null && root.child('users').child(auth.uid).child('admin').val() === true",
      "$notification_id": {
        ".read": "auth != null && (data.child('userId').val() === auth.uid || root.child('users').child(auth.uid).child('admin').val() === true)",
        ".write": "auth != null && (data.child('userId').val() === auth.uid || root.child('users').child(auth.uid).child('admin').val() === true)"
      }
    }
  }
}
```

## Flujo de la Aplicación

1. **Inicio**: El usuario entra a la pantalla de login
2. **Registro/Login**: El usuario puede crear una cuenta o iniciar sesión
   - En el registro puede seleccionar rol de administrador (requiere contraseña maestra)
3. **Pantalla Principal**:
   - Los usuarios normales ven su pantalla de inicio con información básica
   - Los administradores ven un panel de control con lista de usuarios y opciones de notificación
4. **Notificaciones**:
   - Los usuarios pueden ver sus notificaciones recibidas
   - Los administradores pueden enviar notificaciones a usuarios específicos o globales
   - Las notificaciones no leídas aparecen como notificaciones push

## Implementación de Notificaciones Push

La aplicación utiliza Firebase Cloud Messaging y servicios en segundo plano para mostrar notificaciones push cuando:
- Un usuario recibe una nueva notificación
- La notificación no ha sido leída
- El usuario destinatario tiene la sesión iniciada

## Personalización

### Contraseña Maestra de Administrador

Para cambiar la contraseña maestra utilizada para crear cuentas de administrador, modifique la constante `ADMIN_MASTER_PASSWORD` en la clase `AuthRepository`.

### Tema y Estilo

La aplicación utiliza Material 3 de Jetpack Compose. Los colores, tipografía y temas pueden modificarse en los archivos correspondientes dentro del paquete `ui.theme`.

## Consideraciones de Seguridad

- La contraseña maestra de administrador debe cambiarse antes del despliegue
- Se recomienda implementar políticas de contraseñas robustas
- Las reglas de Firebase Realtime Database deben configurarse adecuadamente para proteger los datos
- Se recomienda implementar cifrado adicional para datos sensibles

## Mejoras Futuras

- Implementación de autenticación con proveedores externos (Google, Facebook, etc.)
- Sistema de recuperación de contraseñas
- Panel de administración más completo con análisis y estadísticas
- Mejora en la gestión de permisos granulares
- Sincronización offline con persistencia de datos
- Implementación de pruebas unitarias y de integración

## Contribución

Las contribuciones son bienvenidas. Para contribuir:

1. Haga fork del repositorio
2. Cree una rama para su característica (`git checkout -b feature/amazing-feature`)
3. Haga commit de sus cambios (`git commit -m 'Add amazing feature'`)
4. Haga push a la rama (`git push origin feature/amazing-feature`)
5. Abra un Pull Request

## Licencia

Este proyecto está licenciado bajo [MIT License](LICENSE).

## Autor

Victor Cardoza - [VictorCardoza18@github](https://github.com/VictorCardoza18)

---

Desarrollado con ❤️ y Jetpack Compose.
