# Instalar Plantitas en un iPhone desde Linux (sin Mac, sin cuenta de pago)

Cada push a `master` (o `workflow_dispatch` manual) hace que GitHub Actions compile un
`plantitas.ipa` **sin firmar** — ver `.github/workflows/ios.yml`. Este documento cubre la
otra mitad: cómo llevar ese `.ipa` al iPhone desde un PC Linux, usando el Apple ID gratuito.

La app instalada así **caduca a los 7 días** (límite de Apple para firmas de desarrollador
gratuitas, no de esta herramienta). Refrescarla es volver a ejecutar el mismo comando del
paso 4 — no hace falta repetir nada de los pasos 1-3.

## Por qué este camino

[`AltServer-Linux`](https://github.com/jaakkopalvaila/AltServer-Linux) firma y sideloada un
`.ipa` en un iPhone conectado por USB directamente desde la línea de comandos, sin pasar por
AltStore/SideStore instalados en el propio teléfono. Eso simplifica mucho el refresco semanal:
es un solo comando repetible desde el PC, no depende de que el móvil esté en la misma WiFi con
una app de fondo corriendo.

Uso el fork de `jaakkopalvaila` en vez del original de `NyaMisty` porque a día de hoy
(septiembre de 2026) corrige dos problemas que bloquean el original: el login con Apple ID
devuelve HTTP 503 contra iOS 26.4+/27, y la firma se rechaza con `AMFI:
cmsBlobVerifyWithAgilityHash failed`. Si el fork deja de mantenerse, revisar primero si
`NyaMisty/AltServer-Linux` ha incorporado el fix antes de buscar otra alternativa.

## 1. Conectar el iPhone

```sh
sudo pacman -S usbmuxd libimobiledevice
sudo systemctl enable --now usbmuxd
```

Conecta el iPhone por USB y acepta "Confiar en este ordenador" en el móvil. Comprueba que se
ve:

```sh
idevice_id -l          # debe imprimir el UDID del iPhone
idevicepair pair        # solo la primera vez
```

Guarda ese UDID — hace falta en el paso 4.

Si `idevice_id -l` no muestra nada, sigue las instrucciones de
[netmuxd](https://github.com/jkcoxson/netmuxd) (`cargo build --release`) como reemplazo de
`usbmuxd`: es el camino que documenta el propio AltServer-Linux para USB cuando el usbmuxd del
sistema no coopera, y es obligatorio si más adelante se quiere refrescar por WiFi en vez de por
cable. En ese caso, antes del paso 4 hay que exportar
`USBMUXD_SOCKET_ADDRESS=127.0.0.1:27015` apuntando al netmuxd que arranques.

## 2. Servidor Anisette propio

AltServer necesita datos Anisette (la telemetría de dispositivo que Apple exige para iniciar
sesión) de un servidor compatible. Levantar uno propio con Docker evita depender de uno público
ajeno:

```sh
docker run -d --restart always --name anisette-v3 -p 6969:6969 \
  --volume anisette-v3_data:/home/Alcoholic/.config/anisette-v3/lib/ \
  dadoum/anisette-v3-server
```

Déjalo corriendo (el volumen persiste el estado entre reinicios).

## 3. Descargar AltServer-Linux

```sh
curl -LO https://github.com/jaakkopalvaila/AltServer-Linux/releases/latest/download/AltServer-x86_64
chmod +x AltServer-x86_64
```

## 4. Instalar el .ipa

Descarga `plantitas.ipa` del artifact de la última ejecución de
`.github/workflows/ios.yml` en GitHub Actions (pestaña *Actions* del repo → última run → sección
*Artifacts*). Luego:

```sh
ALTSERVER_ANISETTE_SERVER=http://127.0.0.1:6969 \
./AltServer-x86_64 -u <UDID> -a <tu-apple-id> -p <tu-contraseña> plantitas.ipa
```

(Añade `USBMUXD_SOCKET_ADDRESS=127.0.0.1:27015` delante si has tenido que instalar netmuxd en
el paso 1.) La contraseña se pasa en texto plano por variable de entorno/argumento — usa un
Apple ID dedicado a esto si te incomoda escribir el principal en una terminal, y considera un
gestor de contraseñas de shell (`pass`, o simplemente no dejarlo en el historial:
`HISTCONTROL=ignorespace` + un espacio inicial) en vez de pegarlo a pelo.

Si Apple pide verificación en dos pasos, AltServer lo solicita por consola en ese mismo
comando — introduce el código que llega al dispositivo de confianza.

La app aparece en la pantalla de inicio del iPhone. La primera vez, además, hay que ir a
**Ajustes → General → VPN y gestión de dispositivos** y confiar explícitamente en el perfil de
desarrollador asociado a ese Apple ID.

## Refrescar cada 7 días

Repite solo el comando del paso 4 (con un `.ipa` nuevo si ha habido cambios, o el mismo si no).
El iPhone debe seguir emparejado y accesible por USB; los pasos 1-3 no hace falta repetirlos.
