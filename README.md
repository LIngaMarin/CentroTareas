# CentroTareas

**Laboratorio 14 · Hilos en Android** · Programación en Móviles · Tecsup
**Alumno:** Lucas Inga Marín · **Docente:** Juan León S.

## Observación del Paso 1

Al pulsar **Tarea larga (MAL)**, `tareaLargaMala()` ejecuta `Thread.sleep(4000)` en el **hilo principal**.

- **Qué pasa con la pantalla:** se congela durante 4 segundos. El botón **Tócame** no suma aunque lo toques, nada se redibuja y la app no responde. Al terminar, aparece *Terminó* de golpe.
- **Por qué no se ve *Trabajando...*:** el mensaje sí cambia a *Trabajando...*, pero el hilo principal es el que dibuja la pantalla, y se queda dormido antes de poder redibujarla. Cuando despierta, el mensaje ya cambió a *Terminó*, así que *Trabajando...* nunca llega a pintarse.
- **Qué mostraría Android con una espera de 10 segundos:** un cuadro **ANR** (*Application Not Responding*): *"CentroTareas isn't responding"*, con las opciones **Close app** y **Wait**. Android lo muestra cuando la app no atiende un toque en unos 5 segundos. En la prueba apareció incluso con 4 segundos, porque se tocó **Tócame** varias veces mientras la pantalla estaba congelada.
