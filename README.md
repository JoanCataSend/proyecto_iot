#  KöVa

> Proyecto académico desarrollado en la Universitat Politècnica de València (UPV)

*(Las imágenes, iconos y recursos gráficos se añadirán en la rama `main`. Una vez incorporados, se realizará un `git push` desde `main` a la rama individual de cada integrante del equipo.)*

---

##  Descripción general

**KöVa** es una solución tecnológica orientada a mejorar la **seguridad, el control y la supervisión de vehículos**, especialmente aquellos con una antigüedad aproximada de entre 5 y 10 años que no disponen de sistemas inteligentes de serie.

El proyecto consiste en la instalación de un **prototipo físico** en el vehículo, conectado a una **aplicación móvil**, que permite al usuario monitorizar el estado del coche, gestionar funciones básicas y recibir alertas en tiempo real. De esta forma, se añade una **capa adicional de inteligencia** al vehículo sin necesidad de sustituirlo por uno nuevo.

---

##  Objetivo del proyecto

El objetivo principal de KöVa es **modernizar vehículos existentes** mediante una solución accesible, modular y adaptable, proporcionando funcionalidades similares a las de los vehículos actuales, tales como control remoto, localización GPS, vigilancia y notificaciones de seguridad.

---

##  Problema que aborda

Los propietarios de vehículos sin tecnología avanzada suelen enfrentarse a limitaciones como:
- Falta de confirmación sobre el estado de cierre del vehículo.
- Dificultad para localizar el coche en parkings o zonas desconocidas.
- Ausencia de avisos ante impactos, movimientos o intentos de acceso no autorizados.
- Sensación de inseguridad al estacionar en determinadas zonas.

---

##  Propuesta de valor

- Control remoto del vehículo desde la aplicación móvil.
- Supervisión continua y aumento de la seguridad mediante sensores y alertas.
- Modernización de vehículos sin necesidad de sustitución.
- Instalación modular y adaptable utilizando recursos universitarios.

---

##  Interacción del sistema

### App → Prototipo
- Apertura y cierre remoto.
- Activación del modo vigilancia.
- Emisión de señales de localización.
- Visualización de cámaras en tiempo real.

### Prototipo → App
- Notificaciones de impacto y movimiento.
- Envío de imágenes y vídeo.
- Ubicación GPS en tiempo real.
- Confirmación del estado del vehículo.

---

##  Prototipo y hardware

El prototipo se ha desarrollado utilizando **recursos proporcionados por la universidad**.

- ESP32 y Raspberry Pi
- Sensor de impacto KY-031
- Servomotores SG90
- Sensor magnético
- Módulo GPS NEO-6M
- Cámaras y módulos de comunicación WiFi / 4G

---

##  Aplicación móvil

La aplicación permite autenticación de usuarios, visualización del estado del vehículo, control remoto y recepción de notificaciones en tiempo real, con una interfaz clara y accesible.

---

##  Tecnologías utilizadas

- Hardware: ESP32, Raspberry Pi
- Software: App móvil multiplataforma
- Comunicación: WiFi / 4G
- Prototipado: Arduino IDE y Python

---

##  Equipo de desarrollo

**Equipo 2.2**  
Universitat Politècnica de València
Álvaro Ballester Grau
Aarón Blasco Blay
Enrique Buerbaum del Río
Matilde Calleja García
Joan Catalá Sendra
Julia Valén de Oliveira

---

##  Estado del proyecto

El proyecto se encuentra **prácticamente finalizado**, con el prototipo funcional y las principales características implementadas utilizando los recursos disponibles en la universidad.

---

##  Licencia

Proyecto académico con fines educativos desarrollado en la **Universitat Politècnica de València (UPV)**.
