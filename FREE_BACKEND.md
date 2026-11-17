# Backend gratuito propuesto

- Mantener Firebase Spark para Hosting, Authentication y Firestore.
- Usar Cloudflare Workers Free como API HTTPS para registro, comprobación y revocación de dispositivos, sujeto a cuotas y condiciones vigentes.
- Para consultar Firestore desde el Worker, usar una cuenta de servicio con credenciales almacenadas como secretos del Worker (nunca en GitHub ni APK). Evaluar seguridad y cuotas antes de desplegar.
- Proteger endpoints con límites de peticiones, prueba de posesión de clave de Android Keystore, tokens de vida corta y controles contra suplantación.
- El administrador autenticado cambia estados en Firestore; el Worker los consulta y valida. Evitar que la APK lea Firestore directamente con privilegios de administrador.
- La TV debe consultar el estado periódicamente y bloquear la reproducción si no hay autorización vigente. Definir tolerancia a caídas de red.
- Antes de implementar: crear cuenta de Cloudflare, configurar Worker y secretos; comprobar integración con Firebase y pruebas en Fire TV.

Estado: arquitectura propuesta, NO desplegada ni integrada a la APK.
