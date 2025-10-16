# Köva

> **Estado del proyecto:** En fase inicial de diseño y definición de prototipo.  
> *(El nombre final del proyecto está aún por definir.)*

---

### Equipo 2.2  
**Categorías de la App:** Seguridad · Hogar & Estilo de vida · Herramientas · Utilidades · Vehículos · Viajes  

---

## Descripción general

El **Proyecto X** es una solución inteligente diseñada para mejorar la **seguridad y el control de vehículos**, especialmente aquellos de entre 5 y 10 años que aún funcionan correctamente pero carecen de tecnología moderna.

Su objetivo es ofrecer **una capa adicional de inteligencia y control** instalable en cualquier vehículo, sin necesidad de sustituirlo por uno nuevo.  
Desde una aplicación móvil, el usuario puede **monitorizar, proteger y localizar su vehículo en tiempo real**.

---

## Problema que resuelve

El proyecto aborda diversas situaciones cotidianas y preocupaciones frecuentes entre los propietarios de vehículos:

- Olvido de si se ha cerrado el coche (tras salir de un restaurante, reunión o centro comercial).  
- Duda sobre la ubicación exacta del vehículo (en parkings o zonas desconocidas).  
- Sensación de inseguridad al dejar el coche en zonas poco iluminadas o transitadas.  
- Falta de aviso ante daños leves o arañazos.  
- Limitaciones tecnológicas en coches de 5–10 años que no cuentan con apertura remota, cámaras o sensores modernos.  

---

## Propuesta de valor

- Control total desde el móvil: apertura/cierre, localización y señales visuales/sonoras.  
- Seguridad y tranquilidad gracias a la supervisión constante del vehículo.  
- Modernización de vehículos antiguos, añadiendo funcionalidades de coches actuales.  
- Instalación sencilla y adaptable a cualquier modelo sin necesidad de cambiar de coche.  

---

## Público objetivo

El Proyecto X está dirigido a:

- Propietarios de coches de 5–10 años que buscan más seguridad sin cambiar de vehículo.  
- Personas que viven en ciudades grandes y aparcan en la calle o parkings públicos.  
- Familias con varios conductores o usuarios del mismo coche.  
- Personas mayores que puedan olvidar si cerraron el coche o dónde lo aparcaron.  
- Usuarios preocupados por la seguridad que deseen una solución accesible y moderna.  
- Empresas con flotas de coches, motos o furgonetas (transporte, logística, delivery…).  
- Viajeros frecuentes que dejan su coche estacionado durante largos periodos.  

---

## Interactividad bidireccional

### Desde la App → Prototipo
1. Abrir o cerrar puertas remotamente.  
2. Activar o desactivar el modo vigilancia.  
3. Hacer que el coche emita señales (pitido/luces) para localizarlo.  
4. Controlar cámaras y visualizar video en vivo.  

### Desde el Prototipo → App
1. Notificaciones ante impactos, movimiento o vibraciones.  
2. Envío de imágenes o video al detectarse un evento.  
3. Transmisión de ubicación GPS en tiempo real.  
4. Confirmación de estados (puertas bloqueadas/desbloqueadas, alarma activa, etc.).  

---

## Idea inicial del prototipo

**Plataforma:**  
- ESP32 o Raspberry Pi.  

**Sensores principales:**  
- Módulo KI031 – Sensor de impacto.  
- Servomotor SG90 – Apertura y cierre de puertas.  
- Sensor magnético – Detección de cierre activado.  
- Sensor GPS NEO-6M – Ubicación GPS.  
- Speaker SFM-20B – Emisión de sonido en señales o detección de impacto.  
- LEDs – Indicadores visuales.  
- Cámaras (2 o 3) – Modo vigilancia, capturas, video en vivo.  

**Extras:**  
- Módulo de cámara.  
- Módulo de conectividad WiFi y/o 4G.  

---

## App móvil

La aplicación incluirá:

- Pantalla de autenticación.  
- Vista principal con el estado del coche (puertas, alarma, ubicación).  
- Botones de acción (abrir/cerrar, activar vigilancia, localizar, ver cámara).  
- Recepción de notificaciones y alertas en tiempo real.  

---

## Tecnologías sugeridas

- **Hardware:** ESP32 / Raspberry Pi, sensores GPS, cámaras, servomotores, módulos de comunicación.  
- **Software:** App móvil (Android/iOS) desarrollada con Flutter o React Native.  
- **Comunicación:** WiFi o red 4G para sincronización con la app.  
- **Prototipado:** Arduino IDE / Python según el microcontrolador.  

---

## Equipo de desarrollo

**Equipo 2.2**  
*(Integrantes por definir o añadir aquí los nombres y roles del equipo)*  

---

## Licencia

Este proyecto es de carácter académico y está desarrollado con fines educativos en el marco de la **Universidad Politécnica de Valencia (UPV)**.

---

## Notas del repositorio

K FALTAN PONER IMÁGENES, ICONOS Y DEMÁS EN LA RAMA MAIN.  
CUANDO ESTÉN, HAREMOS UN **GIT PUSH DE LA MAIN A LA RAMA DE CADA PERSONA.**
