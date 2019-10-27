### Tile a big image using Libvips

An excellent tool for image processing, and also for image tiling, is [Libvips](https://github.com/libvips/libvips/).

#### Libvips installation

Libvips is supported on Linux and Windows. It can be used from the command line (the most easy way).

* For Linux

  Libvips is packaged in most distributions. For example, on Ubuntu it can be installed with :
  ```
  sudo apt install libvips
  ```
* For Windows

  Libvips binaries can be downloaded from [here](https://github.com/libvips/libvips/releases).
  The executables are inside the vips-dev-w64-all-x.x.x.zip

#### Using Libvips

If we want to tile the image named big_image.png :

* On Linux

  ```
  vips dzsave big_image.png output --layout=google --tile-size=256 --suffix .jpg --vips-progress
  ```

* On Windows

  For convenience, you can execute `vips.exe` in the image's parent directory. You can right-click
  in the folder while holding the left Shift keystroke, then "Execute a command here".

  ```
  vips.exe dzsave big_image.png output --layout=google --tile-size=256 --suffix .jpg --vips-progress
  ```
In that command, you can specify :
* the size of tiles, with the option `--tile-size`
* the quality and format of the tiles. For example, using `.jpg[Q=90]` will increase the quality but
also the size in MB of map.

At the end of the tiling process, a folder "output" is created. You can rename it to whatever name
you like. That folder contains several subfolders that are number-named (0, 1, 2, etc). Each subfolder
corresponds to a level of the map.