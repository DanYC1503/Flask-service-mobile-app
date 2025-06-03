# UPS GLAM 2.0

**Universidad Politécnica Salesiana**  
**Sede Cuenca**  
**Carrera de Computación**  
**Materia: Computación Paralela**  

## Autores
- Pardo Cambizaca Samuel Alejandro  
- Salazar Carrión Jairo Alexander  
- Yanza Cáceres Daniel Brian  

**Cuenca – Ecuador**  
**2025**

---

## 📄 Informe Técnico del Proyecto: UPS GLAM

### 1. Descripción General

**UPS GLAM** es una aplicación móvil de red social que permite a los usuarios aplicar filtros de imagen avanzados utilizando procesamiento acelerado por GPU.

El sistema está compuesto por:

- Una aplicación Android (`UI_Filter_APP`)
- Un backend basado en microservicios desarrollados con Flask

**Funcionalidades principales:**

- Autenticación de usuarios  
- Gestión de publicaciones  
- Procesamiento de imágenes  

---

### 2. Arquitectura del Sistema

El proyecto adopta una arquitectura moderna basada en **microservicios**, con separación clara entre cliente, pasarela de API, lógica de negocio y servicios externos.

#### Componentes principales

- **Aplicación móvil (Android):** Interfaz principal con Material Design.
- **API Gateway:** Encargado de enrutar solicitudes del cliente a los microservicios.
- **Servicios Backend:**  
  - Autenticación  
  - Gestión de usuarios  
  - Publicaciones  
  - Filtros de imagen  
- **Integraciones Externas:**  
  - Firebase (autenticación y almacenamiento)  
  - Servicios GPU (procesamiento de imágenes)

---

### 3. Aplicación Móvil: `UI_Filter_APP`

#### Funcionalidades

- Interfaz con **Material Design** y navegación inferior.
- Autenticación con **Firebase Auth SDK**.
- Consumo de API con **Retrofit** e inyección automática de token.

#### Clases Clave

| Componente         | Clase/Archivo         | Objetivo |
|--------------------|------------------------|----------|
| Interfaz Principal | `MainActivity.java`    | Navegación y carga de imágenes |
| Cliente HTTP       | `RetrofitClient.java`  | Comunicación con API y manejo de tokens |
| Interfaz API       | `UserServiceAPI.java`  | Definición de endpoints REST |
| Autenticación      | Firebase Auth SDK      | Gestión de usuarios y sesiones |

#### Vistas de la aplicación

- Registro / Login  
- Inicio / Publicar  
- Perfil  

*Nota: Puedes incluir capturas de pantalla correspondientes a cada sección.*

---

### 4. Servicios de Backend

Desarrollados con **Flask**, los microservicios incluyen:

- **Servicio de Autenticación:** Verificación de tokens y login.
- **Servicio de Usuarios:** CRUD de perfiles.
- **Servicio de Publicaciones:** Manejo de imágenes y descripciones.
- **Servicio de Filtros:** Procesamiento de imágenes con aceleración GPU.

#### Despliegue
- Contenerización con **Docker** y orquestación con **docker-compose**

---

### 5. Flujo de Trabajo Principal

#### Autenticación y gestión de usuarios

1. Los usuarios se autentican con Firebase.  
2. Retrofit envía automáticamente el token en cada solicitud.

#### Carga y procesamiento de imágenes

1. El usuario sube una imagen desde la app.  
2. La imagen es procesada con filtros por GPU y publicada.

---

### 6. Stack Tecnológico e Infraestructura

| Capa                  | Tecnología                     | Configuración                         |
|-----------------------|--------------------------------|----------------------------------------|
| Móvil                 | SDK de Android                 | Java/Kotlin + Material Design          |
| Backend               | Flask                          | Python + contenedores NVIDIA           |
| Procesamiento Imagen  | Flask + GPU                    | Aceleración por hardware               |
| Autenticación         | Firebase Auth                  | Tokens JWT                             |
| Base de datos         | Firestore                      | NoSQL orientado a documentos           |
| Almacenamiento        | Google Cloud Storage           | Imágenes procesadas con URL firmadas  |
| Orquestación          | Docker Compose                 | Red bridge personalizada (upsnetred)  |

---

### 7. Arquitectura de Despliegue

El sistema está desplegado en contenedores con soporte de GPU para maximizar el rendimiento en el procesamiento de imágenes.

#### Integraciones

- **Firebase:** Autenticación y almacenamiento NoSQL.
- **Google Cloud Storage:** Almacenamiento de archivos binarios.
- **API Gateway:** Ruteo y autenticación centralizada.
- **Servicios Flask (GPU):** Procesamiento intensivo de imágenes.

---

### 8. Despliegue y Operaciones

- Todos los servicios se encuentran **containerizados**.  
- Se orquestan con **docker-compose**.  
- Variables de entorno sensibles se manejan con un archivo `.env`.

### 9. Conclusión

UPS GLAM representa una red social innovadora que combina tecnologías modernas para brindar procesamiento de imágenes avanzado desde una app móvil.
El uso de Firebase para la autenticación y servicios GPU para el procesamiento hace que la aplicación sea eficiente, segura y escalable.
