# LegendaryEnchantments SMP - prototipo 1.0.0

Para Paper 1.21.1 y Java 21. No sustituye UnboundEnchants.

## Compilar
Con Gradle 8.8+ instalado: `gradle build`. El JAR queda en `build/libs/`.

## Instalar
Subir JAR a `plugins/`, iniciar servidor, editar `plugins/LegendaryEnchantments/config.yml` y reiniciar.

## Comandos
- `/legendary list`
- `/legendary givebook infinity`
- `/legendary givebook riptide NombreJugador`
- `/legendary status`

## Alcance real de esta primera versión
- Controla el número de libros **emitidos por este plugin**, guardando contadores en config.yml.
- No evita duplicación externa por otros plugins, comandos o editores NBT.
- Aplica libros especiales mediante yunque (una marca especial por objeto).
- Tótem con libro Infinidad recupera el tótem después de activarse, con cooldown.
- Aplica un borde de 3000 bloques al mundo normal.
- Los otros efectos propuestos aún NO están implementados.
- No está probado en ejecución con Paper ni con Bedrock/Geyser; respaldar mundo antes de usar.
- Mantener `keepInventory=false` para que los objetos caigan al morir.
