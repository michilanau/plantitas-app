# Instalar Plantitas en un iPhone desde Linux (sin Mac, sin cuenta de pago)

Cada push a `master` (o `workflow_dispatch` manual) hace que GitHub Actions compile un
`plantitas.ipa` **sin firmar** — ver `.github/workflows/ios.yml`. Este documento cubre la otra
mitad: llevar ese `.ipa` al iPhone desde un PC Linux con [iloader](https://github.com/nab138/iloader),
que lo firma con un Apple ID gratuito y lo instala por USB.

La app instalada así **caduca a los 7 días** (límite de Apple para firmas gratuitas). Refrescarla
es repetir los pasos 3-4. Para renovar sin cable, ver *Renovación sin PC* al final.

## Antes de empezar

- **Usa un Apple ID desechable**, creado solo para esto (gratis, sin tarjeta, con verificación en
  dos pasos). La contraseña se introduce en una herramienta de terceros; con una cuenta sin datos,
  lo peor que puede pasar es perder esa cuenta. Cambia su contraseña al terminar.
- iPhone con **iOS 16 o superior** (es el mínimo de la app).
- Un cable USB **de datos** (los de solo carga no sirven).

## 1. Preparar el PC (una sola vez)

```sh
sudo pacman -S usbmuxd libimobiledevice
sudo systemctl enable --now usbmuxd
```

Conecta el iPhone, desbloquéalo y pulsa **Confiar** en el aviso. Comprueba que se ve:

```sh
idevice_id -l          # debe imprimir el UDID del iPhone
```

Si no imprime nada, mira con `lsusb | grep -i apple`: si tampoco sale, el problema es el
cable, el puerto o que el teléfono esté bloqueado.

## 2. Descargar iloader (una sola vez)

Descarga solo desde los sitios oficiales: [GitHub Releases](https://github.com/nab138/iloader/releases)
o [iloader.app](https://iloader.app).

```sh
mkdir -p ~/iloader && cd ~/iloader
curl -LO https://github.com/nab138/iloader/releases/download/v2.3.3/iloader-linux-amd64.AppImage
echo "a9e841259cfec05065dad31428dd1b27b6c3321f10d310cb0315082836b83b7e  iloader-linux-amd64.AppImage" | sha256sum -c -
chmod +x iloader-linux-amd64.AppImage
```

El segundo comando debe responder `La suma coincide`. (El hash es el que publica GitHub para la
release v2.3.3; si actualizas de versión, usa el hash de esa release.)

Para abrirlo sin instalar `fuse2`:

```sh
APPIMAGE_EXTRACT_AND_RUN=1 ~/iloader/iloader-linux-amd64.AppImage
```

(Alternativa: `sudo pacman -S fuse2` y luego basta con `./iloader-linux-amd64.AppImage`.)

## 3. Descargar el .ipa

Desde la raíz del repo, baja el de la última compilación correcta:

```sh
gh run download "$(gh run list -w ios.yml -s success -L1 --json databaseId -q '.[0].databaseId')" \
  -n plantitas-ipa -D ~/iloader
```

Los artifacts de GitHub caducan a los 30 días. Si ya no hay, lanza el workflow a mano
(pestaña *Actions* → *iOS unsigned build* → *Run workflow*) y espera a que termine (~15 min).

## 4. Instalar con iloader

1. Con el iPhone conectado y desbloqueado, abre iloader.
2. Inicia sesión con el Apple ID desechable. Si pide verificación en dos pasos, introduce el
   código que llega al iPhone.
3. Elige la acción para importar un IPA y selecciona `~/iloader/plantitas.ipa`.
4. Espera a que termine. La app aparece en la pantalla de inicio.

Si algo falla, iloader sugiere soluciones para errores comunes. Los registros están en
`~/.local/share/me.nabdev.iloader/logs/` (o en el botón *View Logs* de la app, con nivel *Debug*).

## 5. Confiar en el desarrollador (solo la primera vez)

En el iPhone: **Ajustes → General → VPN y gestión de dispositivos** → toca el Apple ID
desechable → **Confiar**.

Si al abrir la app iOS pide activar el **Modo desarrollador**: **Ajustes → Privacidad y
seguridad → Modo desarrollador**, actívalo y reinicia el teléfono.

## Renovación cada 7 días

Repite los pasos 3 y 4 (con el iPhone conectado). Los recordatorios de cuidados funcionan con la
cuenta gratuita: son notificaciones locales, no *push*.

## Renovación sin PC (opcional)

iloader también puede instalar [SideStore](https://github.com/SideStore/SideStore), que renueva la
firma desde el propio iPhone sin cable. Requiere configuración adicional en el móvil; ver la
documentación de SideStore.

## Límites de la cuenta gratuita

Máximo 3 apps instaladas a la vez y unos 10 identificadores de app nuevos por semana. Sin
notificaciones *push* (Plantitas no las usa).
