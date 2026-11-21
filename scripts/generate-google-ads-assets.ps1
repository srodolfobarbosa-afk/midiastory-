$ErrorActionPreference = "Stop"
Add-Type -AssemblyName PresentationCore
Add-Type -AssemblyName PresentationFramework
Add-Type -AssemblyName WindowsBase

$outDir = "C:\Users\rodolfo\Downloads\google-ads-midiastore"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$baseHomePath = "C:\Users\rodolfo\midiastory-\docs\campaigns\store-screenshots\_base\home.png"

function Convert-HexToColor([string]$hex) {
    return [System.Windows.Media.ColorConverter]::ConvertFromString($hex)
}

function New-SolidBrush([string]$hex) {
    $b = New-Object System.Windows.Media.SolidColorBrush (Convert-HexToColor $hex)
    $b.Freeze()
    return $b
}

function Load-BitmapFrozen([string]$path) {
    $bytes = [System.IO.File]::ReadAllBytes($path)
    $ms = New-Object System.IO.MemoryStream (,$bytes)
    $bmp = New-Object System.Windows.Media.Imaging.BitmapImage
    $bmp.BeginInit()
    $bmp.CacheOption = [System.Windows.Media.Imaging.BitmapCacheOption]::OnLoad
    $bmp.StreamSource = $ms
    $bmp.EndInit()
    $bmp.Freeze()
    $ms.Dispose()
    return $bmp
}

function Save-ElementAsPng([System.Windows.FrameworkElement]$el, [int]$w, [int]$h, [string]$filePath) {
    $el.Width = $w
    $el.Height = $h
    $el.Measure((New-Object System.Windows.Size($w, $h)))
    $el.Arrange((New-Object System.Windows.Rect(0, 0, $w, $h)))
    $el.UpdateLayout()

    $rtb = New-Object System.Windows.Media.Imaging.RenderTargetBitmap($w, $h, 96, 96, [System.Windows.Media.PixelFormats]::Pbgra32)
    $rtb.Render($el)
    $enc = New-Object System.Windows.Media.Imaging.PngBitmapEncoder
    $enc.Frames.Add([System.Windows.Media.Imaging.BitmapFrame]::Create($rtb))
    $fs = [System.IO.File]::Open($filePath, [System.IO.FileMode]::Create, [System.IO.FileAccess]::Write)
    try {
        $enc.Save($fs)
    } finally {
        $fs.Dispose()
    }
    Write-Host "SAVED: $filePath ($w x $h)"
}

function New-MidiastoreLogoCanvas([double]$size, [bool]$rounded = $true) {
    $grid = New-Object System.Windows.Controls.Grid
    $grid.Width = $size
    $grid.Height = $size

    # Outer OLED background
    $bgGrad = New-Object System.Windows.Media.LinearGradientBrush
    $bgGrad.StartPoint = New-Object System.Windows.Point(0, 0)
    $bgGrad.EndPoint = New-Object System.Windows.Point(1, 1)
    $bgGrad.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#0B1324"), 0.0))
    $bgGrad.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#000000"), 0.55))
    $bgGrad.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#07182E"), 1.0))

    $cr = if ($rounded) { $size * 0.22 } else { 0 }
    $outerBorder = New-Object System.Windows.Controls.Border
    $outerBorder.CornerRadius = New-Object System.Windows.CornerRadius($cr)
    $outerBorder.Background = $bgGrad
    if ($rounded) {
        $outerBorder.BorderBrush = New-SolidBrush "#260A84FF"
        $outerBorder.BorderThickness = New-Object System.Windows.Thickness(($size * 0.012))
    }
    $grid.Children.Add($outerBorder) | Out-Null

    # Subtle blue radial aura
    $glow = New-Object System.Windows.Shapes.Ellipse
    $glow.Width = $size * 0.82
    $glow.Height = $size * 0.82
    $glowBrush = New-Object System.Windows.Media.RadialGradientBrush (Convert-HexToColor "#440A84FF"), (Convert-HexToColor "#000A84FF")
    $glow.Fill = $glowBrush
    $grid.Children.Add($glow) | Out-Null

    # Vector Canvas (108x108 coordinate space scaled to $size)
    $vb = New-Object System.Windows.Controls.Viewbox
    $vb.Width = $size * 0.78
    $vb.Height = $size * 0.78
    $cv = New-Object System.Windows.Controls.Canvas
    $cv.Width = 108
    $cv.Height = 108

    # Outer precision gauge ring (270 deg arc + cyan-blue gradient)
    $ringGrad = New-Object System.Windows.Media.LinearGradientBrush
    $ringGrad.StartPoint = New-Object System.Windows.Point(0, 0)
    $ringGrad.EndPoint = New-Object System.Windows.Point(1, 1)
    $ringGrad.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#38BDF8"), 0.0))
    $ringGrad.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#0A84FF"), 0.6))
    $ringGrad.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#30D158"), 1.0))

    $trackCircle = New-Object System.Windows.Shapes.Ellipse
    $trackCircle.Width = 76
    $trackCircle.Height = 76
    [System.Windows.Controls.Canvas]::SetLeft($trackCircle, 16)
    [System.Windows.Controls.Canvas]::SetTop($trackCircle, 16)
    $trackCircle.Stroke = New-SolidBrush "#1F2937"
    $trackCircle.StrokeThickness = 7.5
    $cv.Children.Add($trackCircle) | Out-Null

    $arcPath = New-Object System.Windows.Shapes.Path
    $arcPath.Data = [System.Windows.Media.Geometry]::Parse("M 27.1,27.1 A 38,38 0 1 1 22.5,75.2")
    $arcPath.Stroke = $ringGrad
    $arcPath.StrokeThickness = 8
    $arcPath.StrokeStartLineCap = [System.Windows.Media.PenLineCap]::Round
    $arcPath.StrokeEndLineCap = [System.Windows.Media.PenLineCap]::Round
    $cv.Children.Add($arcPath) | Out-Null

    # Stylized bold 'M' + check/spark inside the ring
    $mPath = New-Object System.Windows.Shapes.Path
    $mPath.Data = [System.Windows.Media.Geometry]::Parse("M 35,66 L 35,42 L 47.5,56 L 54,48.5 L 60.5,56 L 73,42 L 73,66")
    $mPath.Stroke = New-SolidBrush "#FFFFFF"
    $mPath.StrokeThickness = 6.8
    $mPath.StrokeStartLineCap = [System.Windows.Media.PenLineCap]::Round
    $mPath.StrokeEndLineCap = [System.Windows.Media.PenLineCap]::Round
    $mPath.StrokeLineJoin = [System.Windows.Media.PenLineJoin]::Round
    $cv.Children.Add($mPath) | Out-Null

    # Clean sparkle star top-right
    $starPath = New-Object System.Windows.Shapes.Path
    $starPath.Data = [System.Windows.Media.Geometry]::Parse("M 80,16 L 82.8,23.2 L 90,26 L 82.8,28.8 L 80,36 L 77.2,28.8 L 70,26 L 77.2,23.2 Z")
    $starPath.Fill = New-SolidBrush "#38BDF8"
    $cv.Children.Add($starPath) | Out-Null

    $vb.Child = $cv
    $grid.Children.Add($vb) | Out-Null
    return $grid
}

function New-PhoneMockup([System.Windows.Media.Imaging.BitmapImage]$bmp, [double]$phoneW, [double]$phoneH) {
    $phoneBorder = New-Object System.Windows.Controls.Border
    $phoneBorder.Width = $phoneW
    $phoneBorder.Height = $phoneH
    $phoneBorder.CornerRadius = New-Object System.Windows.CornerRadius(32)
    $phoneBorder.Background = New-SolidBrush "#000000"
    $phoneBorder.BorderBrush = New-SolidBrush "#334155"
    $phoneBorder.BorderThickness = New-Object System.Windows.Thickness(4)
    $phoneBorder.Padding = New-Object System.Windows.Thickness(8)

    $innerClip = New-Object System.Windows.Controls.Border
    $innerClip.CornerRadius = New-Object System.Windows.CornerRadius(24)
    $innerClip.ClipToBounds = $true
    $innerClip.Background = New-SolidBrush "#000000"

    $img = New-Object System.Windows.Controls.Image
    $img.Source = $bmp
    $img.Stretch = [System.Windows.Media.Stretch]::UniformToFill
    $img.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center
    $img.VerticalAlignment = [System.Windows.VerticalAlignment]::Top
    $innerClip.Child = $img
    $phoneBorder.Child = $innerClip
    return $phoneBorder
}

$homeBmp = Load-BitmapFrozen $baseHomePath
$font = New-Object System.Windows.Media.FontFamily "Segoe UI"

# 1. LOGO QUADRADO 512x512 (Google Ads Square Logo + Play Store 512x512 — full-bleed OLED)
$logo512 = New-MidiastoreLogoCanvas 512 $false
Save-ElementAsPng $logo512 512 512 (Join-Path $outDir "logo-quadrado-512x512.png")

# 2. BANNER HORIZONTAL 1200x628 (Google Ads Landscape 1.91:1)
$horizRoot = New-Object System.Windows.Controls.Grid
$hBg = New-Object System.Windows.Media.LinearGradientBrush
$hBg.StartPoint = New-Object System.Windows.Point(0, 0)
$hBg.EndPoint = New-Object System.Windows.Point(1, 1)
$hBg.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#050912"), 0.0))
$hBg.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#000000"), 0.5))
$hBg.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#071426"), 1.0))
$horizRoot.Background = $hBg

$hGlow = New-Object System.Windows.Shapes.Ellipse
$hGlow.Width = 760
$hGlow.Height = 620
$hGlow.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Right
$hGlow.VerticalAlignment = [System.Windows.VerticalAlignment]::Center
$hGlow.Margin = New-Object System.Windows.Thickness(0, 0, -80, 0)
$hGlow.Fill = New-Object System.Windows.Media.RadialGradientBrush (Convert-HexToColor "#3A0A84FF"), (Convert-HexToColor "#000A84FF")
$horizRoot.Children.Add($hGlow) | Out-Null

$hCols = New-Object System.Windows.Controls.Grid
$hCols.Margin = New-Object System.Windows.Thickness(68, 44, 56, 44)
$c0 = New-Object System.Windows.Controls.ColumnDefinition
$c0.Width = New-Object System.Windows.GridLength(1.35, [System.Windows.GridUnitType]::Star)
$c1 = New-Object System.Windows.Controls.ColumnDefinition
$c1.Width = New-Object System.Windows.GridLength(1.0, [System.Windows.GridUnitType]::Star)
$hCols.ColumnDefinitions.Add($c0)
$hCols.ColumnDefinitions.Add($c1)

$leftStack = New-Object System.Windows.Controls.StackPanel
$leftStack.VerticalAlignment = [System.Windows.VerticalAlignment]::Center

# Top Brand Row (Mini Icon + Midiastore)
$brandRow = New-Object System.Windows.Controls.StackPanel
$brandRow.Orientation = [System.Windows.Controls.Orientation]::Horizontal
$brandRow.Margin = New-Object System.Windows.Thickness(0, 0, 0, 18)
$miniLogo = New-MidiastoreLogoCanvas 56
$brandRow.Children.Add($miniLogo) | Out-Null

$brandText = New-Object System.Windows.Controls.TextBlock
$brandText.FontFamily = $font
$brandText.FontSize = 36
$brandText.FontWeight = [System.Windows.FontWeights]::ExtraBold
$brandText.VerticalAlignment = [System.Windows.VerticalAlignment]::Center
$brandText.Margin = New-Object System.Windows.Thickness(14, 0, 0, 0)
$run1 = New-Object System.Windows.Documents.Run("Midia")
$run1.Foreground = New-SolidBrush "#FFFFFF"
$run2 = New-Object System.Windows.Documents.Run("store")
$run2.Foreground = New-SolidBrush "#0A84FF"
$brandText.Inlines.Add($run1)
$brandText.Inlines.Add($run2)
$brandRow.Children.Add($brandText) | Out-Null
$leftStack.Children.Add($brandRow) | Out-Null

$hTitle = New-Object System.Windows.Controls.TextBlock
$hTitle.FontFamily = $font
$hTitle.Text = "Libere espaço no`nAndroid com 1 toque"
$hTitle.FontSize = 52
$hTitle.LineHeight = 58
$hTitle.FontWeight = [System.Windows.FontWeights]::ExtraBold
$hTitle.Foreground = New-SolidBrush "#FFFFFF"
$leftStack.Children.Add($hTitle) | Out-Null

$hSub = New-Object System.Windows.Controls.TextBlock
$hSub.FontFamily = $font
$hSub.Text = "Duplicatas exatas SHA-256, vídeos grandes e WhatsApp.`nApenas 5,9 MB • 100% seguro no aparelho."
$hSub.FontSize = 21
$hSub.LineHeight = 29
$hSub.Foreground = New-SolidBrush "#98989D"
$hSub.Margin = New-Object System.Windows.Thickness(0, 16, 0, 24)
$leftStack.Children.Add($hSub) | Out-Null

$ctaBadge = New-Object System.Windows.Controls.Border
$ctaBadge.Background = New-SolidBrush "#0A84FF"
$ctaBadge.CornerRadius = New-Object System.Windows.CornerRadius(14)
$ctaBadge.Padding = New-Object System.Windows.Thickness(24, 14, 24, 14)
$ctaBadge.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Left
$ctaText = New-Object System.Windows.Controls.TextBlock
$ctaText.FontFamily = $font
$ctaText.Text = "Baixar na Google Play  ›"
$ctaText.FontSize = 21
$ctaText.FontWeight = [System.Windows.FontWeights]::Bold
$ctaText.Foreground = New-SolidBrush "#FFFFFF"
$ctaBadge.Child = $ctaText
$leftStack.Children.Add($ctaBadge) | Out-Null

[System.Windows.Controls.Grid]::SetColumn($leftStack, 0)
$hCols.Children.Add($leftStack) | Out-Null

$rightBox = New-Object System.Windows.Controls.Grid
$rightBox.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center
$rightBox.VerticalAlignment = [System.Windows.VerticalAlignment]::Center
$phoneHoriz = New-PhoneMockup $homeBmp 300 540
$rightBox.Children.Add($phoneHoriz) | Out-Null
[System.Windows.Controls.Grid]::SetColumn($rightBox, 1)
$hCols.Children.Add($rightBox) | Out-Null

$horizRoot.Children.Add($hCols) | Out-Null
Save-ElementAsPng $horizRoot 1200 628 (Join-Path $outDir "banner-horizontal-1200x628.png")

# 3. BANNER QUADRADO 1200x1200 (Google Ads Square 1:1)
$sqRoot = New-Object System.Windows.Controls.Grid
$sqBg = New-Object System.Windows.Media.LinearGradientBrush
$sqBg.StartPoint = New-Object System.Windows.Point(0.5, 0)
$sqBg.EndPoint = New-Object System.Windows.Point(0.5, 1)
$sqBg.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#060B16"), 0.0))
$sqBg.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#000000"), 0.5))
$sqBg.GradientStops.Add((New-Object System.Windows.Media.GradientStop (Convert-HexToColor "#071528"), 1.0))
$sqRoot.Background = $sqBg

$sqGlow = New-Object System.Windows.Shapes.Ellipse
$sqGlow.Width = 920
$sqGlow.Height = 760
$sqGlow.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center
$sqGlow.VerticalAlignment = [System.Windows.VerticalAlignment]::Bottom
$sqGlow.Margin = New-Object System.Windows.Thickness(0, 0, 0, -80)
$sqGlow.Fill = New-Object System.Windows.Media.RadialGradientBrush (Convert-HexToColor "#3A0A84FF"), (Convert-HexToColor "#000A84FF")
$sqRoot.Children.Add($sqGlow) | Out-Null

$sqStack = New-Object System.Windows.Controls.StackPanel
$sqStack.Margin = New-Object System.Windows.Thickness(64, 52, 64, 0)
$sqStack.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center

$sqBrandRow = New-Object System.Windows.Controls.StackPanel
$sqBrandRow.Orientation = [System.Windows.Controls.Orientation]::Horizontal
$sqBrandRow.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center
$sqBrandRow.Margin = New-Object System.Windows.Thickness(0, 0, 0, 16)
$sqMiniLogo = New-MidiastoreLogoCanvas 58
$sqBrandRow.Children.Add($sqMiniLogo) | Out-Null

$sqBrandText = New-Object System.Windows.Controls.TextBlock
$sqBrandText.FontFamily = $font
$sqBrandText.FontSize = 38
$sqBrandText.FontWeight = [System.Windows.FontWeights]::ExtraBold
$sqBrandText.VerticalAlignment = [System.Windows.VerticalAlignment]::Center
$sqBrandText.Margin = New-Object System.Windows.Thickness(14, 0, 0, 0)
$sr1 = New-Object System.Windows.Documents.Run("Midia")
$sr1.Foreground = New-SolidBrush "#FFFFFF"
$sr2 = New-Object System.Windows.Documents.Run("store")
$sr2.Foreground = New-SolidBrush "#0A84FF"
$sqBrandText.Inlines.Add($sr1)
$sqBrandText.Inlines.Add($sr2)
$sqBrandRow.Children.Add($sqBrandText) | Out-Null
$sqStack.Children.Add($sqBrandRow) | Out-Null

$sqTitle = New-Object System.Windows.Controls.TextBlock
$sqTitle.FontFamily = $font
$sqTitle.Text = "Libere espaço no Android`ncom 1 toque"
$sqTitle.FontSize = 54
$sqTitle.LineHeight = 62
$sqTitle.TextAlignment = [System.Windows.TextAlignment]::Center
$sqTitle.FontWeight = [System.Windows.FontWeights]::ExtraBold
$sqTitle.Foreground = New-SolidBrush "#FFFFFF"
$sqStack.Children.Add($sqTitle) | Out-Null

$sqSub = New-Object System.Windows.Controls.TextBlock
$sqSub.FontFamily = $font
$sqSub.Text = "Apenas 5,9 MB • Duplicatas SHA-256 • Vídeos Grandes • 100% Local"
$sqSub.FontSize = 22
$sqSub.TextAlignment = [System.Windows.TextAlignment]::Center
$sqSub.Foreground = New-SolidBrush "#98989D"
$sqSub.Margin = New-Object System.Windows.Thickness(0, 12, 0, 28)
$sqStack.Children.Add($sqSub) | Out-Null

$phoneSq = New-PhoneMockup $homeBmp 460 800
$phoneSq.HorizontalAlignment = [System.Windows.HorizontalAlignment]::Center
$sqStack.Children.Add($phoneSq) | Out-Null

$sqRoot.Children.Add($sqStack) | Out-Null
Save-ElementAsPng $sqRoot 1200 1200 (Join-Path $outDir "banner-quadrado-1200x1200.png")
