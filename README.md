## 📊 Diagrama de estructura del proyecto

```mermaid
graph TD

    %% Carpeta raíz del módulo
    A[Mensajeria] --> B[src]
    A --> C[target]
    A --> D[BD.txt]
    A --> E[pom.xml]
    A --> F[dependency-reduced-pom.xml]
    A --> G[server.properties.txt]
    A --> H[DOC_MENSAJERIA.pdf]

    %% Subcarpetas de src/main/java/com/tarea
    B --> B1[main]
    B1 --> B2[java]
    B2 --> B3[com]
    B3 --> B4[tarea]

    %% Archivos Java
    B4 --> J1[AESUtil.java]
    B4 --> J2[ChatServer.java]
    B4 --> J3[ClientApp.java]
    B4 --> J4[DatabaseManager.java]
    B4 --> J5[Launcher.java]

    %% Target (no detallado porque es generado)
    C --> C1[(archivos compilados)]
