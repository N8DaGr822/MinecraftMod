# Package the generated source art at power-of-two sizes for Minecraft mipmaps.
# No semantic image edits: nearest-neighbor scaling preserves the authored pixels.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
foreach ($asset in @(@('cuisine_atlas.png', 'textures/block/cuisine_atlas.png', 512), @('project-icon.png', 'icon.png', 256))) {
    $sourcePath = Join-Path $projectRoot ('art/source/' + $asset[0])
    $destinationPath = Join-Path $projectRoot ('src/main/resources/assets/darkspawn/' + $asset[1])
    $original = [Drawing.Image]::FromFile($sourcePath)
    $bitmap = [Drawing.Bitmap]::new([int]$asset[2], [int]$asset[2])
    $graphics = [Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
        $graphics.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::Half
        $graphics.DrawImage($original, [Drawing.Rectangle]::new(0, 0, [int]$asset[2], [int]$asset[2]))
        $bitmap.Save($destinationPath, [Drawing.Imaging.ImageFormat]::Png)
    } finally { $graphics.Dispose(); $bitmap.Dispose(); $original.Dispose() }
    Write-Output ($asset[0] + ': ' + $asset[2] + ' pixels')
}
