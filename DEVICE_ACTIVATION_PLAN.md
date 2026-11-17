# Zorrillo TV — Activación por dispositivo

Estado: diseño. La APK todavía no está conectada.

1. Generar UUID persistente por instalación y clave privada en Android Keystore.
2. Registrar dispositivo mediante servicio HTTPS seguro, inicialmente pendiente.
3. Mostrar código de emparejamiento corto (no es una contraseña).
4. El administrador autoriza o bloquea desde el panel web.
5. El servicio verifica identidad y licencia vigente; la APK habilita o bloquea reproducción.
6. Consultar periódicamente para aplicar revocaciones.

Seguridad: mantener Firestore solo para UID administrador; nunca exponer claves privadas en la APK. Un UUID o código de seis dígitos por sí solo no demuestra identidad. El panel actual edita registros, pero no hay bloqueo real hasta implementar el backend y la APK. Firebase Hosting estático no ejecuta backend; Cloud Functions puede requerir plan Blaze. Mantener main intacta y probar en Fire TV antes de distribuir.
