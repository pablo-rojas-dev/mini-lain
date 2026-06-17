![Mini Lain banner](./doc/assets/mini-lain-banner.gif)

# Mini Lain

<table>
<tr>
<td width="60%" valign="top">

Mini Lain es un pequeño juego inspirado en la protagonista del anime [*Serial experiments Lain*](https://www.imdb.com/title/tt0500092/). La dinámica simula un diálogo con Lain en el que tras conocer tu nombre te reta a que adivines el número que está pensando entre 5 opciones que van del 1 al 5.

Si fallas entrará en bucle hasta que adivines el número, como si quedaras atrapado en ["The Wired"](https://lain.fandom.com/wiki/The_Wired), así hasta que adivines el número y decidas si quedarte en el bucle o salir.

</td>
<td width="40%" align="center">

<img src="./doc/assets/mini-lain-v1.gif" alt="Mini Lain demo" height="300">

</td>
</tr>
</table>

## Algoritmo simplificado
A continuación se describe de forma simplificado el flujo de la aplicación, el [algoritmo completo](./doc/algoritmo.md) es en `doc/`.

```mermaid
flowchart TD

A([Inicio]) --> Intro[Mostrar animación inicial de Lain]
Intro --> Nombre[Lain pregunta el nombre del usuario]
Nombre --> Verificar{¿Usuario existente o se llama Lain?}

Verificar -->|Sí| Menu[Mostrar menú: Jugar o Leer secreto]
Verificar -->|No| Registro[Registrar usuario nuevo]
Registro --> Identidad[Secuencia de identidad con Lain]
Identidad --> PreguntaFinal{¿Quiere volver a ser su nombre?}

PreguntaFinal -->|No| Derrota
PreguntaFinal -->|Sí| Reto

Menu -->|Jugar| Reto[Adivinar número del 1 al 5]
Menu -->|Leer secreto| LeerSecreto[Validar contraseña y mostrar secreto]

Reto --> Validar{¿Número correcto?}

Validar -->|No| Derrota[Escena de escape / Game Over]
Validar -->|Sí| Victoria[Victoria del usuario]

Victoria --> Nuevo{¿Usuario nuevo quiere guardar secreto?}
Nuevo -->|Sí| Guardar[Guardar secreto con contraseña]
Nuevo -->|No| OpcionesFinales

Guardar --> OpcionesFinales[Opciones: Salir u Otra vez]
LeerSecreto --> OpcionesFinales
Derrota --> Reintento[Opción: Nuevo intento]

Reintento --> Reto
OpcionesFinales -->|Otra vez| Reto
OpcionesFinales -->|Salir| Z([Fin])
```
